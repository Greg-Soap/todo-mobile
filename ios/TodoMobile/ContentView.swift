import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var store: TodoStore
    @State private var draft = ""
    @State private var isEditing = true

    private var completedCount: Int {
        store.todos.filter(\.isCompleted).count
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    TextField("What needs doing?", text: $draft)
                        .textFieldStyle(.roundedBorder)
                        .submitLabel(.done)
                        .onSubmit(addTodo)

                    Button(action: addTodo) {
                        Image(systemName: "plus")
                            .fontWeight(.semibold)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityLabel("Add todo")
                }
                .padding()

                if store.todos.isEmpty {
                    ContentUnavailableView(
                        "No todos yet",
                        systemImage: "checklist",
                        description: Text("Add one above, then drag to reorder.")
                    )
                } else {
                    List {
                        ForEach(store.todos) { todo in
                            TodoRowView(
                                todo: todo,
                                onToggle: { store.toggle(todo) },
                                onDelete: { store.delete(todo) }
                            )
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        }
                        .onMove(perform: store.move)
                        .onDelete { indexSet in
                            for index in indexSet {
                                store.delete(store.todos[index])
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                    .environment(\.editMode, .constant(isEditing ? .active : .inactive))
                }
            }
            .navigationTitle("Todos")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(isEditing ? "Done" : "Reorder") {
                        isEditing.toggle()
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                if !store.todos.isEmpty {
                    Text("\(completedCount) of \(store.todos.count) completed — drag the grip to reorder")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial)
                }
            }
        }
    }

    private func addTodo() {
        store.add(draft)
        draft = ""
    }
}

#Preview {
    ContentView()
        .environmentObject(TodoStore())
}
