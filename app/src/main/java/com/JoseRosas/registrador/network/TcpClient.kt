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
            try {
                ensureConnected()

                val payload = """{"number":"$number","type":"$type","ts":$timestamp}"""
                writer?.write(payload)
                writer?.write("\n")
                writer?.flush()

                AppLogger.d("TCP enviado: $payload")
            } catch (e: Exception) {
                AppLogger.e("Error enviando TCP", e)
                close()
            }
        }
    }

    @Synchronized
    private fun ensureConnected() {
        if (socket?.isConnected == true && socket?.isClosed == false) return

        socket = Socket()
        socket?.connect(InetSocketAddress(host, port), 2000)
        writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))

        AppLogger.d("TCP conectado a $host:$port")
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