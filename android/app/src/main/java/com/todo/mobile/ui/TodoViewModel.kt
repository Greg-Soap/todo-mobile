package com.todo.mobile.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todo.mobile.data.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _todos = mutableStateListOf<TodoItem>()
    val todos: SnapshotStateList<TodoItem> = _todos

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    init {
        load()
    }

    fun updateDraft(value: String) {
        _draft.value = value
    }

    fun addTodo() {
        val text = _draft.value.trim()
        if (text.isEmpty()) return
        _todos.add(0, TodoItem(text = text))
        _draft.value = ""
        persist()
    }

    fun toggleTodo(id: String) {
        val index = _todos.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = _todos[index]
        _todos[index] = current.copy(isCompleted = !current.isCompleted)
        persist()
    }

    fun deleteTodo(id: String) {
        _todos.removeAll { it.id == id }
        persist()
    }

    fun moveTodo(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in _todos.indices || toIndex !in _todos.indices) return
        val item = _todos.removeAt(fromIndex)
        _todos.add(toIndex, item)
        persist()
    }

    private fun load() {
        val raw = prefs.getString(KEY_TODOS, null)
        if (raw.isNullOrBlank()) {
            _todos.addAll(DEFAULT_TODOS)
            persist()
            return
        }

        runCatching {
            val array = JSONArray(raw)
            val loaded = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        TodoItem(
                            id = obj.getString("id"),
                            text = obj.getString("text"),
                            isCompleted = obj.getBoolean("isCompleted"),
                        )
                    )
                }
            }
            _todos.clear()
            _todos.addAll(loaded)
        }.onFailure {
            _todos.clear()
            _todos.addAll(DEFAULT_TODOS)
            persist()
        }
    }

    private fun persist() {
        viewModelScope.launch(Dispatchers.IO) {
            val array = JSONArray()
            _todos.forEach { todo ->
                array.put(
                    JSONObject()
                        .put("id", todo.id)
                        .put("text", todo.text)
                        .put("isCompleted", todo.isCompleted)
                )
            }
            prefs.edit().putString(KEY_TODOS, array.toString()).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "todo_mobile"
        private const val KEY_TODOS = "todos"

        private val DEFAULT_TODOS = listOf(
            TodoItem(text = "Set up the project", isCompleted = true),
            TodoItem(text = "Add a new todo"),
            TodoItem(text = "Drag items to reorder"),
        )
    }
}
