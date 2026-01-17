package org.example.api

import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val name: String,
    val password: String
)

@Serializable
data class UpdateUserRequest(
    val name: String
)

@Serializable
data class OrderRequest(
    val userId: Int,
    val status: String
)

@Serializable
data class UpdateOrderRequest(
    val status: String
)
