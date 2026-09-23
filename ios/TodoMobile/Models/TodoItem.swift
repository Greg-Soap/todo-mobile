import Combine
import Foundation
import SwiftUI

struct TodoItem: Identifiable, Equatable, Codable {
    let id: UUID
    var text: String
    var isCompleted: Bool

    init(id: UUID = UUID(), text: String, isCompleted: Bool = false) {
        self.id = id
        self.text = text
        self.isCompleted = isCompleted
    }
}

@MainActor
final class TodoStore: ObservableObject {
    @Published var todos: [TodoItem] {
        didSet { persist() }
    }

    private let storageKey = "todo-mobile.todos"

    init() {
        if let data = UserDefaults.standard.data(forKey: storageKey),
           let decoded = try? JSONDecoder().decode([TodoItem].self, from: data) {
            todos = decoded
        } else {
            todos = [
                TodoItem(text: "Set up the project", isCompleted: true),
                TodoItem(text: "Add a new todo"),
                TodoItem(text: "Drag items to reorder"),
            ]
        }
    }

    func add(_ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        todos.insert(TodoItem(text: trimmed), at: 0)
    }

    func toggle(_ todo: TodoItem) {
        guard let index = todos.firstIndex(of: todo) else { return }
        todos[index].isCompleted.toggle()
    }

    func delete(_ todo: TodoItem) {
        todos.removeAll { $0.id == todo.id }
    }

    func move(from source: IndexSet, to destination: Int) {
        todos.move(fromOffsets: source, toOffset: destination)
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(todos) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }
}
