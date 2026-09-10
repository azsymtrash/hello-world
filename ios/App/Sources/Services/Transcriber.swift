import Foundation

struct ServiceError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

/// Транскрипция през OpenAI-съвместим endpoint `POST {base}/v1/audio/transcriptions`.
/// Работи с whisper.cpp сървър, faster-whisper-server, OpenAI и подобни.
struct Transcriber {
    let settings: Settings

    func transcribe(fileURL: URL) async throws -> String {
        let base = settings.asrBaseURL.trimmingCharacters(in: .whitespaces)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !base.isEmpty, let url = URL(string: "\(base)/v1/audio/transcriptions") else {
            throw ServiceError(message: String(localized: "No transcription server configured (Settings → Transcription)."))
        }
        let audio = try Data(contentsOf: fileURL)
        guard !audio.isEmpty else {
            throw ServiceError(message: String(localized: "The audio file is empty."))
        }

        let boundary = "callscribe-\(UUID().uuidString)"
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 180
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        if !settings.asrAPIKey.isEmpty {
            request.setValue("Bearer \(settings.asrAPIKey)", forHTTPHeaderField: "Authorization")
        }

        var body = Data()
        func appendField(_ name: String, _ value: String) {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(value)\r\n".data(using: .utf8)!)
        }

        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append(
            "Content-Disposition: form-data; name=\"file\"; filename=\"\(fileURL.lastPathComponent)\"\r\n"
                .data(using: .utf8)!
        )
        body.append("Content-Type: audio/m4a\r\n\r\n".data(using: .utf8)!)
        body.append(audio)
        body.append("\r\n".data(using: .utf8)!)

        appendField("model", settings.asrModel.isEmpty ? "whisper-1" : settings.asrModel)
        appendField("language", settings.language.isEmpty ? "bg" : settings.language)
        appendField("response_format", "json")
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ServiceError(message: String(localized: "No response from the transcription server."))
        }
        guard (200..<300).contains(http.statusCode) else {
            let detail = String(data: data, encoding: .utf8)?.prefix(300) ?? ""
            throw ServiceError(message: String(format: String(localized: "Transcription failed (HTTP %d): %@"), http.statusCode, String(detail)))
        }

        if let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let text = object["text"] as? String {
            return text.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return (String(data: data, encoding: .utf8) ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
