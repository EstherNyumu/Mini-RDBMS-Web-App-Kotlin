package org.example.engine

import org.example.db.Column
import org.example.db.DataType
import org.example.db.Database
import org.example.db.Index
import org.example.db.Table
import org.example.sql.Command
import org.example.sql.CreateTable
import org.example.sql.Delete
import org.example.sql.Insert
import org.example.sql.Join
import org.example.sql.Select
import org.example.sql.Update

class Executor(private val db: Database) {

    fun execute(cmd: Command): List<Map<String, Any>> {
        return when (cmd) {
            is CreateTable -> {
                if (db.tables.containsKey(cmd.name)) return emptyList()
                val columns = cmd.columns.map {
                    Column(
                        name = it.first,
                        type = DataType.valueOf(it.second.uppercase()),
                        primary = it.third?.uppercase() == "PRIMARY"
                    )
                }
                val table = Table(cmd.name, columns)
                columns.filter { it.primary }.forEach { table.indexes[it.name] = Index() }
                db.tables[cmd.name] = table
                emptyList()
            }

            is Insert -> {
                val table = db.tables[cmd.table]!!
                val row = mutableMapOf<String, Any>()
                table.columns.forEachIndexed { i, col ->
                    row[col.name] = when (col.type) {
                        DataType.INT -> cmd.values[i].toString().toInt()
                        DataType.TEXT -> cmd.values[i].toString()
                    }
                }
                table.columns.filter { it.primary }.forEach { pk ->
                    val key = row[pk.name]!!
                    val index = table.indexes[pk.name]!!
                    if (index.get(key) != null) { throw IllegalArgumentException("Duplicate primary key: $key") }
                    index.add(key, row)
                }
                table.rows.add(row)
                emptyList()
            }

            is Select -> {
                val table = db.tables[cmd.table]!!
                table.rows.filter {
                    cmd.whereColumn == null || it[cmd.whereColumn].toString() == cmd.whereValue
                }
            }

            is Update -> {
                val table = db.tables[cmd.table]!!
                table.rows.forEach {
                    if (it[cmd.whereColumn].toString() == cmd.whereValue) { it[cmd.column] = cmd.value }
                }
                emptyList()
            }

            is Delete -> {
                val table = db.tables[cmd.table]!!
                table.rows.removeIf { it[cmd.whereColumn].toString() == cmd.whereValue }
                emptyList()
            }

            is Join -> {
                val t1 = db.tables[cmd.table1]!!
                val t2 = db.tables[cmd.table2]!!
                val results = mutableListOf<Map<String, Any>>()
                for (r1 in t1.rows) {
                    val key = r1[cmd.col1]
                    val match = t2.indexes[cmd.col2]?.get(key)
                    if (match != null) { results.add(r1 + match) }
                }
                results
            }
        }
    }
}
