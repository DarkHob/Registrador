package com.JoseRosas.registrador.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.JoseRosas.registrador.contacts.ContactResolver
import com.JoseRosas.registrador.filters.EventFilter
import com.JoseRosas.registrador.model.EventType
import com.JoseRosas.registrador.util.AppLogger
import com.JoseRosas.registrador.network.TcpClient
import com.JoseRosas.registrador.network.AppConfig
class WhatsAppNotificationListener : NotificationListenerService() {

    private lateinit var contactResolver: ContactResolver
    private lateinit var eventFilter: EventFilter
    private lateinit var tcpClient: TcpClient
    override fun onCreate() {
        super.onCreate()
        contactResolver = ContactResolver(applicationContext)
        eventFilter = EventFilter()

        val ip = AppConfig.getServerIp(applicationContext)
        val port = AppConfig.getServerPort(applicationContext)
        tcpClient = TcpClient(ip, port)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        AppLogger.d("Listener conectado ✅")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim() ?: return
        val text = extras.getCharSequence("android.text")?.toString()?.trim()

        AppLogger.d("Notificación de paquete: $packageName")

        if (title.contains(":")) {
            AppLogger.d("Ignorado: grupo detectado -> $title")
            return
        }

        val number = contactResolver.getPhoneNumber(title)

        if (number.isNullOrEmpty()) {
            AppLogger.d("No se encontró número para: $title")
            return
        }

        val type = detectEventType(text)

        if (type == EventType.UNKNOWN) {
            AppLogger.d("Ignorado: tipo desconocido")
            return
        }

        val allowed = eventFilter.shouldSend(number, type)
        AppLogger.d("Filtro -> number=$number | type=$type | allowed=$allowed")

        if (!allowed) {
            AppLogger.d("Filtrado: $number | type=$type")
            return
        }

        AppLogger.d("WhatsApp detectado 📩")
        AppLogger.d("Número real: $number")
        AppLogger.d("Tipo: $type")
        AppLogger.d("Mensaje: $text")

        tcpClient.send(number, type.name, System.currentTimeMillis())
    }

    private fun isWhatsAppPackage(packageName: String): Boolean {
        return packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b"
    }

    private fun detectEventType(text: String?): EventType {
        if (text.isNullOrBlank()) return EventType.UNKNOWN

        val lower = text.lowercase()

        return when {
            "llamada" in lower -> EventType.CALL
            "videollamada" in lower -> EventType.CALL
            else -> EventType.MESSAGE
        }
    }

    private fun isProbablyGroup(title: String, text: String?): Boolean {
        if (title.contains(":")) return true
        if (text != null && text.contains(":")) return true
        return false
    }




}