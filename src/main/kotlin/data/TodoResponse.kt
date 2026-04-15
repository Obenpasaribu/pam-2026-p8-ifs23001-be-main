package org.delcom.data

import kotlinx.serialization.Serializable
import org.delcom.entities.Todo

@Serializable
data class TodoListResponse(
    val todos: List<Todo>,
    val total: Long,
    val page: Int,
    val perPage: Int
)

@Serializable
data class TodoStats(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val percentage: Double
)
