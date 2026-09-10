import AVFoundation
import Foundation
import Observation

/// Запис на срещи и гласови бележки.
///
/// Забележка: iOS не позволява на приложения да записват аудио по време на
/// телефонен разговор — сесията се отнема от системата. Това служи за срещи,
/// бележки и разговори, пуснати на друго устройство.
@Observable
final class AudioRecorder: NSObject {

    private(set) var isRecording = false
    private(set) var elapsed: TimeInterval = 0
    private(set) var lastError: String?

    private var recorder: AVAudioRecorder?
    private var timer: Timer?
    private var currentFileName: String?

    func requestPermission() async -> Bool {
        await AVAudioApplication.requestRecordPermission()
    }

    func start() {
        guard !isRecording else { return }
        lastError = nil

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let name = "recording-\(Int(Date().timeIntervalSince1970)).m4a"
            let url = Paths.recordings.appendingPathComponent(name)

            let settings: [String: Any] = [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                AVSampleRateKey: 16000,
                AVNumberOfChannelsKey: 1,
                AVEncoderBitRateKey: 64000,
                AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue
            ]

            let recorder = try AVAudioRecorder(url: url, settings: settings)
            recorder.delegate = self
            guard recorder.record() else {
                lastError = String(localized: "The recording could not start.")
                return
            }

            self.recorder = recorder
            currentFileName = name
            isRecording = true
            elapsed = 0
            startTimer()
        } catch {
            lastError = error.localizedDescription
        }
    }

    /// Спира записа и връща името на файла и продължителността.
    func stop() -> (fileName: String, duration: Int)? {
        guard let recorder, isRecording else { return nil }
        let duration = Int(recorder.currentTime.rounded())
        recorder.stop()
        stopTimer()
        isRecording = false
        self.recorder = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)

        guard let name = currentFileName else { return nil }
        currentFileName = nil

        let url = Paths.recordings.appendingPathComponent(name)
        let attributes = try? FileManager.default.attributesOfItem(atPath: url.path)
        let size = (attributes?[.size] as? NSNumber)?.intValue ?? 0
        guard size > 4096 else {
            try? FileManager.default.removeItem(at: url)
            lastError = String(localized: "The recording is too short.")
            return nil
        }
        return (name, max(duration, 1))
    }

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self, let recorder = self.recorder else { return }
            self.elapsed = recorder.currentTime
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}

extension AudioRecorder: AVAudioRecorderDelegate {
    func audioRecorderEncodeErrorDidOccur(_ recorder: AVAudioRecorder, error: Error?) {
        lastError = error?.localizedDescription ?? String(localized: "The recording failed to encode.")
        isRecording = false
        stopTimer()
    }
}
