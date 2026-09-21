package com.thorcompanion

import org.junit.Assert.assertEquals
import org.junit.Test

class PokemonDecoderTest {
    /** Builds a 100-byte struct Pokemon with personality/otId and substructs in natural (decrypted) order. */
    private fun buildMon(personality: Int, otId: Int): ByteArray {
        val data = ByteArray(100)
        writeU32(data, 0, personality)
        writeU32(data, 4, otId)
        // type0 @0x20: species=1 (Bulbasaur), friendship=70 @0x29
        writeU16(data, 0x20, 1)
        data[0x29] = 70
        // type2 @0x38: EVs 10..60
        val evs = intArrayOf(10, 20, 30, 40, 50, 60)
        for (i in evs.indices) data[0x38 + i] = evs[i].toByte()
        // type3 @0x44: packed IVs u32 @0x48
        val ivs = intArrayOf(31, 30, 29, 28, 27, 26)
        val packed = ivs[0] or (ivs[1] shl 5) or (ivs[2] shl 10) or (ivs[3] shl 15) or (ivs[4] shl 20) or (ivs[5] shl 25)
        writeU32(data, 0x48, packed)
        data[0x54] = 5 // level
        return data
    }

    @Test
    fun decodePokemon_identityOrder_extractsFields() {
        val mon = PokemonDecoder.decodePokemon(buildMon(personality = 0, otId = 0))!!

        assertEquals(1, mon.speciesId)
        assertEquals(5, mon.level)
        assertEquals(70, mon.friendship)
        assertEquals(31, mon.ivs.hp)
        assertEquals(30, mon.ivs.attack)
        assertEquals(26, mon.ivs.spDefense)
        assertEquals(10, mon.evs?.hp)
        assertEquals(60, mon.evs?.spDefense)
        assertEquals(0, mon.nature) // Hardy
    }

    @Test
    fun decodePokemon_decryptsNonZeroKey() {
        val key = 0x12345678
        val data = buildMon(personality = 0, otId = key) // key = personality ^ otId = key
        // XOR each u32 of the secure section to simulate encryption
        for (i in 0 until 12) {
            val off = 0x20 + i * 4
            val word = readU32(data, off) xor key
            writeU32(data, off, word)
        }
        val mon = PokemonDecoder.decodePokemon(data)!!
        assertEquals(1, mon.speciesId)
        assertEquals(31, mon.ivs.hp)
    }

    @Test
    fun decodePokemon_reordersSubstructs() {
        // personality % 24 == 1 -> order [0,1,3,2]: slot0=type0, slot1=type1, slot2=type3, slot3=type2
        val personality = 1
        val data = ByteArray(100)
        writeU32(data, 0, personality)
        writeU32(data, 4, personality) // otId == personality so the XOR key is 0 (plaintext)
        writeU16(data, 0x20 + 0, 1)      // type0 at slot 0 -> species @0x20
        data[0x20 + 9] = 70              // type0 friendship
        writeU32(data, 0x20 + 24 + 4, 31) // type3 at slot 2 -> IVs u32 @ (24 + 4)
        data[0x54] = 7                   // level
        val mon = PokemonDecoder.decodePokemon(data)!!
        assertEquals(1, mon.speciesId)
        assertEquals(70, mon.friendship)
        assertEquals(31, mon.ivs.hp)
        assertEquals(7, mon.level)
    }

    @Test
    fun natureModifier_adamant_boostsAttackHindersSpAtk() {
        assertEquals("+Atq −AtEsp", PokemonDecoder.natureModifier(3))
    }

    @Test
    fun hiddenPowerType_all31IVs_isDark() {
        val ivs = PokemonDecoder.IvSpread(31, 31, 31, 31, 31, 31)
        assertEquals("Siniestro", ivs.hiddenPowerType)
    }

    @Test
    fun decodePokemon_unsignedPersonalityModulo() {
        // personality with bit 31 set: as unsigned u32, 0xFFFFFFFB % 24 = 11 (order [3,0,2,1]).
        val personality = 0xFFFFFFFB.toInt() // -5 as a signed Int
        val data = ByteArray(100)
        writeU32(data, 0, personality)
        writeU32(data, 4, personality) // otId == personality -> XOR key 0 (plaintext)
        writeU16(data, 0x20 + 36, 1)      // type0 species at slot 3 (secure offset 36)
        data[0x20 + 36 + 9] = 70          // type0 friendship
        writeU32(data, 0x20 + 12 + 4, 31) // type3 IVs at slot 1 (secure offset 12 + 4)
        data[0x54] = 7                    // level

        val mon = PokemonDecoder.decodePokemon(data)!!

        assertEquals(1, mon.speciesId)
        assertEquals(70, mon.friendship)
        assertEquals(31, mon.ivs.hp)
        assertEquals(7, mon.level)
        assertEquals(16, mon.nature) // 0xFFFFFFFB (unsigned) % 25 = 16
    }

    private fun readU32(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or ((data[offset + 3].toInt() and 0xFF) shl 24)

    private fun writeU32(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        data[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }
}
