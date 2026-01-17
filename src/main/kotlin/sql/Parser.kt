package org.example.sql

object Parser {

    fun parse(input: String): Command {
        val tokens = input.trim().split(" ")

        return when (tokens[0].uppercase()) {
            "CREATE" -> parseCreate(input)
            "INSERT" -> parseInsert(input)
            "SELECT" -> parseSelect(tokens)
            "UPDATE" -> parseUpdate(tokens)
            "DELETE" -> parseDelete(tokens)
            "JOIN" -> parseJoin(tokens)
            else -> throw IllegalArgumentException("Unknown command")
        }
    }

    private fun parseCreate(sql: String): Command {
        val name = sql.split(" ")[2]
        val cols = sql.substringAfter("(").substringBefore(")")
            .split(",")
            .map {
                val parts = it.trim().split(" ")
                Triple(
                    parts[0],
                    parts[1],
                    parts.getOrNull(2)
                )
            }
        return CreateTable(name, cols)
    }

    private fun parseInsert(sql: String): Command {
        val table = sql.split(" ")[2]
        val values = sql.substringAfter("(")
            .substringBefore(")")
            .split(",")
            .map { it.trim().replace("'", "") }
        return Insert(table, values)
    }

    private fun parseSelect(tokens: List<String>): Command {
        val table = tokens[3]
        return if (tokens.contains("WHERE")) {
            val idx = tokens.indexOf("WHERE")
            Select(table, tokens[idx + 1], tokens[idx + 3])
        } else {
            Select(table, null, null)
        }
    }

    private fun parseUpdate(tokens: List<String>): Command {
        if (
            tokens.size < 10 ||
            tokens[0].uppercase() != "UPDATE" ||
            tokens[2].uppercase() != "SET" ||
            tokens[6].uppercase() != "WHERE" ||
            tokens[4] != "=" ||
            tokens[8] != "="
        ) {
            throw IllegalArgumentException("Invalid UPDATE syntax")
        }
        return Update(
            table = tokens[1],
            column = tokens[3],
            value = tokens[5],
            whereColumn = tokens[7],
            whereValue = tokens[9]
        )
    }

    private fun parseDelete(tokens: List<String>): Command {
        if (tokens.size < 5) {
            throw IllegalArgumentException("Invalid DELETE syntax")
        }

        val table = tokens[2]
        val whereColumn = tokens[4].substringBefore("=")
        val whereValue = tokens[4].substringAfter("=")
        return Delete(table, whereColumn, whereValue)
    }

    private fun parseJoin(tokens: List<String>): Command {
        if (tokens.size < 6) throw IllegalArgumentException("Invalid JOIN syntax")
        return Join(
            table1 = tokens[1],
            table2 = tokens[2],
            col1 = tokens[4],
            col2 = tokens[5]
        )
    }

}
