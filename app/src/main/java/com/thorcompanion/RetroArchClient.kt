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
        val profile = CoreProfiles.vGbaNextEmerald
        onLog("Intentando leer memoria desde $host:$port")
        // The save block is ASLR-shifted: dereference gSaveBlock1Ptr to find the location.
        val bytes = readBytes(host, port, resolveMapAddress(host, port, onLog), profile.mapLength, onLog)
        bytes?.let { parseMapBytes(it) }
    }

    /** Resolves the current map address by dereferencing gSaveBlock1Ptr (falls back to the fixed address). */
    private suspend fun resolveMapAddress(host: String, port: Int, onLog: (String) -> Unit): Long {
        val ptrBytes = readBytes(host, port, EmeraldAddresses.SAVE_BLOCK1_PTR, 4, onLog)
        if (ptrBytes != null && ptrBytes.size >= 4) {
            val ptr = (ptrBytes[0].toLong() and 0xFF) or ((ptrBytes[1].toLong() and 0xFF) shl 8) or
                ((ptrBytes[2].toLong() and 0xFF) shl 16) or ((ptrBytes[3].toLong() and 0xFF) shl 24)
            if (ptr in 0x02000000L..0x02FFFFFFL) {
                onLog("SaveBlock1 ptr: 0x${ptr.toString(16)}")
                return ptr + EmeraldAddresses.SAVEBLOCK1_LOCATION
            }
        }
        onLog("No se pudo leer gSaveBlock1Ptr; usando dirección fija")
        return CoreProfiles.vGbaNextEmerald.mapAddress
    }

    /**
     * Reads [length] bytes from the core's memory at [address] using READ_CORE_MEMORY.
     * Returns the raw bytes (little-endian memory order) or null on failure.
     */
    suspend fun readBytes(host: String, port: Int, address: Long, length: Int, onLog: (String) -> Unit = {}): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val activeSocket = DatagramSocket().also { socket = it }
                activeSocket.soTimeout = 750
                val commandText = "READ_CORE_MEMORY 0x${address.toString(16)} $length"
                val command = commandText.toByteArray()
                onLog("Comando UDP: $commandText")
                activeSocket.send(DatagramPacket(command, command.size, InetAddress.getByName(host), port))
                val response = ByteArray((length * 3 + 96).coerceAtLeast(256))
                val packet = DatagramPacket(response, response.size)
                onLog("Esperando respuesta UDP…")
                activeSocket.receive(packet)
                val raw = packet.data.decodeToString(0, packet.length).trim()
                onLog("Respuesta cruda (${raw.length} chars)")
                parseHexBytes(raw)
            } catch (e: Exception) {
                onLog("Error con RetroArch: ${e.javaClass.simpleName} - ${e.message ?: "sin detalle"}")
                null
            } finally {
                socket?.close()
                socket = null
                onLog("Socket UDP cerrado")
            }
        }

    /** Parses the hex byte tokens of a READ_CORE_MEMORY response, in order. */
    internal fun parseHexBytes(raw: String): ByteArray =
        raw.trim()
            .split(Regex("\\s+"))
            .mapNotNull { token -> token.toIntOrNull(16)?.takeIf { it in 0..255 }?.toByte() }
            .toByteArray()

    internal fun parseMap(raw: String): MapReading? = parseMapBytes(parseHexBytes(raw))

    private fun parseMapBytes(bytes: ByteArray): MapReading? {
        if (bytes.size < 2) return null
        val mapGroup = bytes[0].toInt() and 0xFF
        val mapNumber = bytes[1].toInt() and 0xFF
        val map = EmeraldMapCatalog.resolveMapId(mapGroup, mapNumber)
        return MapReading(map.id, map.name)
    }

    fun close() {
        socket?.close()
        socket = null
    }
}
