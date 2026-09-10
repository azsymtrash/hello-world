import Foundation

enum Paths {
    static let appGroupID = "group.com.callscribe.ios"

    /// Записите живеят в контейнера на приложението, не в споделения — те не се
    /// разменят с разширението.
    static var recordings: URL {
        let base = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dir = base.appendingPathComponent("Recordings", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    /// Пощенска кутия, в която разширението за споделяне оставя новите елементи.
    static var sharedInbox: URL? {
        guard let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupID
        ) else { return nil }
        let dir = container.appendingPathComponent("Inbox", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
}
