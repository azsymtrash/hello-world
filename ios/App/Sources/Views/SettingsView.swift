import SwiftUI

struct SettingsView: View {

    @State private var settings = Settings.shared

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Анализирай автоматично", isOn: $settings.autoProcess)
                    Toggle("Трий аудиото след транскрипция", isOn: $settings.deleteAudioAfterTranscript)
                    Stepper(
                        "Напомняй \(settings.reminderOffsetMinutes) мин. преди срока",
                        value: $settings.reminderOffsetMinutes,
                        in: 0...1440,
                        step: 15
                    )
                } header: {
                    Text("Поведение")
                } footer: {
                    Text("Изключено, източниците стоят необработени, докато не натиснеш „Анализирай наново“.")
                }

                Section {
                    TextField("Адрес на API", text: $settings.aiBaseURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField("API ключ", text: $settings.aiAPIKey)
                    TextField("Модел", text: $settings.aiModel)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                } header: {
                    Text("Анализ (Claude)")
                } footer: {
                    Text("Ключът стои некриптиран в паметта на приложението. По-безопасно е адресът да сочи към собствен прокси сървър, а полето за ключ да остане празно. Модели: claude-opus-5 (най-точен), claude-sonnet-5 (баланс), claude-haiku-4-5 (най-евтин).")
                }

                Section {
                    TextField("Адрес на сървъра", text: $settings.asrBaseURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField("API ключ", text: $settings.asrAPIKey)
                    TextField("Модел", text: $settings.asrModel)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    Picker("Език", selection: $settings.language) {
                        ForEach(Languages.all) { option in
                            Text(option.label).tag(option.code)
                        }
                    }
                } header: {
                    Text("Транскрипция")
                } footer: {
                    Text("Езикът важи и за двете стъпки: подава се на сървъра за транскрипция и определя на какъв език моделът пише извлечените задачи. Очаква се OpenAI-съвместим endpoint POST {адрес}/v1/audio/transcriptions — работи и с whisper.cpp сървър в локалната мрежа, ако не искаш аудиото да напуска дома ти.")
                }

                Section {
                    LabeledContent("Анализ", value: settings.aiConfigured ? "готов" : "не е настроен")
                    LabeledContent("Транскрипция", value: settings.asrConfigured ? "готова" : "не е настроена")
                } header: {
                    Text("Състояние")
                }
            }
            .navigationTitle("Настройки")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
