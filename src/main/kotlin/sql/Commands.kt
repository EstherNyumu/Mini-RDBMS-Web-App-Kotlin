package org.example.sql

sealed class Command

data class CreateTable(
    val name: String,
    val columns: List<Triple<String, String, String?>>
) : Command()

data class Insert(
    val table: String,
    val values: List<String>
) : Command()

data class Select(
    val table: String,
    val whereColumn: String?,
    val whereValue: String?
) : Command()

data class Update(
    val table: String,
    val column: String,
    val value: String,
    val whereColumn: String,
    val whereValue: String
) : Command()

data class Delete(
    val table: String,
    val whereColumn: String,
    val whereValue: String
) : Command()

data class Join(
    val table1: String,
    val table2: String,
    val col1: String,
    val col2: String
) : Command()


