import Foundation

enum Kind {
    static let call = "CALL"
    static let text = "SMS"
    static let manual = "MANUAL"
}

enum Direction {
    static let incoming = "IN"
    static let outgoing = "OUT"
    static let unknown = "UNKNOWN"
}

enum CaptureStatus {
    static let new = "NEW"
    static let transcribing = "TRANSCRIBING"
    static let analyzing = "ANALYZING"
    static let done = "DONE"
    static let error = "ERROR"
}

enum TaskState {
    static let open = "OPEN"
    static let done = "DONE"
    static let cancelled = "CANCELLED"
}

enum Priority {
    static let low = "LOW"
    static let normal = "NORMAL"
    static let high = "HIGH"
}
