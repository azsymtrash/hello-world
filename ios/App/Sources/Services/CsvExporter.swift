import Foundation

enum CsvExporter {

    private static var headers: [String] {
        [
            String(localized: "Created"), String(localized: "Source"),
            String(localized: "Contact"), String(localized: "Task"),
            String(localized: "Details"), String(localized: "Due"),
            String(localized: "All day"), String(localized: "Priority"),
            String(localized: "Category"), String(localized: "Status"),
            String(localized: "Confidence"), String(localized: "Reminder"),
            String(localized: "Quote")
        ]
    }

    static func csv(for tasks: [TaskRow]) -> String {
        // BOM, за да отвори Excel кирилицата коректно.
        var output = "\u{FEFF}"
        output += headers.map(escape).joined(separator: ",") + "\r\n"

        for task in tasks {
            let row = [
                Formatting.dateTime(task.createdAt),
                Formatting.source(task.source),
                task.contactName ?? "",
                task.title,
                task.details ?? "",
                task.dueAt.map { Formatting.due($0, allDay: task.allDay) } ?? "",
                task.allDay ? String(localized: "yes") : String(localized: "no"),
                Formatting.priority(task.priority),
                task.category ?? "",
                Formatting.status(task.status),
                String(format: "%.2f", task.confidence),
                task.reminderAt.map(Formatting.dateTime) ?? "",
                task.quote ?? ""
            ]
            output += row.map(escape).joined(separator: ",") + "\r\n"
        }
        return output
    }

    /// Записва CSV във временен файл, готов за споделяне.
    static func writeTemporaryFile(for tasks: [TaskRow]) -> URL? {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("napomnyania.csv")
        do {
            try csv(for: tasks).data(using: .utf8)?.write(to: url, options: .atomic)
            return url
        } catch {
            return nil
        }
    }

    private static func escape(_ value: String) -> String {
        let cleaned = value
            .replacingOccurrences(of: "\r\n", with: " ")
            .replacingOccurrences(of: "\n", with: " ")
            .replacingOccurrences(of: "\r", with: " ")
        return "\"" + cleaned.replacingOccurrences(of: "\"", with: "\"\"") + "\""
    }
}
