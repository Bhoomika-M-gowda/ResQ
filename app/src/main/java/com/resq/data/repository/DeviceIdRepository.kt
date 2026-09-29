package com.resq.data.repository

import android.content.Context
import java.util.UUID

class DeviceIdRepository(context: Context) {
    private val preferences = context.getSharedPreferences("resq_device", Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = "DEVICE-${UUID.randomUUID().toString().take(4).uppercase()}"
        preferences.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    companion object { private const val KEY_DEVICE_ID = "device_id" }
}
