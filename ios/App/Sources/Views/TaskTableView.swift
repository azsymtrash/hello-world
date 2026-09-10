import SwiftData
import SwiftUI
import UIKit

private enum SortKey: String, CaseIterable, Identifiable {
    case created, source, contact, title, due, priority, status, confidence

    var id: String { rawValue }

    var label: String {
        switch self {
        case .created: return String(localized: "Created")
        case .source: return String(localized: "Source")
        case .contact: return String(localized: "Contact")
        case .title: return String(localized: "Task")
        case .due: return String(localized: "Due")
        case .priority: return String(localized: "Priority")
        case .status: return String(localized: "Status")
        case .confidence: return String(localized: "Confidence")
        }
    }

    var width: CGFloat {
        switch self {
        case .created: return 112
        case .source: return 96
        case .contact: return 132
        case .title: return 240
        case .due: return 124
        case .priority: return 100
        case .status: return 96
        case .confidence: return 92
        }
    }
}

private let doneColumnWidth: CGFloat = 52

private struct SharePayload: Identifiable {
    let id = UUID()
    let url: URL
}

@MainActor
struct TaskTableView: View {

    @Environment(\.modelContext) private var context
    @Query(sort: \TaskRow.createdAt, order: .reverse) private var tasks: [TaskRow]

    @State private var settings = Settings.shared
    @State private var query = ""
    @State private var statusFilter: String? = TaskState.open
    @State private var sortKey: SortKey = .due
    @State private var ascending = true
    @State private var editing: TaskRow?
    @State private var showAdd = false
    @State private var share: SharePayload?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                filterBar

