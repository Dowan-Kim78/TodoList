package com.example.todolist2

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isCompleted: Boolean = false
)
