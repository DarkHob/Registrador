package com.JoseRosas.registrador.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.JoseRosas.registrador.contacts.ContactResolver
import com.JoseRosas.registrador.filters.EventFilter
import com.JoseRosas.registrador.model.EventType
import com.JoseRosas.registrador.network.AppConfig
import com.JoseRosas.registrador.network.TcpClient
import com.JoseRosas.registrador.util.AppLogger

class WhatsAppNotificationListener : NotificationListenerService() {

    private lateinit var contactResolver: ContactResolver
    private lateinit var eventFilter: EventFilter
    private lateinit var tcpClient: TcpClient

    private var currentIp: String = ""
    private var currentPort: Int = 0

    override fun onCreate() {
        super.onCreate()
        contactResolver = ContactResolver(applicationContext)
        eventFilter = EventFilter()
        refreshTcpClientIfNeeded()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        AppLogger.d("Listener conectado ✅")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        if (!isWhatsAppPackage(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim() ?: return
        val text = extras.getCharSequence("android.text")?.toString()?.trim()
        val subText = extras.getCharSequence("android.subText")?.toString()?.trim()

        AppLogger.d("Notificación de paquete: $packageName")

        if (isGeneralAppNotification(title)) {
            AppLogger.d("Ignorado: notificación general de app -> $title")
            return
        }

        if (isProbablyGroup(title, text)) {
            AppLogger.d("Ignorado: grupo detectado -> $title")
            return
        }

        if (isReactionNotification(text, subText)) {
            AppLogger.d("Ignorado: reacción -> text=$text | subText=$subText")
            return
        }

        val rawNumber = if (looksLikePhoneNumber(title)) {
            title
        } else {
            contactResolver.getPhoneNumber(title)
        }

        if (rawNumber.isNullOrEmpty()) {
            AppLogger.d("No se encontró número para: $title")
            return
        }

        val number = normalizeNumber(rawNumber)

        val blockedNumbers = setOf("60345413")
        if (number in blockedNumbers) {
            AppLogger.d("Ignorado: número bloqueado -> $number")
            return
        }

        val type = detectEventType(text, subText)

        if (type == EventType.OUTGOING_CALL) {
            AppLogger.d("Ignorado: llamada saliente -> $number")
            return
        }

        if (type == EventType.UNKNOWN) {
            AppLogger.d("Ignorado: tipo desconocido")
            return
        }

        val allowed = eventFilter.shouldSend(number, type, text)
        AppLogger.d("Filtro -> number=$number | type=$type | allowed=$allowed")

        if (!allowed) {
            AppLogger.d("Filtrado: $number | type=$type")
            return
        }

        AppLogger.d("WhatsApp detectado 📩")
        AppLogger.d("Número real: $number")
        AppLogger.d("Tipo: $type")
        AppLogger.d("Mensaje: $text")
        AppLogger.d("SubText: $subText")

        refreshTcpClientIfNeeded()
        tcpClient.send(number, type.name, System.currentTimeMillis())
    }

    private fun isWhatsAppPackage(packageName: String): Boolean {
        return packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b"
    }

    private fun isGeneralAppNotification(title: String): Boolean {
        return title.equals("WA Business", ignoreCase = true) ||
                title.equals("WhatsApp Business", ignoreCase = true) ||
                title.equals("WhatsApp", ignoreCase = true)
    }

    private fun looksLikePhoneNumber(value: String): Boolean {
        val cleaned = value
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        return cleaned.matches(Regex("^\\+?\\d{7,15}$"))
    }

    private fun detectEventType(text: String?, subText: String?): EventType {
        val t = (text ?: "").lowercase()
        val s = (subText ?: "").lowercase()
        val joined = "$t $s".trim()

        if (joined.isBlank()) return EventType.UNKNOWN

        if (
            joined.contains("llamando") ||
            joined.contains("calling") ||
            joined.contains("outgoing") ||
            joined.contains("videollamada saliente") ||
            joined.contains("llamada saliente")
        ) {
            return EventType.OUTGOING_CALL
        }

        if (
            joined.contains("llamada") ||
            joined.contains("videollamada") ||
            joined.contains("incoming voice call") ||
            joined.contains("incoming video call") ||
            joined.contains("llamada entrante")
        ) {
            return EventType.CALL
        }

        return EventType.MESSAGE
    }

    private fun isReactionNotification(text: String?, subText: String?): Boolean {
        val joined = "${text.orEmpty()} ${subText.orEmpty()}".lowercase()

        return joined.contains("reaccionó a tu mensaje") ||
                joined.contains("reaccionó con") ||
                joined.contains("reaccionó ") ||
                joined.contains("reacted to your message") ||
                joined.contains("reacted with") ||
                joined.contains("reaction to your message")
    }

    private fun isProbablyGroup(title: String, text: String?): Boolean {
        if (title.contains(":")) return true
        if (!text.isNullOrEmpty() && text.contains(":")) return true
        return false
    }

    private fun normalizeNumber(number: String): String {
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

    private fun refreshTcpClientIfNeeded() {
        val newIp = AppConfig.getServerIp(applicationContext)
        val newPort = AppConfig.getServerPort(applicationContext)

        if (!::tcpClient.isInitialized || newIp != currentIp || newPort != currentPort) {
            if (::tcpClient.isInitialized) {
                tcpClient.close()
            }

            currentIp = newIp
            currentPort = newPort
            tcpClient = TcpClient(currentIp, currentPort)

            AppLogger.d("TCP configurado a $currentIp:$currentPort")
        }
    }
}