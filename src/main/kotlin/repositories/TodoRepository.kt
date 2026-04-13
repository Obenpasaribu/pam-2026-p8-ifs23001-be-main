package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

// Helper untuk menghindari crash saat parsing UUID
fun String.toUUIDOrNull(): UUID? = try { UUID.fromString(this) } catch (e: Exception) { null }

class TodoRepository(private val baseUrl: String) : ITodoRepository {
    override suspend fun getAll(
        userId: String, 
        search: String, 
        status: String, 
        page: Int, 
        perPage: Int
    ): List<Todo> = suspendTransaction {
        val userUuid = userId.toUUIDOrNull() ?: return@suspendTransaction emptyList()
        
        val query = TodoTable.selectAll().where { TodoTable.userId eq userUuid }

        if (search.isNotBlank()) {
            query.andWhere { TodoTable.title.lowerCase() like "%${search.lowercase()}%" }
        }

        when (status.lowercase()) {
            "completed" -> query.andWhere { TodoTable.isDone eq true }
            "pending" -> query.andWhere { TodoTable.isDone eq false }
        }

        val offset = ((page - 1) * perPage).toLong()

        TodoDAO.wrapRows(query)
            .orderBy(TodoTable.createdAt to SortOrder.DESC)
            .limit(perPage)
            .offset(offset)
            .map { todoDAOToModel(it, baseUrl) }
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        val uuid = todoId.toUUIDOrNull() ?: return@suspendTransaction null
        TodoDAO
            .find {
                (TodoTable.id eq uuid)
            }
            .limit(1)
            .map{ todoDAOToModel(it, baseUrl) }
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val userUuid = todo.userId.toUUIDOrNull() ?: throw IllegalArgumentException("Invalid User ID")
        val todoDAO = TodoDAO.new {
            userId = userUuid
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
        val uuid = todoId.toUUIDOrNull() ?: return@suspendTransaction false
        val userUuid = userId.toUUIDOrNull() ?: return@suspendTransaction false
        
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq uuid) and
                        (TodoTable.userId eq userUuid)
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
        val uuid = todoId.toUUIDOrNull() ?: return@suspendTransaction false
        val userUuid = userId.toUUIDOrNull() ?: return@suspendTransaction false
        
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq uuid) and
                    (TodoTable.userId eq userUuid)
        }
        rowsDeleted >= 1
    }

    override suspend fun getStats(userId: String): Map<String, Any> = suspendTransaction {
        val userUuid = userId.toUUIDOrNull() ?: return@suspendTransaction mapOf(
            "total" to 0, "completed" to 0, "pending" to 0, "percentage" to 0.0
        )
        
        val allTodos = TodoDAO.find { TodoTable.userId eq userUuid }.toList()
        
        val total = allTodos.size
        val completed = allTodos.count { it.isDone }
        val pending = total - completed
        val percentage = if (total > 0) (completed.toDouble() / total.toDouble() * 100.0) else 0.0

        mapOf(
            "total" to total,
            "completed" to completed,
            "pending" to pending,
            "percentage" to percentage
        )
    }

}