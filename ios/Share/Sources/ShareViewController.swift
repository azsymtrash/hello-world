import UniformTypeIdentifiers
import UIKit

/// Приема споделен текст или аудио файл и го оставя в общата пощенска кутия.
/// Основното приложение го прибира при следващото си активиране.
final class ShareViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        Task { await handleInput() }
    }

    private func handleInput() async {
        guard let items = extensionContext?.inputItems as? [NSExtensionItem] else {
            finish()
            return
        }

        var text: String?
        var audioFileName: String?

        for item in items {
            for provider in item.attachments ?? [] {
                if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
                    text = await loadText(from: provider) ?? text
                } else if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
                    if let url = await loadURL(from: provider) {
                        text = [text, url.absoluteString].compactMap { $0 }.joined(separator: "\n")
                    }
                } else if provider.hasItemConformingToTypeIdentifier(UTType.audio.identifier) {
                    audioFileName = await copyAudio(from: provider) ?? audioFileName
                }
            }
            if let attributed = item.attributedContentText?.string, text == nil, !attributed.isEmpty {
                text = attributed
            }
        }

        let trimmed = text?.trimmingCharacters(in: .whitespacesAndNewlines)
        if (trimmed?.isEmpty ?? true) && audioFileName == nil {
            finish()
            return
        }

        let payload = SharedItem(
            text: trimmed?.isEmpty == false ? trimmed : nil,
            audioFileName: audioFileName,
            contactName: nil,
            createdAt: Date()
        )
        try? SharedInbox.write(payload)
        finish()
    }

    private func loadText(from provider: NSItemProvider) async -> String? {
        await withCheckedContinuation { continuation in
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { value, _ in
                continuation.resume(returning: value as? String)
            }
        }
    }

    private func loadURL(from provider: NSItemProvider) async -> URL? {
        await withCheckedContinuation { continuation in
            provider.loadItem(forTypeIdentifier: UTType.url.identifier, options: nil) { value, _ in
                continuation.resume(returning: value as? URL)
            }
        }
    }

    /// Копира аудиото в споделената папка — оригиналът не е достъпен след затваряне.
    private func copyAudio(from provider: NSItemProvider) async -> String? {
        let source: URL? = await withCheckedContinuation { continuation in
            provider.loadItem(forTypeIdentifier: UTType.audio.identifier, options: nil) { value, _ in
                continuation.resume(returning: value as? URL)
            }
        }
        guard let source, let inbox = Paths.sharedInbox else { return nil }

        let name = "shared-\(UUID().uuidString).\(source.pathExtension.isEmpty ? "m4a" : source.pathExtension)"
        let target = inbox.appendingPathComponent(name)
        do {
            try FileManager.default.copyItem(at: source, to: target)
            return name
        } catch {
            return nil
        }
    }

    private func finish() {
        DispatchQueue.main.async { [weak self] in
            self?.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
        }
    }
}
