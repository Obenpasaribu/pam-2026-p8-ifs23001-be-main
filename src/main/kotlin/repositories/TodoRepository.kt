package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class TodoRepository(private val baseUrl: String) : ITodoRepository {
    override suspend fun getAll(userId: String, search: String): List<Todo> = suspendTransaction {
        if (search.isBlank()) {
            TodoDAO
                .find {
                    (TodoTable.userId eq UUID.fromString(userId))
                }
                .orderBy(TodoTable.createdAt to SortOrder.DESC)
                .map{ todoDAOToModel(it, baseUrl) }
        } else {
            val keyword = "%${search.lowercase()}%"

            TodoDAO
                .find {
                    (TodoTable.userId eq UUID.fromString(userId)) and (TodoTable.title.lowerCase() like keyword)
                }
                .orderBy(TodoTable.title to SortOrder.ASC)
                .map{ todoDAOToModel(it, baseUrl) }
        }
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId))
            }
            .limit(1)
            .map{ todoDAOToModel(it, baseUrl) }
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val todoDAO = TodoDAO.new {
            userId = UUID.fromString(todo.userId)
            title = todo.title
            description = todo.description
            cover = todo.cover
            isDone = todo.isDone
            createdAt = todo.createdAt
            updatedAt = todo.updatedAt
        }

        todoDAO.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (todoDAO != null) {
            todoDAO.title = newTodo.title
            todoDAO.description = newTodo.description
            todoDAO.cover = newTodo.cover
            todoDAO.isDone = newTodo.isDone
            todoDAO.updatedAt = newTodo.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }

    override suspend fun getStats(userId: String): Map<String, Any> = suspendTransaction {
        val userUuid = UUID.fromString(userId)
        val total = TodoTable.selectAll().where { TodoTable.userId eq userUuid }.count()
        val completed = TodoTable.selectAll().where { (TodoTable.userId eq userUuid) and (TodoTable.isDone eq true) }.count()
        val pending = total - completed
        val percentage = if (total > 0) (completed.toDouble() / total.toDouble() * 100) else 0.0

        mapOf(
            "total" to total,
            "completed" to completed,
            "pending" to pending,
            "percentage" to percentage
        )
    }

}