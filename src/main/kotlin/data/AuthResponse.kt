package org.delcom.data

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val authToken: String,
    val refreshToken: String,
    val userId: String = "",
    val redirectTo: String = "/home"
)