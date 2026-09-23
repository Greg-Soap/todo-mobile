package com.todo.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todo.mobile.ui.TodoApp
import com.todo.mobile.ui.TodoTheme
import com.todo.mobile.ui.TodoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: TodoViewModel = viewModel()
                    TodoApp(viewModel = viewModel)
                }
            }
        }
    }
}
