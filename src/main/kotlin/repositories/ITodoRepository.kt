package org.delcom.repositories

import org.delcom.data.TodoListResponse
import org.delcom.data.TodoStats
import org.delcom.entities.Todo

interface ITodoRepository {
    suspend fun getAll(
        userId: String, 
        search: String, 
        status: String = "all", 
        page: Int = 1, 
        perPage: Int = 10
    ): TodoListResponse
    suspend fun getById(todoId: String): Todo?
    suspend fun create(todo: Todo): String
    suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean
    suspend fun delete(userId: String, todoId: String): Boolean
    suspend fun getStats(userId: String): TodoStats
}