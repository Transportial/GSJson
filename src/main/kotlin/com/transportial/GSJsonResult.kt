package com.transportial

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode


/**
 * Result type for GSJson operations
 */
data class GSJsonResult(
    val value: Any?,
    val raw: String,
    val type: ResultType,
    val exists: Boolean = true,
    val index: Int = -1
) {
    enum class ResultType {
        STRING, NUMBER, BOOLEAN, NULL, OBJECT, ARRAY
    }

    fun string(): String = value?.toString() ?: ""
    fun int(): Int = when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }
    fun double(): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    fun boolean(): Boolean = when (value) {
        is Boolean -> value
        is String -> value.toBoolean()
        else -> false
    }

    fun array(): List<GSJsonResult> {
        return when (value) {
            is ArrayNode -> value.map { GSJsonResult(it, it.toString(), getResultType(it)) }
            is List<*> -> value.map { GSJsonResult(it, it.toString(), getResultType(it)) }
            else -> listOf(this)
        }
    }

    fun forEach(action: (GSJsonResult) -> Unit) {
        array().forEach(action)
    }

    private fun getResultType(value: Any?): ResultType {
        return when (value) {
            is String -> ResultType.STRING
            is Number -> ResultType.NUMBER
            is Boolean -> ResultType.BOOLEAN
            is ArrayNode -> ResultType.ARRAY
            is ObjectNode -> ResultType.OBJECT
            null -> ResultType.NULL
            else -> ResultType.STRING
        }
    }
}
