package com.JoseRosas.registrador.model

enum class EventType {
    MESSAGE,
    CALL,
    UNKNOWN
}

data class NotificationEvent(
    val contactName: String,
    val number: String,
    val messageText: String?,
    val type: EventType,
    val timestamp: Long = System.currentTimeMillis()
)