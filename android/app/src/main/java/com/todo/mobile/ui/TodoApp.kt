package com.todo.mobile.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.todo.mobile.data.TodoItem
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(viewModel: TodoViewModel) {
    val draft by viewModel.draft.collectAsState()
    val todos = viewModel.todos
    val completedCount = todos.count { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todos") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = viewModel::updateDraft,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("What needs doing?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.addTodo() }),
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = viewModel::addTodo,
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add todo")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (todos.isEmpty()) {
                    "Add tasks and long-press the grip to reorder them."
                } else {
                    "$completedCount of ${todos.size} completed — long-press drag handle to reorder"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No todos yet. Add one above to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                ReorderableTodoList(
                    todos = todos,
                    onToggle = viewModel::toggleTodo,
                    onDelete = viewModel::deleteTodo,
                    onMove = viewModel::moveTodo,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReorderableTodoList(
    todos: List<TodoItem>,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(todos, key = { _, item -> item.id }) { index, todo ->
            val isDragging = index == draggingIndex
            val elevation by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 1.dp,
                label = "elevation",
            )

            TodoRow(
                todo = todo,
                isDragging = isDragging,
                dragOffsetY = if (isDragging) dragOffsetY else 0f,
                elevation = elevation,
                onToggle = { onToggle(todo.id) },
                onDelete = { onDelete(todo.id) },
                onDragStart = {
                    draggingIndex = index
                    dragOffsetY = 0f
                },
                onDrag = { change ->
                    dragOffsetY += change.y
                    val itemHeightPx = with(density) { 64.dp.toPx() }
                    val shift = (dragOffsetY / itemHeightPx).roundToInt()
                    val target = (draggingIndex + shift).coerceIn(0, todos.lastIndex)
                    if (target != draggingIndex && draggingIndex >= 0) {
                        onMove(draggingIndex, target)
                        draggingIndex = target
                        dragOffsetY -= shift * itemHeightPx
                    }
                },
                onDragEnd = {
                    draggingIndex = -1
                    dragOffsetY = 0f
                },
            )
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoItem,
    isDragging: Boolean,
    dragOffsetY: Float,
    elevation: androidx.compose.ui.unit.Dp,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer { translationY = dragOffsetY }
            .shadow(elevation, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .pointerInput(todo.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            },
                        )
                    },
            )

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (todo.isCompleted) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (todo.isCompleted) {
                        "Mark incomplete"
                    } else {
                        "Mark complete"
                    },
                )
            }

            Text(
                text = todo.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isCompleted) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                },
                color = if (todo.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete todo",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
