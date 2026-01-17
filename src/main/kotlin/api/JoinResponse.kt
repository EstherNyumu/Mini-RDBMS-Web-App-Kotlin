package org.example.api

import kotlinx.serialization.Serializable

@Serializable
data class JoinResponse(
    val orderId: Int,
    val userId: Int,
    val userName: String,
    val status: String
)