package com.JoseRosas.registrador.network

import com.JoseRosas.registrador.util.AppLogger
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class TcpClient(
    private val host: String,
    private val port: Int
) {
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun send(number: String, type: String, timestamp: Long) {
        executor.execute {
            val payload = """{"number":"$number","type":"$type","ts":$timestamp}"""

            try {
                reconnectIfNeeded()

                writer?.write(payload)
                writer?.write("\n")
                writer?.flush()

                AppLogger.d("TCP enviado: $payload")
            } catch (e: Exception) {
                AppLogger.e("Error enviando TCP", e)
                close()

                try {
                    AppLogger.d("Reintentando TCP...")
                    reconnectIfNeeded()
                    writer?.write(payload)
                    writer?.write("\n")
                    writer?.flush()
                    AppLogger.d("TCP reenviado OK: $payload")
                } catch (e2: Exception) {
                    AppLogger.e("Fallo reintento TCP", e2)
                    close()
                }
            }
        }
    }

    @Synchronized
    private fun reconnectIfNeeded() {
        if (isSocketUsable()) return

        close()

        socket = Socket()
        socket?.keepAlive = true
        socket?.tcpNoDelay = true
        socket?.connect(InetSocketAddress(host, port), 2000)

        writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
        AppLogger.d("TCP conectado a $host:$port")
    }

    @Synchronized
    private fun isSocketUsable(): Boolean {
        val s = socket ?: return false
        if (s.isClosed) return false
        if (!s.isConnected) return false
        if (s.isOutputShutdown) return false
        return true
    }

    @Synchronized
    fun close() {
        try {
            writer?.close()
        } catch (_: Exception) {
        }

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        writer = null
        socket = null
    }
}