                if visible.isEmpty {
                    emptyState
                } else {
                    table
                }
            }
            .navigationTitle("Reminders")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, prompt: Text("Search"))
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Menu {
                        Picker("Sort by", selection: $sortKey) {
                            ForEach(SortKey.allCases) { key in
                                Text(key.label).tag(key)
                            }
                        }
                        Toggle("Ascending", isOn: $ascending)
                    } label: {
                        Image(systemName: "arrow.up.arrow.down")
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        if let url = CsvExporter.writeTemporaryFile(for: visible) {
                            share = SharePayload(url: url)
                        }
                    } label: {
                        Image(systemName: "square.and.arrow.up")
                    }
                    Button { showAdd = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(item: $editing) { task in
                TaskEditorView(task: task) { save() }
            }
            .sheet(isPresented: $showAdd) {
                NewTaskView { title, contact, dueAt in
                    addTask(title: title, contact: contact, dueAt: dueAt)
                }
            }
            .sheet(item: $share) { payload in
                ActivityView(url: payload.url)
            }
        }
    }

    // MARK: - Части

    private var filterBar: some View {
        HStack {
            Picker("Filter", selection: $statusFilter) {
                Text("Open").tag(String?.some(TaskState.open))
                Text("Done").tag(String?.some(TaskState.done))
                Text("All").tag(String?.none)
            }
            .pickerStyle(.segmented)

            Text("\(visible.count)")
                .font(.footnote.monospacedDigit())
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Spacer()
            Text(tasks.isEmpty ? "No tasks pulled out yet." : "Nothing matches this filter.")
                .font(.headline)
            if tasks.isEmpty {
                Text("Tasks appear on their own as soon as you add a recording or text under “Sources”.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            Spacer()
        }
    }

    private var table: some View {
        ScrollView(.horizontal, showsIndicators: true) {
            VStack(spacing: 0) {
                header
                Divider()
                ScrollView(.vertical) {
                    LazyVStack(spacing: 0) {
                        ForEach(visible) { task in
                            row(task)
                            Divider()
                        }
                    }
                }
            }
            .frame(width: totalWidth, alignment: .leading)
        }
    }

    private var header: some View {
        HStack(spacing: 0) {
            Text("Done")
                .frame(width: doneColumnWidth, alignment: .leading)
                .padding(.horizontal, 6)
            ForEach(SortKey.allCases) { key in
                Button {
                    if sortKey == key { ascending.toggle() } else {
                        sortKey = key
                        ascending = true
                    }
                } label: {
                    HStack(spacing: 2) {
                        Text(key.label)
                        if sortKey == key {
                            Image(systemName: ascending ? "chevron.up" : "chevron.down")
                                .font(.system(size: 8))
                        }
                    }
                    .frame(width: key.width, alignment: .leading)
                    .padding(.horizontal, 8)
                }
                .buttonStyle(.plain)
            }
        }
        .font(.caption.weight(.semibold))
        .padding(.vertical, 10)
        .background(Color(.secondarySystemBackground))
    }

    private func row(_ task: TaskRow) -> some View {
        HStack(spacing: 0) {
            Button {
                toggleDone(task)
            } label: {
                Image(systemName: task.isDone ? "checkmark.square.fill" : "square")
                    .font(.title3)
                    .foregroundStyle(task.isDone ? Color.accentColor : Color.secondary)
            }
            .buttonStyle(.plain)
            .frame(width: doneColumnWidth, alignment: .leading)
            .padding(.horizontal, 6)

            cell(Formatting.dateTime(task.createdAt), .created, task)
            cell(Formatting.source(task.source), .source, task)
            cell(task.contactName ?? Formatting.emDash, .contact, task)
            cell(task.title, .title, task, lines: 2)
            cell(Formatting.due(task.dueAt, allDay: task.allDay), .due, task,
                 color: task.isOverdue ? .red : nil)
            cell(Formatting.priority(task.priority), .priority, task)
            cell(Formatting.status(task.status), .status, task)
            cell(Formatting.confidence(task.confidence), .confidence, task)
        }
        .padding(.vertical, 8)
        .contentShape(Rectangle())
        .onTapGesture { editing = task }
    }

    private func cell(
        _ text: String,
        _ key: SortKey,
        _ task: TaskRow,
        lines: Int = 1,
        color: Color? = nil
    ) -> some View {
        Text(text)
            .font(.system(size: 13))
            .foregroundStyle(color ?? .primary)
            .strikethrough(task.isDone)
            .lineLimit(lines)
            .truncationMode(.tail)
            .frame(width: key.width, alignment: .leading)
            .padding(.horizontal, 8)
    }

    private var totalWidth: CGFloat {
        SortKey.allCases.reduce(doneColumnWidth) { $0 + $1.width }
    }

    // MARK: - Данни

    private var visible: [TaskRow] {
        let needle = query.trimmingCharacters(in: .whitespaces).lowercased()
        let filtered = tasks.filter { task in
            let matchesStatus = statusFilter == nil || task.status == statusFilter
            guard matchesStatus else { return false }
            guard !needle.isEmpty else { return true }
            return task.title.lowercased().contains(needle)
                || (task.contactName ?? "").lowercased().contains(needle)
                || (task.details ?? "").lowercased().contains(needle)
        }

        let sorted = filtered.sorted { left, right in
            switch sortKey {
            case .created: return left.createdAt < right.createdAt
            case .source: return left.source < right.source
            case .contact: return (left.contactName ?? "").localizedCompare(right.contactName ?? "") == .orderedAscending
            case .title: return left.title.localizedCompare(right.title) == .orderedAscending
            case .due: return (left.dueAt ?? .distantFuture) < (right.dueAt ?? .distantFuture)
            case .priority: return rank(left.priority) < rank(right.priority)
            case .status: return left.status < right.status
            case .confidence: return left.confidence < right.confidence
            }
        }
        return ascending ? sorted : sorted.reversed()
    }

    private func rank(_ priority: String) -> Int {
        switch priority {
        case Priority.high: return 0
        case Priority.normal: return 1
        default: return 2
        }
    }

    private func toggleDone(_ task: TaskRow) {
        task.status = task.isDone ? TaskState.open : TaskState.done
        task.reminderAt = task.status == TaskState.open
            ? ReminderScheduler.reminderTime(for: task, offsetMinutes: settings.reminderOffsetMinutes)
            : nil
        ReminderScheduler.cancel(taskID: task.id)
        ReminderScheduler.schedule(task)
        save()
    }

    private func addTask(title: String, contact: String?, dueAt: Date?) {
        let task = TaskRow(source: Kind.manual, contactName: contact, title: title, dueAt: dueAt)
        task.reminderAt = ReminderScheduler.reminderTime(
            for: task,
            offsetMinutes: settings.reminderOffsetMinutes
        )
        context.insert(task)
        ReminderScheduler.schedule(task)
        save()
    }

    private func save() {
        try? context.save()
    }
}

/// Стандартният системен лист за споделяне — за изнасяне на CSV файла.
struct ActivityView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
