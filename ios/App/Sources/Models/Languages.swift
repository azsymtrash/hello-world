import Foundation

/// Езикът важи и за двете стъпки: подава се на сървъра за транскрипция и определя
/// на какъв език моделът пише извлечените задачи.
enum Languages {

    struct Option: Identifiable, Hashable {
        let code: String
        let label: String
        var id: String { code }
    }

    static let all: [Option] = [
        Option(code: "bg", label: "Български"),
        Option(code: "en", label: "English")
    ]

    static let defaultCode = "bg"

    static func label(_ code: String) -> String {
        all.first { $0.code == code }?.label ?? code
    }

    static func normalize(_ code: String) -> String {
        all.contains { $0.code == code } ? code : defaultCode
    }
}
