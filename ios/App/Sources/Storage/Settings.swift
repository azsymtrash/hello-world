import Foundation
import Observation

/// Настройки на приложението.
///
/// Ключовете стоят в UserDefaults и не са криптирани. За реална употреба насочи
/// `aiBaseURL` към собствен прокси сървър и остави полето за ключ празно.
@Observable
final class Settings {
    static let shared = Settings()

    private let defaults = UserDefaults.standard

    private enum Key {
        static let consent = "consent"
        static let aiBase = "ai_base"
        static let aiKey = "ai_key"
        static let aiModel = "ai_model"
        static let asrBase = "asr_base"
        static let asrKey = "asr_key"
        static let asrModel = "asr_model"
        static let language = "language"
        static let reminderOffset = "reminder_offset"
        static let deleteAudio = "delete_audio"
        static let autoProcess = "auto_process"
    }

    var consentAccepted: Bool {
        didSet { defaults.set(consentAccepted, forKey: Key.consent) }
    }

    var aiBaseURL: String {
        didSet { defaults.set(aiBaseURL, forKey: Key.aiBase) }
    }

    var aiAPIKey: String {
        didSet { defaults.set(aiAPIKey, forKey: Key.aiKey) }
    }

    var aiModel: String {
        didSet { defaults.set(aiModel, forKey: Key.aiModel) }
    }

    var asrBaseURL: String {
        didSet { defaults.set(asrBaseURL, forKey: Key.asrBase) }
    }

    var asrAPIKey: String {
        didSet { defaults.set(asrAPIKey, forKey: Key.asrKey) }
    }

    var asrModel: String {
        didSet { defaults.set(asrModel, forKey: Key.asrModel) }
    }

    var language: String {
        didSet { defaults.set(language, forKey: Key.language) }
    }

    var reminderOffsetMinutes: Int {
        didSet { defaults.set(reminderOffsetMinutes, forKey: Key.reminderOffset) }
    }

    var deleteAudioAfterTranscript: Bool {
        didSet { defaults.set(deleteAudioAfterTranscript, forKey: Key.deleteAudio) }
    }

    /// Анализът тръгва сам щом се появи нов източник, без да се натиска бутон.
    var autoProcess: Bool {
        didSet { defaults.set(autoProcess, forKey: Key.autoProcess) }
    }

    private init() {
        defaults.register(defaults: [
            Key.aiBase: "https://api.anthropic.com",
            Key.aiModel: "claude-sonnet-5",
            Key.asrModel: "whisper-1",
            Key.language: "bg",
            Key.reminderOffset: 30,
            Key.autoProcess: true
        ])

        consentAccepted = defaults.bool(forKey: Key.consent)
        aiBaseURL = defaults.string(forKey: Key.aiBase) ?? ""
        aiAPIKey = defaults.string(forKey: Key.aiKey) ?? ""
        aiModel = defaults.string(forKey: Key.aiModel) ?? ""
        asrBaseURL = defaults.string(forKey: Key.asrBase) ?? ""
        asrAPIKey = defaults.string(forKey: Key.asrKey) ?? ""
        asrModel = defaults.string(forKey: Key.asrModel) ?? ""
        language = defaults.string(forKey: Key.language) ?? "bg"
        reminderOffsetMinutes = defaults.integer(forKey: Key.reminderOffset)
        deleteAudioAfterTranscript = defaults.bool(forKey: Key.deleteAudio)
        autoProcess = defaults.bool(forKey: Key.autoProcess)
    }

    var aiConfigured: Bool {
        let base = aiBaseURL.trimmingCharacters(in: .whitespaces)
        guard !base.isEmpty else { return false }
        return !aiAPIKey.isEmpty || !base.contains("api.anthropic.com")
    }

    var asrConfigured: Bool {
        !asrBaseURL.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
