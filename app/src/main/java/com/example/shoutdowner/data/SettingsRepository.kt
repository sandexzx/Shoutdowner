package com.example.shoutdowner.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class SshSettings(
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val usePassword: Boolean = true,
    val password: String = "",
    val privateKeyPem: String = ""
)

data class WolSettings(
    val mac: String = "",
    val broadcast: String = "",
    val port: Int = 9
)

class SettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "shoutdowner_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USERNAME = "username"
        private const val KEY_USE_PASSWORD = "use_password"
        private const val KEY_PASSWORD = "password"
        private const val KEY_PRIVATE_KEY = "private_key"

        private const val KEY_WOL_MAC = "wol_mac"
        private const val KEY_WOL_BROADCAST = "wol_broadcast"
        private const val KEY_WOL_PORT = "wol_port"
    }

    fun getSettings(): SshSettings {
        val host = prefs.getString(KEY_HOST, "") ?: ""
        val port = prefs.getInt(KEY_PORT, 22)
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val usePassword = prefs.getBoolean(KEY_USE_PASSWORD, true)
        val password = prefs.getString(KEY_PASSWORD, "") ?: ""
        val privateKey = prefs.getString(KEY_PRIVATE_KEY, "") ?: ""
        return SshSettings(
            host = host,
            port = port,
            username = username,
            usePassword = usePassword,
            password = password,
            privateKeyPem = privateKey
        )
    }

    fun saveSettings(s: SshSettings) {
        prefs.edit()
            .putString(KEY_HOST, s.host)
            .putInt(KEY_PORT, s.port)
            .putString(KEY_USERNAME, s.username)
            .putBoolean(KEY_USE_PASSWORD, s.usePassword)
            .putString(KEY_PASSWORD, s.password)
            .putString(KEY_PRIVATE_KEY, s.privateKeyPem)
            .apply()
    }

    fun getWolSettings(): WolSettings {
        val mac = prefs.getString(KEY_WOL_MAC, "") ?: ""
        val broadcast = prefs.getString(KEY_WOL_BROADCAST, "") ?: ""
        val port = prefs.getInt(KEY_WOL_PORT, 9)
        return WolSettings(
            mac = mac,
            broadcast = broadcast,
            port = port
        )
    }

    fun saveWolSettings(w: WolSettings) {
        prefs.edit()
            .putString(KEY_WOL_MAC, w.mac)
            .putString(KEY_WOL_BROADCAST, w.broadcast)
            .putInt(KEY_WOL_PORT, w.port)
            .apply()
    }
}
