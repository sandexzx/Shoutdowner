package com.example.shoutdowner.wol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WOLManager {
    private const val TAG = "WOLManager"

    /**
     * Отправить Magic Packet (Wake-on-LAN).
     * @param mac MAC-адрес в формате AA:BB:CC:DD:EE:FF или AABBCCDDEEFF
     * @param broadcastAddr Broadcast адрес, например "192.168.8.255". Если null или пустой — используется "255.255.255.255"
     * @param port UDP порт (обычно 7 или 9). По умолчанию 9.
     * @return true если пакет успешно отправлен, false при ошибке.
     */
    suspend fun sendWake(mac: String, broadcastAddr: String? = null, port: Int = 9): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val macBytes = parseMac(mac) ?: run {
                    Log.e(TAG, "Invalid MAC: $mac")
                    return@withContext false
                }

                // magic packet: 6 x 0xFF + 16 x MAC
                val packet = ByteArray(6 + 16 * macBytes.size)
                for (i in 0 until 6) {
                    packet[i] = 0xFF.toByte()
                }
                var idx = 6
                for (i in 0 until 16) {
                    System.arraycopy(macBytes, 0, packet, idx, macBytes.size)
                    idx += macBytes.size
                }

                val addr = if (broadcastAddr.isNullOrBlank()) "255.255.255.255" else broadcastAddr
                val inetAddress = InetAddress.getByName(addr)

                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val datagram = DatagramPacket(packet, packet.size, inetAddress, port)
                    socket.send(datagram)
                }

                Log.d(TAG, "Magic packet sent to $mac via $addr:$port")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send WOL packet", e)
                false
            }
        }
    }

    private fun parseMac(mac: String): ByteArray? {
        val cleaned = mac.replace("[:\\-]".toRegex(), "").trim()
        if (cleaned.length != 12) return null
        return try {
            ByteArray(6) { i ->
                cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (e: Exception) {
            null
        }
    }
}
