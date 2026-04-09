package com.JoseRosas.registrador.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.JoseRosas.registrador.util.AppLogger

class WhatsAppNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        AppLogger.d("Listener conectado ✅")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        AppLogger.d("Notificación de paquete: $packageName")

        if (packageName != "com.whatsapp") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()

        AppLogger.d("WhatsApp detectado 📩")
        AppLogger.d("Contacto: $title")
        AppLogger.d("Mensaje: $text")
    }
}