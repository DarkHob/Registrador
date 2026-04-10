package com.JoseRosas.registrador.filters

import com.JoseRosas.registrador.model.EventType

class EventFilter(
    private val debounceMs: Long = 3000L,
    private val messageCooldownMs: Long = 5 * 60 * 1000L
) {

    private val lastEventByNumber = mutableMapOf<String, Long>()
    private val lastMessageSentByNumber = mutableMapOf<String, Long>()

    fun shouldSend(number: String, type: EventType): Boolean {
        val now = System.currentTimeMillis()
        val key = normalize(number)

        val lastEventTime = lastEventByNumber[key]
        if (lastEventTime != null) {
            val diff = now - lastEventTime
            if (diff < debounceMs) {
                return false
            }
        }
        lastEventByNumber[key] = now

        return when (type) {
            EventType.CALL -> true

            EventType.MESSAGE -> {
                val lastMessageTime = lastMessageSentByNumber[key]
                if (lastMessageTime != null) {
                    val diff = now - lastMessageTime
                    if (diff < messageCooldownMs) {
                        return false
                    }
                }
                lastMessageSentByNumber[key] = now
                true
            }

            EventType.UNKNOWN -> false
        }
    }

    private fun normalize(number: String): String {
        var n = number.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        // 🔥 quitar código país Bolivia
        if (n.startsWith("+591")) {
            n = n.substring(4)
        } else if (n.startsWith("591")) {
            n = n.substring(3)
        }

        return n
    }
}