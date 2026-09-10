import Foundation
import UserNotifications

/// Локални известия за задачите. iOS няма точни аларми като Android —
/// известието се насрочва през UNUserNotificationCenter.
enum ReminderScheduler {

    static let categoryID = "TASK_REMINDER"
    static let doneActionID = "TASK_DONE"
    static let snoozeActionID = "TASK_SNOOZE"

    static func registerCategories() {
        let done = UNNotificationAction(
            identifier: doneActionID,
            title: "Готово",
            options: [.authenticationRequired]
        )
        let snooze = UNNotificationAction(
            identifier: snoozeActionID,
            title: "Отложи 1 час",
            options: []
        )
        let category = UNNotificationCategory(
            identifier: categoryID,
            actions: [done, snooze],
            intentIdentifiers: [],
            options: []
        )
        UNUserNotificationCenter.current().setNotificationCategories([category])
    }

    static func requestAuthorization() async {
        _ = try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound, .badge])
    }

    /// Кога да звънне напомнянето, или nil ако няма срок в бъдещето.
    static func reminderTime(for task: TaskRow, offsetMinutes: Int) -> Date? {
        guard let due = task.dueAt else { return nil }
        let offset = task.allDay ? 0 : TimeInterval(offsetMinutes * 60)
        let candidate = due.addingTimeInterval(-offset)
        if candidate > Date() { return candidate }
        if due > Date() { return Date().addingTimeInterval(60) }
        return nil
    }

    static func schedule(_ task: TaskRow) {
        cancel(taskID: task.id)
        guard task.status == TaskState.open,
              let at = task.reminderAt,
              at > Date() else { return }

        let content = UNMutableNotificationContent()
        content.title = task.title
        content.body = subtitle(for: task)
        content.sound = .default
        content.categoryIdentifier = categoryID
        content.userInfo = ["taskID": task.id.uuidString]

        let interval = max(at.timeIntervalSinceNow, 1)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let request = UNNotificationRequest(
            identifier: task.id.uuidString,
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request)
    }

    static func cancel(taskID: UUID) {
        let id = taskID.uuidString
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [id])
    }

    private static func subtitle(for task: TaskRow) -> String {
        var parts: [String] = []
        if let contact = task.contactName { parts.append(contact) }
        if let due = task.dueAt { parts.append(Formatting.due(due, allDay: task.allDay)) }
        return parts.joined(separator: " · ")
    }
}
