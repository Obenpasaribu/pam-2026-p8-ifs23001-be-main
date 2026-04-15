package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.data.TodoListResponse
import org.delcom.data.TodoStats
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.slf4j.LoggerFactory
import java.util.*

// Helper untuk menghindari crash saat parsing UUID
fun String.toUUIDOrNull(): UUID? = try { UUID.fromString(this) } catch (e: Exception) { null }

class TodoRepository(private val baseUrl: String) : ITodoRepository {
    private val logger = LoggerFactory.getLogger(TodoRepository::class.java)

    override suspend fun getAll(
        userId: String, 
        search: String, 
        status: String, 
        page: Int, 
        perPage: Int
    ): TodoListResponse = suspendTransaction {
        val userUuid = userId.toUUIDOrNull() ?: return@suspendTransaction TodoListResponse(emptyList(), 0, page, perPage)
        
        logger.info("Fetching todos for user: $userId, search: $search, status: $status, page: $page")

        val conditions = buildConditions(userUuid, search, status)

        val total = TodoTable.selectAll().where(conditions).count()
        val offset = ((page - 1) * perPage).toLong()

        val todos = TodoDAO
            .find(conditions)
            .orderBy(TodoTable.createdAt to SortOrder.DESC)
            .limit(perPage)
            .offset(offset)
            .toList() // Execute query here
            .map { todoDAOToModel(it, baseUrl) }

        logger.info("Found ${todos.size} todos out of $total total")

        TodoListResponse(
            todos = todos,
            total = total,
            page = page,
            perPage = perPage
        )
    }

    private fun buildConditions(
        userUuid: UUID,
        search: String,
        status: String,
    ): Op<Boolean> {
        var conditions: Op<Boolean> = TodoTable.userId eq userUuid

        if (search.isNotBlank()) {
            conditions = conditions and (TodoTable.title.lowerCase() like "%${search.lowercase()}%")
        }

        // PERBAIKAN LOGIKA: Pastikan perbandingan string bersih
        val statusClean = status.lowercase().trim()
        when (statusClean) {
            "completed" -> {
                conditions = conditions and (TodoTable.isDone eq true)
            }
            "pending" -> {
                conditions = conditions and (TodoTable.isDone eq false)
            }
            // "all", "semua", atau string lain tidak menambah filter isDone
        }

        return conditions
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

    override suspend fun getStats(userId: String): TodoStats = suspendTransaction {
        val userUuid = userId.toUUIDOrNull() ?: return@suspendTransaction TodoStats(
            total = 0, completed = 0, pending = 0, percentage = 0.0
        )
        
        val allTodos = TodoDAO.find { TodoTable.userId eq userUuid }.toList()
        
        val total = allTodos.size
        val completed = allTodos.count { it.isDone }
        val pending = total - completed
        val percentage = if (total > 0) (completed.toDouble() / total.toDouble() * 100.0) else 0.0

        TodoStats(
            total = total,
            completed = completed,
            pending = pending,
            percentage = percentage
        )
    }

}
