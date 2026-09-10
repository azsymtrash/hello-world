import Foundation

enum Formatting {

    private static var dateTimeFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.dateFormat = "dd.MM.yy HH:mm"
        return formatter
    }

    private static var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.dateFormat = "dd.MM.yyyy"
        return formatter
    }

    static let emDash = "—"

    static func dateTime(_ date: Date?) -> String {
        guard let date else { return emDash }
        return dateTimeFormatter.string(from: date)
    }

    static func due(_ date: Date?, allDay: Bool) -> String {
        guard let date else { return emDash }
        return allDay ? dateFormatter.string(from: date) : dateTimeFormatter.string(from: date)
    }

    static func duration(_ seconds: Int) -> String {
        String(format: "%d:%02d", seconds / 60, seconds % 60)
    }

    static func source(_ kind: String) -> String {
        switch kind {
        case Kind.call: return String(localized: "Recording")
        case Kind.text: return String(localized: "Message")
        default: return String(localized: "Manual")
        }
    }

    static func priority(_ value: String) -> String {
        switch value {
        case Priority.high: return String(localized: "High")
        case Priority.low: return String(localized: "Low")
        default: return String(localized: "Normal")
        }
    }

    static func status(_ value: String) -> String {
        switch value {
        case TaskState.done: return String(localized: "Done")
        case TaskState.cancelled: return String(localized: "Cancelled")
        default: return String(localized: "Open")
        }
    }

    static func captureStatus(_ value: String) -> String {
        switch value {
        case CaptureStatus.new: return String(localized: "Waiting to be processed")
        case CaptureStatus.transcribing: return String(localized: "Transcribing")
        case CaptureStatus.analyzing: return String(localized: "Analyzing")
        case CaptureStatus.done: return String(localized: "Done")
        default: return String(localized: "Error")
        }
    }

    static func confidence(_ value: Double) -> String {
        "\(Int(value * 100))%"
    }
}
