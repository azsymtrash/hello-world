import Foundation
import SwiftData

/// Суров източник: запис на среща, поставен текст или споделено съобщение.
@Model
final class Capture {
    @Attribute(.unique) var id: UUID
    var kind: String
    var direction: String
    var contactName: String?
    var startedAt: Date
    var durationSec: Int
    /// Само името на файла — пълният път се сглобява наново, защото контейнерът на
    /// приложението получава нов идентификатор при всяко инсталиране.
    var audioFileName: String?
    var text: String?
    var status: String
    var errorMessage: String?
    var tasksFound: Int

    init(
        id: UUID = UUID(),
        kind: String,
        direction: String = Direction.unknown,
        contactName: String? = nil,
        startedAt: Date = Date(),
        durationSec: Int = 0,
        audioFileName: String? = nil,
        text: String? = nil,
        status: String = CaptureStatus.new,
        errorMessage: String? = nil,
        tasksFound: Int = 0
    ) {
        self.id = id
        self.kind = kind
        self.direction = direction
        self.contactName = contactName
        self.startedAt = startedAt
        self.durationSec = durationSec
        self.audioFileName = audioFileName
        self.text = text
        self.status = status
        self.errorMessage = errorMessage
        self.tasksFound = tasksFound
    }

    var audioURL: URL? {
        guard let audioFileName else { return nil }
        return Paths.recordings.appendingPathComponent(audioFileName)
    }
}
