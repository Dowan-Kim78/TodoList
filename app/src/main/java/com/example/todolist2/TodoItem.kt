package com.example.todolist2

import kotlinx.serialization.Serializable

enum class ItemType {
    TODO, SHOPPING
}

@Serializable
data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isCompleted: Boolean = false,
    val type: ItemType = ItemType.TODO
)
