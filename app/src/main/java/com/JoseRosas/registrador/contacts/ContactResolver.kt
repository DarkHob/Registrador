package com.JoseRosas.registrador.contacts

import android.content.Context
import android.provider.ContactsContract

class ContactResolver(private val context: Context) {

    fun getPhoneNumber(name: String): String? {

        val resolver = context.contentResolver

        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?",
            arrayOf(name),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {

                val number = it.getString(
                    it.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                )

                return cleanNumber(number)
            }
        }

        return null
    }

    private fun cleanNumber(number: String): String {
        var n = number
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
}