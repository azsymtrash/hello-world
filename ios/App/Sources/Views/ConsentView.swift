import SwiftUI

struct ConsentView: View {

    let onAccept: () -> Void
    @State private var accepted = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("CallScribe")
                    .font(.largeTitle.bold())

                Text("Records meetings and text, turns them into text, and pulls out tasks with deadlines into a table of reminders.")
                    .font(.body)

                Text("What this app cannot do on iPhone")
                    .font(.headline)
                    .padding(.top, 6)

                bullet("iOS gives no access to the audio of a phone call. There is no API for it — neither public nor roundabout.")
                bullet("While a call is running, the system takes the microphone away from every app. Recording a call is impossible, even on speakerphone.")
                bullet("There is no access to Messages. Text only comes in when you share it to the app or paste it.")

                Text("What it does")
                    .font(.headline)
                    .padding(.top, 6)

                bullet("Records meetings and voice notes through the microphone.")
                bullet("Takes text through the share button from Messages, Mail and Notes.")
                bullet("Transcribes and pulls out the tasks on its own as soon as a source appears.")

                Text("Before you continue")
                    .font(.headline)
                    .padding(.top, 6)

                bullet("Recording someone else's voice without consent is illegal in many countries. Responsibility is yours.")
                bullet("Transcription and analysis are sent to the servers you configure. The content leaves the phone.")
                bullet("Recordings and text stay on this phone only.")

                Toggle(isOn: $accepted) {
                    Text("I understand the limits and take responsibility for lawful use.")
                        .font(.footnote)
                }
                .padding(.top, 8)

                Button(action: onAccept) {
                    Text("Understood, continue")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!accepted)
                .padding(.top, 4)
            }
            .padding(24)
        }
    }

    private func bullet(_ text: LocalizedStringKey) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Text(verbatim: "•")
            Text(text)
        }
        .font(.footnote)
    }
}
