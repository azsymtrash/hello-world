import Foundation

/// Формат на елемент, оставен от разширението за споделяне.
struct SharedItem: Codable {
    var text: String?
    var audioFileName: String?
    var contactName: String?
    var createdAt: Date
}

/// Пренася споделеното от разширението към основното приложение през App Group.
enum SharedInbox {

    static func write(_ item: SharedItem) throws {
        guard let inbox = Paths.sharedInbox else {
            throw NSError(
                domain: "CallScribe", code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Споделената папка не е налична."]
            )
        }
        let url = inbox.appendingPathComponent("\(UUID().uuidString).json")
        let data = try JSONEncoder.callScribe.encode(item)
        try data.write(to: url, options: .atomic)
    }

    /// Прочита и изтрива всичко натрупано. Извиква се при всяко активиране на приложението.
    static func drain() -> [SharedItem] {
        guard let inbox = Paths.sharedInbox else { return [] }
        let manager = FileManager.default
        guard let files = try? manager.contentsOfDirectory(
            at: inbox, includingPropertiesForKeys: nil
        ) else { return [] }

        var items: [SharedItem] = []
        for file in files where file.pathExtension == "json" {
            if let data = try? Data(contentsOf: file),
               let item = try? JSONDecoder.callScribe.decode(SharedItem.self, from: data) {
                items.append(item)
                if let audioName = item.audioFileName {
                    moveAudioIntoApp(named: audioName, from: inbox)
                }
            }
            try? manager.removeItem(at: file)
        }
        return items.sorted { $0.createdAt < $1.createdAt }
    }

    private static func moveAudioIntoApp(named name: String, from inbox: URL) {
        let manager = FileManager.default
        let source = inbox.appendingPathComponent(name)
        let target = Paths.recordings.appendingPathComponent(name)
        guard manager.fileExists(atPath: source.path) else { return }
        try? manager.removeItem(at: target)
        try? manager.moveItem(at: source, to: target)
    }
}

extension JSONEncoder {
    static var callScribe: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }
}

extension JSONDecoder {
    static var callScribe: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}
