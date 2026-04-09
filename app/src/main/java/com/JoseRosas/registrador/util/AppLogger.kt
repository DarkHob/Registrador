package com.JoseRosas.registrador.util

import android.util.Log

object AppLogger {
    private const val TAG = "WA_BRIDGE"

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun e(msg: String, e: Exception? = null) {
        Log.e(TAG, msg, e)
    }
}