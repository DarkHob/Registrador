package com.JoseRosas.registrador.filters

class EventFilter(
    private val debounceMs: Long = 3000L,
    private val globalCooldownMs: Long = 5 * 60 * 1000L,
    private val duplicateMessageWindowMs: Long = 24 * 60 * 60 * 1000L
) {

    private val lastEventByNumber = mutableMapOf<String, Long>()
    private val lastProcessedMessageSignature = mutableMapOf<String, Long>()

    fun shouldSend(number: String, messageText: String?): Boolean {
        val now = System.currentTimeMillis()
        val key = normalize(number)

        val lastEventTime = lastEventByNumber[key]

        // anti-rebote corto
        if (lastEventTime != null && now - lastEventTime < debounceMs) {
            return false
        }

        // evitar repetir exactamente el mismo mensaje por mucho tiempo
        val normalizedMessage = normalizeMessage(messageText)
        if (normalizedMessage.isNotEmpty()) {
            val signature = "$key|$normalizedMessage"
            val lastSeen = lastProcessedMessageSignature[signature]
            if (lastSeen != null && now - lastSeen < duplicateMessageWindowMs) {
                return false
            }
            lastProcessedMessageSignature[signature] = now
            cleanupOldSignatures(now)
        }

        // cooldown global por número
        if (lastEventTime != null && now - lastEventTime < globalCooldownMs) {
            return false
        }

        lastEventByNumber[key] = now
        return true
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