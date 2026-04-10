package com.JoseRosas.registrador.network

import android.content.Context

object AppConfig {
    private const val PREFS_NAME = "registrador_prefs"
    private const val KEY_SERVER_IP = "server_ip"
    private const val KEY_SERVER_PORT = "server_port"

    private const val DEFAULT_IP = "192.168.31.5"
    private const val DEFAULT_PORT = 9000

    fun getServerIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    fun setServerIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_IP, ip).apply()
    }

    fun getServerPort(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
    }

    fun setServerPort(context: Context, port: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SERVER_PORT, port).apply()
    }
}