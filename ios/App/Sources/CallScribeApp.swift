import SwiftData
import SwiftUI
import UserNotifications

@main
struct CallScribeApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate

    private let container: ModelContainer = {
        do {
            return try ModelContainer(for: Capture.self, TaskRow.self)
        } catch {
            fatalError("The database could not be opened: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .modelContainer(container)
    }
}

/// Поема действията от известията — „Готово“ и „Отложи 1 час“ работят,
/// без приложението да се отваря.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    var container: ModelContainer?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        ReminderScheduler.registerCategories()
        container = try? ModelContainer(for: Capture.self, TaskRow.self)
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .list]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let info = response.notification.request.content.userInfo
        guard let raw = info["taskID"] as? String, let taskID = UUID(uuidString: raw) else { return }

        switch response.actionIdentifier {
        case ReminderScheduler.doneActionID:
            await update(taskID: taskID) { task in
                task.status = TaskState.done
                task.reminderAt = nil
                ReminderScheduler.cancel(taskID: task.id)
            }
        case ReminderScheduler.snoozeActionID:
            await update(taskID: taskID) { task in
                task.reminderAt = Date().addingTimeInterval(60 * 60)
                ReminderScheduler.schedule(task)
            }
        default:
            break
        }
    }

    @MainActor
    private func update(taskID: UUID, _ change: (TaskRow) -> Void) {
        guard let container else { return }
        let context = ModelContext(container)
        guard let all = try? context.fetch(FetchDescriptor<TaskRow>()),
              let task = all.first(where: { $0.id == taskID }) else { return }
        change(task)
        try? context.save()
    }
}
