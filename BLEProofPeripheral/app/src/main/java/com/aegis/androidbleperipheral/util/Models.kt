package com.aegis.androidbleperipheral.util

//region Permissions and Settings management
enum class AskType {
    AskOnce,
    InsistUntilSuccess
}

data class KeyValuePair(
    val key: String,
    val value: String,
)