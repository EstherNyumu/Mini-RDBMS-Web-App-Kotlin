package org.example.api

import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val id: Int,
    val userId: Int,
    val status: String
)