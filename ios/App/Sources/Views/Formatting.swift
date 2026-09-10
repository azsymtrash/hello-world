import Foundation

enum Formatting {

    private static let locale = Locale(identifier: "bg_BG")

    private static let dateTimeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.dateFormat = "dd.MM.yy HH:mm"
        return formatter
    }()

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.dateFormat = "dd.MM.yyyy"
        return formatter
    }()

    static func dateTime(_ date: Date?) -> String {
        guard let date else { return "—" }
        return dateTimeFormatter.string(from: date)
    }

    static func due(_ date: Date?, allDay: Bool) -> String {
        guard let date else { return "—" }
        return allDay ? dateFormatter.string(from: date) : dateTimeFormatter.string(from: date)
    }

    static func duration(_ seconds: Int) -> String {
        String(format: "%d:%02d", seconds / 60, seconds % 60)
    }

    static func source(_ kind: String) -> String {
        switch kind {
        case Kind.call: return "Запис"
        case Kind.text: return "Съобщение"
        default: return "Ръчно"
        }
    }

    static func priority(_ value: String) -> String {
        switch value {
        case Priority.high: return "Висок"
        case Priority.low: return "Нисък"
        default: return "Нормален"
        }
    }

    static func status(_ value: String) -> String {
        switch value {
        case TaskState.done: return "Готово"
        case TaskState.cancelled: return "Отказано"
        default: return "Отворено"
        }
    }

    static func captureStatus(_ value: String) -> String {
        switch value {
        case CaptureStatus.new: return "Чака обработка"
        case CaptureStatus.transcribing: return "Транскрибира се"
        case CaptureStatus.analyzing: return "Анализира се"
        case CaptureStatus.done: return "Готово"
        default: return "Грешка"
        }
    }

    static func confidence(_ value: Double) -> String {
        "\(Int(value * 100))%"
    }
}
