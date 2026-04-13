package com.JoseRosas.registrador.filters

import com.JoseRosas.registrador.model.EventType

class EventFilter(
    private val debounceMs: Long = 3000L,
    private val messageCooldownMs: Long = 5 * 60 * 1000L,
    private val duplicateMessageWindowMs: Long = 24 * 60 * 60 * 1000L
) {

    private val lastEventByNumber = mutableMapOf<String, Long>()
    private val lastMessageSentByNumber = mutableMapOf<String, Long>()
    private val lastProcessedMessageSignature = mutableMapOf<String, Long>()

    fun shouldSend(number: String, type: EventType, messageText: String?): Boolean {
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
                val normalizedMessage = normalizeMessage(messageText)

                if (normalizedMessage.isNotEmpty()) {
                    val signature = "$key|$normalizedMessage"
                    val lastSeen = lastProcessedMessageSignature[signature]
                    if (lastSeen != null && now - lastSeen < duplicateMessageWindowMs) {
                        return false
                    }
                    lastProcessedMessageSignature[signature] = now
                    cleanupOldSignatures(now)
                    return true
                }

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

            EventType.OUTGOING_CALL -> false
            EventType.UNKNOWN -> false
        }
    }

    private fun cleanupOldSignatures(now: Long) {
        val iterator = lastProcessedMessageSignature.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > duplicateMessageWindowMs) {
                iterator.remove()
            }
        }
    }

    private fun normalize(number: String): String {
        var n = number.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (n.startsWith("+591")) {
            n = n.substring(4)
        } else if (n.startsWith("591")) {
            n = n.substring(3)
        }

        return n
    }

    private fun normalizeMessage(messageText: String?): String {
        if (messageText.isNullOrBlank()) return ""

        return messageText.trim()
            .lowercase()
            .replace("\\s+".toRegex(), " ")
    }
}