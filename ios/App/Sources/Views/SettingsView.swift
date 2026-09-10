import SwiftUI

struct SettingsView: View {

    @State private var settings = Settings.shared

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Analyze automatically", isOn: $settings.autoProcess)
                    Toggle("Delete audio after transcription", isOn: $settings.deleteAudioAfterTranscript)
                    Stepper(
                        value: $settings.reminderOffsetMinutes,
                        in: 0...1440,
                        step: 15
                    ) {
                        Text(
                            verbatim: String(
                                format: String(localized: "Remind %d min before the deadline"),
                                settings.reminderOffsetMinutes
                            )
                        )
                    }
                } header: {
                    Text("Behavior")
                } footer: {
                    Text("Switched off, sources stay unprocessed until you press “Analyze again”.")
                }

                Section {
                    TextField("API address", text: $settings.aiBaseURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField("API key", text: $settings.aiAPIKey)
                    TextField("Model", text: $settings.aiModel)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                } header: {
                    Text("Analysis (Claude)")
                } footer: {
                    Text("The key sits unencrypted in the app's storage. It is safer to point the address at your own proxy server and leave the key field empty. Models: claude-opus-5 (most accurate), claude-sonnet-5 (balanced), claude-haiku-4-5 (cheapest).")
                }

                Section {
                    TextField("Server address", text: $settings.asrBaseURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField("API key", text: $settings.asrAPIKey)
                    TextField("Model", text: $settings.asrModel)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    Picker("Language", selection: $settings.language) {
                        ForEach(Languages.all) { option in
                            Text(option.label).tag(option.code)
                        }
                    }
                } header: {
                    Text("Transcription")
                } footer: {
                    Text("The language covers both steps: it is passed to the transcription server and it decides which language the model writes the extracted tasks in. Expects an OpenAI-compatible endpoint POST {address}/v1/audio/transcriptions — works with a whisper.cpp server on your local network if you don't want the audio leaving your home.")
                }

                Section {
                    LabeledContent("Analysis", value: settings.aiConfigured ? String(localized: "ready") : String(localized: "not configured"))
                    LabeledContent("Transcription", value: settings.asrConfigured ? String(localized: "ready") : String(localized: "not configured"))
                } header: {
                    Text("State")
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
