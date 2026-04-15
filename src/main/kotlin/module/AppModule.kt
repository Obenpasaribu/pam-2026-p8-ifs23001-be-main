package org.delcom.module

import org.delcom.repositories.*
import org.delcom.services.AuthService
import org.delcom.services.TodoService
import org.delcom.services.UserService
import org.koin.dsl.module
import io.ktor.server.application.*

fun appModule(application: Application) = module {
    val baseUrl = application.environment.config
        .property("ktor.app.baseUrl")
        .getString()
        .trimEnd('/')   // Pastikan tidak ada trailing slash

    val jwtSecret = application.environment.config
        .property("ktor.jwt.secret")
        .getString()

    // User Repository
    single<IUserRepository> {
        UserRepository(baseUrl)
    }

    // Refresh Token Repository
    single<IRefreshTokenRepository> {
        RefreshTokenRepository()
    }

    // User Service
    single {
        UserService(get(), get(), baseUrl)
    }

    // Auth Service
    single {
        AuthService(jwtSecret, get(), get())
    }

    // Todo Repository
    single<ITodoRepository> {
        TodoRepository(baseUrl)
    }

    // Todo Service
    single {
        TodoService(get(), get())
    }
}
