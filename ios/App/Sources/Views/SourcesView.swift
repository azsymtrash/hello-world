import SwiftData
import SwiftUI
import UIKit

@MainActor
struct SourcesView: View {

    @Environment(\.modelContext) private var context
    @Query(sort: \Capture.startedAt, order: .reverse) private var captures: [Capture]

    @State private var settings = Settings.shared
    @State private var recorder = AudioRecorder()
    @State private var showTextEntry = false
    @State private var detail: Capture?
    @State private var permissionDenied = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    recordButton

                    Button {
                        showTextEntry = true
                    } label: {
                        Label("Добави текст", systemImage: "text.badge.plus")
                    }

                    Button {
                        pasteFromClipboard()
                    } label: {
                        Label("Постави от клипборда", systemImage: "doc.on.clipboard")
                    }
                } footer: {
                    Text("Анализът тръгва сам, щом добавиш източник. От Съобщения натисни и задръж съобщението → Сподели → CallScribe.")
                }

                if !captures.isEmpty {
                    Section("Източници") {
                        ForEach(captures) { capture in
                            captureRow(capture)
                                .contentShape(Rectangle())
                                .onTapGesture { detail = capture }
                        }
                        .onDelete(perform: delete)
                    }
                }
            }
            .navigationTitle("Източници")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showTextEntry) {
                TextEntryView { text, contact in
                    add(text: text, contact: contact)
                }
            }
            .sheet(item: $detail) { capture in
                CaptureDetailView(capture: capture) {
                    Task { await CaptureProcessor(context: context).process(capture) }
                }
            }
            .alert("Няма достъп до микрофона", isPresented: $permissionDenied) {
                Button("Добре", role: .cancel) {}
            } message: {
                Text("Разреши достъпа от Настройки → CallScribe → Микрофон.")
            }
        }
    }

    private var recordButton: some View {
        Button {
            if recorder.isRecording {
                stopRecording()
            } else {
                startRecording()
            }
        } label: {
            HStack {
                Label(
                    recorder.isRecording ? "Спри записа" : "Запиши среща",
                    systemImage: recorder.isRecording ? "stop.circle.fill" : "mic.circle.fill"
                )
                Spacer()
                if recorder.isRecording {
                    Text(Formatting.duration(Int(recorder.elapsed)))
                        .font(.body.monospacedDigit())
                        .foregroundStyle(.secondary)
                }
            }
        }
        .foregroundStyle(recorder.isRecording ? Color.red : Color.accentColor)
    }

    private func captureRow(_ capture: Capture) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text("\(Formatting.source(capture.kind)) · \(capture.contactName ?? "без контакт")")
                .font(.subheadline.weight(.semibold))
            Text(Formatting.dateTime(capture.startedAt))
                .font(.caption)
                .foregroundStyle(.secondary)
            HStack(spacing: 4) {
                if capture.status == CaptureStatus.transcribing || capture.status == CaptureStatus.analyzing {
                    ProgressView().controlSize(.mini)
                }
                Text(statusLine(capture))
                    .font(.caption)
                    .foregroundStyle(capture.status == CaptureStatus.error ? Color.red : Color.secondary)
            }
            if let text = capture.text, !text.isEmpty {
                Text(text)
                    .font(.caption)
                    .lineLimit(2)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 2)
    }

    private func statusLine(_ capture: Capture) -> String {
        if capture.status == CaptureStatus.done {
            return "Готово · \(capture.tasksFound) задачи"
        }
        if capture.status == CaptureStatus.error {
            return capture.errorMessage ?? "Грешка"
        }
        return Formatting.captureStatus(capture.status)
    }

    // MARK: - Действия

    private func startRecording() {
        Task {
            let granted = await recorder.requestPermission()
            guard granted else {
                permissionDenied = true
                return
            }
            recorder.start()
        }
    }

    private func stopRecording() {
        guard let result = recorder.stop() else { return }
        let capture = Capture(
            kind: Kind.call,
            audioFileName: result.fileName,
            durationSec: result.duration
        )
        insert(capture)
    }

    private func pasteFromClipboard() {
        guard let text = UIPasteboard.general.string,
              !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        add(text: text, contact: nil)
    }

    private func add(text: String, contact: String?) {
        let capture = Capture(kind: Kind.text, contactName: contact, text: text)
        insert(capture)
    }

    private func insert(_ capture: Capture) {
        context.insert(capture)
        try? context.save()
        guard settings.autoProcess else { return }
        Task { await CaptureProcessor(context: context).process(capture) }
    }

    private func delete(at offsets: IndexSet) {
        for index in offsets {
            let capture = captures[index]
            if let url = capture.audioURL {
                try? FileManager.default.removeItem(at: url)
            }
            if let all = try? context.fetch(FetchDescriptor<TaskRow>()) {
                for task in all where task.captureID == capture.id {
                    ReminderScheduler.cancel(taskID: task.id)
                    context.delete(task)
                }
            }
            context.delete(capture)
        }
        try? context.save()
    }
}

struct TextEntryView: View {

    let onAdd: (String, String?) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @State private var contact = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Контакт (по избор)") {
                    TextField("Име", text: $contact)
                }
                Section("Текст") {
                    TextEditor(text: $text)
                        .frame(minHeight: 180)
                }
            }
            .navigationTitle("Добави текст")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отказ") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Анализирай") {
                        onAdd(text.trimmingCharacters(in: .whitespacesAndNewlines),
                              contact.isEmpty ? nil : contact)
                        dismiss()
                    }
                    .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

@MainActor
struct CaptureDetailView: View {

    @Bindable var capture: Capture
    let onReprocess: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    LabeledContent("Тип", value: Formatting.source(capture.kind))
                    LabeledContent("Контакт", value: capture.contactName ?? "—")
                    LabeledContent("Кога", value: Formatting.dateTime(capture.startedAt))
                    if capture.kind == Kind.call {
                        LabeledContent("Времетраене", value: Formatting.duration(capture.durationSec))
                    }
                    LabeledContent("Състояние", value: Formatting.captureStatus(capture.status))
                    if let error = capture.errorMessage {
                        Text(error).font(.footnote).foregroundStyle(.red)
                    }
                }

                Section("Текст") {
                    Text(capture.text?.isEmpty == false ? capture.text! : "— няма —")
                        .font(.footnote)
                }

                Section {
                    Button("Анализирай наново") {
                        capture.status = CaptureStatus.new
                        capture.errorMessage = nil
                        onReprocess()
                        dismiss()
                    }
                }
            }
            .navigationTitle("Източник")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Затвори") { dismiss() }
                }
            }
        }
    }
}
