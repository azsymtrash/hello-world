import Foundation
import SwiftData

/// Веригата транскрипция → анализ → напомняния.
///
/// Работи веднага щом се появи нов източник; нищо не чака натискане на бутон.
@MainActor
final class CaptureProcessor {

    private let context: ModelContext
    private let settings: Settings
    private var running: Set<UUID> = []

    init(context: ModelContext, settings: Settings = .shared) {
        self.context = context
        self.settings = settings
    }

    /// Обработва всичко, което е останало необработено.
    func processPending() async {
        let unfinished = [CaptureStatus.new, CaptureStatus.transcribing, CaptureStatus.analyzing]
        let descriptor = FetchDescriptor<Capture>(sortBy: [SortDescriptor(\Capture.startedAt)])
        guard let all = try? context.fetch(descriptor) else { return }
        for capture in all where unfinished.contains(capture.status) {
            await process(capture)
        }
    }

    func process(_ capture: Capture) async {
        guard !running.contains(capture.id) else { return }
        running.insert(capture.id)
        defer { running.remove(capture.id) }

        do {
            if (capture.text?.isEmpty ?? true), let audioURL = capture.audioURL {
                guard settings.asrConfigured else {
                    finish(capture, status: CaptureStatus.error,
                           error: "Не е конфигуриран сървър за транскрипция.")
                    return
                }
                capture.status = CaptureStatus.transcribing
                capture.errorMessage = nil
                save()

                let transcript = try await Transcriber(settings: settings).transcribe(fileURL: audioURL)
                capture.text = transcript
                save()

                if settings.deleteAudioAfterTranscript, !transcript.isEmpty {
                    try? FileManager.default.removeItem(at: audioURL)
                    capture.audioFileName = nil
                    save()
                }
            }

            guard let text = capture.text, !text.isEmpty else {
                finish(capture, status: CaptureStatus.error, error: "Няма текст за анализ.")
                return
            }
            guard settings.aiConfigured else {
                finish(capture, status: CaptureStatus.error, error: "Не е конфигуриран API ключ за Claude.")
                return
            }

            capture.status = CaptureStatus.analyzing
            capture.errorMessage = nil
            save()

            let extracted = try await TaskExtractor(settings: settings).extract(
                text: text,
                kind: capture.kind,
                contact: capture.contactName,
                reference: capture.startedAt
            )

            removeExistingTasks(for: capture.id)

            for item in extracted {
                let task = TaskRow(
                    captureID: capture.id,
                    source: capture.kind,
                    contactName: item.person ?? capture.contactName,
                    title: item.title,
                    details: item.details,
                    dueAt: item.dueAt,
                    allDay: item.allDay,
                    priority: item.priority,
                    category: item.category,
                    confidence: item.confidence,
                    quote: item.quote
                )
                task.reminderAt = ReminderScheduler.reminderTime(
                    for: task,
                    offsetMinutes: settings.reminderOffsetMinutes
                )
                context.insert(task)
                ReminderScheduler.schedule(task)
            }

            capture.tasksFound = extracted.count
            finish(capture, status: CaptureStatus.done, error: nil)
        } catch {
            finish(capture, status: CaptureStatus.error, error: error.localizedDescription)
        }
    }

    private func removeExistingTasks(for captureID: UUID) {
        let descriptor = FetchDescriptor<TaskRow>()
        guard let all = try? context.fetch(descriptor) else { return }
        for task in all where task.captureID == captureID {
            ReminderScheduler.cancel(taskID: task.id)
            context.delete(task)
        }
    }

    private func finish(_ capture: Capture, status: String, error: String?) {
        capture.status = status
        capture.errorMessage = error
        save()
    }

    private func save() {
        try? context.save()
    }
}
