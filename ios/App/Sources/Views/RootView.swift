import SwiftData
import SwiftUI

@MainActor
struct RootView: View {

    @Environment(\.modelContext) private var context
    @Environment(\.scenePhase) private var scenePhase
    @State private var settings = Settings.shared
    @State private var selection = 0

    var body: some View {
        Group {
            if settings.consentAccepted {
                TabView(selection: $selection) {
                    TaskTableView()
                        .tabItem { Label("Table", systemImage: "tablecells") }
                        .tag(0)

                    SourcesView()
                        .tabItem { Label("Sources", systemImage: "waveform") }
                        .tag(1)

                    SettingsView()
                        .tabItem { Label("Settings", systemImage: "gearshape") }
                        .tag(2)
                }
                .task { await bootstrap() }
                .onChange(of: scenePhase) { _, phase in
                    guard phase == .active else { return }
                    Task { await bootstrap() }
                }
            } else {
                ConsentView { settings.consentAccepted = true }
            }
        }
    }

    /// Изпълнява се при всяко активиране: прибира споделеното и дообработва
    /// всичко, което е останало — без потребителят да натиска каквото и да е.
    private func bootstrap() async {
        await ReminderScheduler.requestAuthorization()
        ingestShared()
        guard settings.autoProcess else { return }
        await CaptureProcessor(context: context).processPending()
    }

    private func ingestShared() {
        let items = SharedInbox.drain()
        guard !items.isEmpty else { return }
        for item in items {
            let capture = Capture(
                kind: item.audioFileName != nil ? Kind.call : Kind.text,
                contactName: item.contactName,
                startedAt: item.createdAt,
                audioFileName: item.audioFileName,
                text: item.text
            )
            context.insert(capture)
        }
        try? context.save()
    }
}
