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
