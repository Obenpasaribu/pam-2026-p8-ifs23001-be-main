package org.delcom.data

import kotlinx.serialization.Serializable

@Serializable
data class TodoStats(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val percentage: Double
)