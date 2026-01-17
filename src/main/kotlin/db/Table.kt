package org.example.db

class Table(
    val name: String,
    val columns: List<Column>
) {
    val rows = mutableListOf<MutableMap<String, Any>>()
    val indexes = mutableMapOf<String, Index>()
}
