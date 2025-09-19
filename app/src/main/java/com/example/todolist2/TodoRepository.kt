package com.example.todolist2

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class TodoRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    companion object {
        private const val TODO_LIST_KEY = "todo_list"
        private const val SHOPPING_LIST_KEY = "shopping_list"
    }
    
    fun saveTodos(todos: List<TodoItem>) {
        try {
            val todosJson = json.encodeToString(todos)
            sharedPreferences.edit()
                .putString(TODO_LIST_KEY, todosJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun saveShoppingItems(items: List<TodoItem>) {
        try {
            val itemsJson = json.encodeToString(items)
            sharedPreferences.edit()
                .putString(SHOPPING_LIST_KEY, itemsJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun loadTodos(): List<TodoItem> {
        return try {
            val todosJson = sharedPreferences.getString(TODO_LIST_KEY, null)
            if (todosJson != null) {
                json.decodeFromString<List<TodoItem>>(todosJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun loadShoppingItems(): List<TodoItem> {
        return try {
            val itemsJson = sharedPreferences.getString(SHOPPING_LIST_KEY, null)
            if (itemsJson != null) {
                json.decodeFromString<List<TodoItem>>(itemsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun clearTodos() {
        sharedPreferences.edit()
            .remove(TODO_LIST_KEY)
            .apply()
    }
    
    fun clearShoppingItems() {
        sharedPreferences.edit()
            .remove(SHOPPING_LIST_KEY)
            .apply()
    }
}
