package com.twotv.tv.util

import android.content.Context
import android.content.SharedPreferences
import com.twotv.tv.server.DevicePairInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PairingManager {
    private const val PREF_NAME = "2tv_pairing_prefs"
    private const val KEY_PAIRED_DEVICES = "paired_devices_json"
    private val json = Json { ignoreUnknownKeys = true }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getPairedDevices(context: Context): MutableList<DevicePairInfo> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_PAIRED_DEVICES, null) ?: return mutableListOf()
        return try {
            json.decodeFromString<List<DevicePairInfo>>(jsonStr).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveDevice(context: Context, device: DevicePairInfo) {
        val list = getPairedDevices(context)
        val existingIndex = list.indexOfFirst { it.deviceIp == device.deviceIp }
        if (existingIndex != -1) {
            list[existingIndex] = device
        } else {
            list.add(0, device)
        }
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_PAIRED_DEVICES, json.encodeToString(list)).apply()
    }

    fun removeDevice(context: Context, index: Int): List<DevicePairInfo> {
        val list = getPairedDevices(context)
        if (index in list.indices) {
            list.removeAt(index)
            val prefs = getPrefs(context)
            prefs.edit().putString(KEY_PAIRED_DEVICES, json.encodeToString(list)).apply()
        }
        return list
    }
}
