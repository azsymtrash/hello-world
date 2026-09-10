import SwiftUI

struct ConsentView: View {

    let onAccept: () -> Void
    @State private var accepted = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("CallScribe")
                    .font(.largeTitle.bold())

                Text("Записва срещи и текстове, превръща ги в текст и извлича от тях задачи с краен срок в таблица с напомняния.")
                    .font(.body)

                Text("Какво това приложение НЕ може на iPhone")
                    .font(.headline)
                    .padding(.top, 6)

                bullet("iOS не дава достъп до аудиото на телефонен разговор. Няма API за това — нито публично, нито заобиколено.")
                bullet("Докато тече разговор, системата отнема микрофона на всички приложения. Записът на разговор е невъзможен, дори на високоговорител.")
                bullet("Няма достъп до Съобщения. Текстовете влизат само когато ти ги споделиш към приложението или ги поставиш.")

                Text("Какво прави")
                    .font(.headline)
                    .padding(.top, 6)

                bullet("Записва срещи и гласови бележки през микрофона.")
                bullet("Приема текст през бутона за споделяне от Съобщения, Поща, Бележки.")
                bullet("Транскрибира и извлича задачите автоматично, щом се появи нов източник.")

                Text("Преди да продължиш")
                    .font(.headline)
                    .padding(.top, 6)

                bullet("Записът на чужд глас без съгласие е незаконен в много държави. Отговорността е твоя.")
                bullet("Транскрипцията и анализът се изпращат към сървърите, които ти конфигурираш. Съдържанието напуска телефона.")
                bullet("Записите и текстовете се пазят само на този телефон.")

                Toggle(isOn: $accepted) {
                    Text("Разбирам ограниченията и поемам отговорност за законосъобразната употреба.")
                        .font(.footnote)
                }
                .padding(.top, 8)

                Button(action: onAccept) {
                    Text("Разбрах, продължи")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!accepted)
                .padding(.top, 4)
            }
            .padding(24)
        }
    }

    private func bullet(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Text("•")
            Text(text)
        }
        .font(.footnote)
    }
}
