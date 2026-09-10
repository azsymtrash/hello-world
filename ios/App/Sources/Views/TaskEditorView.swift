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
                            task.isDone ? "Върни като отворена" : "Направено",
                            systemImage: task.isDone ? "arrow.uturn.backward" : "checkmark.circle.fill"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }

                Section("Задача") {
                    TextField("Какво трябва да се направи", text: $task.title, axis: .vertical)
                    TextField("Контакт", text: Binding(
                        get: { task.contactName ?? "" },
                        set: { task.contactName = $0.isEmpty ? nil : $0 }
                    ))
                }

                Section("Краен срок") {
                    Toggle("Има срок", isOn: $hasDueDate)
                    if hasDueDate {
                        DatePicker("Кога", selection: $dueDate)
                        Toggle("Цял ден", isOn: $task.allDay)
                    }
                }

                Section("Класификация") {
                    Picker("Приоритет", selection: $task.priority) {
                        Text("Висок").tag(Priority.high)
                        Text("Нормален").tag(Priority.normal)
                        Text("Нисък").tag(Priority.low)
                    }
                    Picker("Статус", selection: $task.status) {
                        Text("Отворено").tag(TaskState.open)
                        Text("Готово").tag(TaskState.done)
                        Text("Отказано").tag(TaskState.cancelled)
                    }
                }

                Section("От източника") {
                    LabeledContent("Тип", value: Formatting.source(task.source))
                    LabeledContent("Създадено", value: Formatting.dateTime(task.createdAt))
                    LabeledContent("Увереност", value: Formatting.confidence(task.confidence))
                    if let details = task.details, !details.isEmpty {
                        Text(details).font(.footnote)
                    }
                    if let quote = task.quote, !quote.isEmpty {
                        Text("„\(quote)“")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }

                Section {
                    Button("Изтрий задачата", role: .destructive) {
                        ReminderScheduler.cancel(taskID: task.id)
                        context.delete(task)
                        onSave()
                        dismiss()
                    }
                }
            }
            .navigationTitle("Задача")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Затвори") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Запази") {
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
                Section("Задача") {
                    TextField("Какво трябва да се направи", text: $title, axis: .vertical)
                    TextField("Контакт (по избор)", text: $contact)
                }
                Section("Краен срок") {
                    Toggle("Има срок", isOn: $hasDueDate)
                    if hasDueDate {
                        DatePicker("Кога", selection: $dueDate)
                    }
                }
            }
            .navigationTitle("Нова задача")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отказ") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Добави") {
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
