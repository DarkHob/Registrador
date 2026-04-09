package com.JoseRosas.registrador.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "WA_BRIDGE"
    private const val MAX_LOGS = 200

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun d(msg: String) {
        Log.d(TAG, msg)
        addLog("D", msg)
    }

    fun e(msg: String, e: Exception? = null) {
        Log.e(TAG, msg, e)
        addLog("E", if (e != null) "$msg | ${e.message}" else msg)
    }

    private fun addLog(level: String, msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] $level | $msg"

        val updated = (_logs.value + line).takeLast(MAX_LOGS)
        _logs.value = updated
    }

    fun clear() {
        _logs.value = emptyList()
    }
}