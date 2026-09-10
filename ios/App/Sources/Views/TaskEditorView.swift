import SwiftData
import SwiftUI

@MainActor
struct TaskEditorView: View {

    @Bindable var task: TaskRow
    let onSave: () -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context
    @State private var settings = Settings.shared
    @State private var hasDueDate: Bool
    @State private var dueDate: Date

    init(task: TaskRow, onSave: @escaping () -> Void) {
        self.task = task
        self.onSave = onSave
        _hasDueDate = State(initialValue: task.dueAt != nil)
        _dueDate = State(initialValue: task.dueAt ?? Date().addingTimeInterval(3600))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Button {
                        task.status = task.isDone ? TaskState.open : TaskState.done
                    } label: {
                        Label(
                            task.isDone ? "Reopen" : "Done",
                            systemImage: task.isDone ? "arrow.uturn.backward" : "checkmark.circle.fill"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }

                Section("Task") {
                    TextField("What needs to happen", text: $task.title, axis: .vertical)
                    TextField("Contact", text: Binding(
                        get: { task.contactName ?? "" },
                        set: { task.contactName = $0.isEmpty ? nil : $0 }
                    ))
                }

                Section("Due") {
                    Toggle("Has a deadline", isOn: $hasDueDate)
                    if hasDueDate {
                        DatePicker("When", selection: $dueDate)
                        Toggle("All day", isOn: $task.allDay)
                    }
                }

                Section("Classification") {
                    Picker("Priority", selection: $task.priority) {
                        Text("High").tag(Priority.high)
                        Text("Normal").tag(Priority.normal)
                        Text("Low").tag(Priority.low)
                    }
                    Picker("Status", selection: $task.status) {
                        Text("Open").tag(TaskState.open)
                        Text("Done").tag(TaskState.done)
                        Text("Cancelled").tag(TaskState.cancelled)
                    }
                }

                Section("From the source") {
                    LabeledContent("Type", value: Formatting.source(task.source))
                    LabeledContent("Created", value: Formatting.dateTime(task.createdAt))
                    LabeledContent("Confidence", value: Formatting.confidence(task.confidence))
                    if let details = task.details, !details.isEmpty {
                        Text(details).font(.footnote)
                    }
                    if let quote = task.quote, !quote.isEmpty {
                        Text(verbatim: "“\(quote)”")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }

                Section {
                    Button("Delete the task", role: .destructive) {
                        ReminderScheduler.cancel(taskID: task.id)
                        context.delete(task)
                        onSave()
                        dismiss()
                    }
                }
            }
            .navigationTitle("Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        task.dueAt = hasDueDate ? dueDate : nil
                        task.reminderAt = task.status == TaskState.open
                            ? ReminderScheduler.reminderTime(
                                for: task,
                                offsetMinutes: settings.reminderOffsetMinutes
                            )
                            : nil
                        ReminderScheduler.cancel(taskID: task.id)
                        ReminderScheduler.schedule(task)
                        onSave()
                        dismiss()
                    }
                }
            }
        }
    }
}

struct NewTaskView: View {

    let onAdd: (String, String?, Date?) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var contact = ""
    @State private var hasDueDate = false
    @State private var dueDate = Date().addingTimeInterval(3600)

    var body: some View {
        NavigationStack {
            Form {
                Section("Task") {
                    TextField("What needs to happen", text: $title, axis: .vertical)
                    TextField("Contact (optional)", text: $contact)
                }
                Section("Due") {
                    Toggle("Has a deadline", isOn: $hasDueDate)
                    if hasDueDate {
                        DatePicker("When", selection: $dueDate)
                    }
                }
            }
            .navigationTitle("New task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(
                            title.trimmingCharacters(in: .whitespacesAndNewlines),
                            contact.isEmpty ? nil : contact,
                            hasDueDate ? dueDate : nil
                        )
                        dismiss()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
