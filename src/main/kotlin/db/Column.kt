package org.example.db

enum class DataType { INT, TEXT }

data class Column(
    val name: String,
    val type: DataType,
    val primary: Boolean = false,
    val unique: Boolean = false
)
