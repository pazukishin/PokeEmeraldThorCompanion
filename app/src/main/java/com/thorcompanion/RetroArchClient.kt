package com.thorcompanion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class MapReading(val mapId: String, val mapName: String)

data class EmeraldMap(val id: String, val name: String)

data class CoreMemoryProfile(
    val coreName: String,
    val mapAddress: Long,
    val mapLength: Int
)

object CoreProfiles {
    // Pokemon Emerald USA v1.0 used with RetroAchievements on V.GBA-Next.
    val vGbaNextEmerald = CoreMemoryProfile("V.GBA-Next", 0x020322E4, 2)
}

class RetroArchClient {
    private var socket: DatagramSocket? = null

    suspend fun readMap(host: String, port: Int, onLog: (String) -> Unit = {}): MapReading? = withContext(Dispatchers.IO) {
        try {
            val profile = CoreProfiles.vGbaNextEmerald
            onLog("Intentando leer memoria desde $host:$port")
            val activeSocket = DatagramSocket().also { socket = it }
            activeSocket.soTimeout = 450
            val commandText = "READ_CORE_MEMORY 0x${profile.mapAddress.toString(16)} ${profile.mapLength}"
            val command = commandText.toByteArray()
            onLog("Comando UDP: $commandText")
            activeSocket.send(DatagramPacket(command, command.size, InetAddress.getByName(host), port))
            val response = ByteArray(256)
            val packet = DatagramPacket(response, response.size)
            onLog("Esperando respuesta UDP…")
            activeSocket.receive(packet)
            val raw = packet.data.decodeToString(0, packet.length).trim()
            onLog("Respuesta cruda: '$raw'")
            parseMap(raw)
        } catch (e: Exception) {
            onLog("Error con RetroArch: ${e.javaClass.simpleName} - ${e.message ?: "sin detalle"}")
            null
        } finally {
            socket?.close()
            socket = null
            onLog("Socket UDP cerrado")
        }
    }

    internal fun parseMap(raw: String): MapReading? {
        val bytes = raw.trim()
            .split(Regex("\\s+"))
            .mapNotNull { token -> token.toIntOrNull(16)?.takeIf { token.matches(Regex("(?i)[0-9a-f]{2}")) } }
            .takeLast(2)
        if (bytes.size != 2) return null

        val mapGroup = bytes[0]
        val mapNumber = bytes[1]
        val map = EmeraldMapCatalog.resolveMapId(mapGroup, mapNumber)
        return MapReading(map.id, map.name)
    }

    fun close() {
        socket?.close()
        socket = null
    }
}