package org.example.db

class Index {
    private val map = mutableMapOf<Any, MutableMap<String, Any>>()

    fun add(key: Any, row: MutableMap<String, Any>) {
        map[key] = row
    }

    fun get(key: Any?): MutableMap<String, Any>? = map[key]
}
