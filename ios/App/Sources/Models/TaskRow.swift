import Foundation
import SwiftData

/// Един ред в таблицата с напомняния.
@Model
final class TaskRow {
    @Attribute(.unique) var id: UUID
    var captureID: UUID?
    var createdAt: Date
    var source: String
    var contactName: String?
    var title: String
    var details: String?
    var dueAt: Date?
    var allDay: Bool
    var priority: String
    var category: String?
    var status: String
    var confidence: Double
    var quote: String?
    var reminderAt: Date?

    init(
        id: UUID = UUID(),
        captureID: UUID? = nil,
        createdAt: Date = Date(),
        source: String = Kind.manual,
        contactName: String? = nil,
        title: String,
        details: String? = nil,
        dueAt: Date? = nil,
        allDay: Bool = false,
        priority: String = Priority.normal,
        category: String? = nil,
        status: String = TaskState.open,
        confidence: Double = 1,
        quote: String? = nil,
        reminderAt: Date? = nil
    ) {
        self.id = id
        self.captureID = captureID
        self.createdAt = createdAt
        self.source = source
        self.contactName = contactName
        self.title = title
        self.details = details
        self.dueAt = dueAt
        self.allDay = allDay
        self.priority = priority
        self.category = category
        self.status = status
        self.confidence = confidence
        self.quote = quote
        self.reminderAt = reminderAt
    }

    var isDone: Bool { status == TaskState.done }

    var isOverdue: Bool {
        guard let dueAt, status == TaskState.open else { return false }
        return dueAt < Date()
    }
}
