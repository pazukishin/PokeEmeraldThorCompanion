package com.thorcompanion

/**
 * Pure-GBA decoders for the Pokémon data structures used by Pokemon Emerald (pret/pokeemerald).
 *
 * Layouts mirror include/pokemon.h:
 *  - struct Pokemon   (100 bytes): personality @0x00, otId @0x04, secure substructs @0x20..0x4F,
 *    status @0x50, level @0x54, stats @0x56..0x63.
 *  - struct BattlePokemon (88 bytes): species @0x00, stats @0x02..0x0A, moves @0x0C,
 *    IVs @0x14, ability @0x20, level @0x2A, friendship @0x2B, maxHP @0x2C, personality @0x48.
 *
 * Encryption (src/pokemon.c EncryptBoxMon/DecryptBoxMon): each 32-bit word of the 48-byte
 * "secure" section is XORed with (personality ^ otId). The four 12-byte substructs are then
 * re-ordered according to `personality % 24` (GetSubstruct).
 */
object PokemonDecoder {
    /** substruct order table: order[personality % 24][substructType] = slot index (0..3). */
    private val substructOrder = arrayOf(
        intArrayOf(0, 1, 2, 3), intArrayOf(0, 1, 3, 2), intArrayOf(0, 2, 1, 3), intArrayOf(0, 3, 1, 2),
        intArrayOf(0, 2, 3, 1), intArrayOf(0, 3, 2, 1), intArrayOf(1, 0, 2, 3), intArrayOf(1, 0, 3, 2),
        intArrayOf(2, 0, 1, 3), intArrayOf(3, 0, 1, 2), intArrayOf(2, 0, 3, 1), intArrayOf(3, 0, 2, 1),
        intArrayOf(1, 2, 0, 3), intArrayOf(1, 3, 0, 2), intArrayOf(2, 1, 0, 3), intArrayOf(3, 1, 0, 2),
        intArrayOf(2, 3, 0, 1), intArrayOf(3, 2, 0, 1), intArrayOf(1, 2, 3, 0), intArrayOf(1, 3, 2, 0),
        intArrayOf(2, 1, 3, 0), intArrayOf(3, 1, 2, 0), intArrayOf(2, 3, 1, 0), intArrayOf(3, 2, 1, 0)
    )

    /** Spanish nature names, indexed by nature id (personality % 25). */
    val natureNames = listOf(
        "Fuerte", "Huraña", "Audaz", "Firme", "Pícara",
        "Osada", "Dócil", "Plácida", "Agitada", "Floja",
        "Miedosa", "Activa", "Seria", "Alegre", "Ingenua",
        "Modesta", "Afable", "Mansa", "Tímida", "Alocada",
        "Serena", "Amable", "Grosera", "Cauta", "Rara"
    )

    /** gNatureStatTable (src/pokemon.c): rows are nature id, columns are [Atk, Def, Spe, SpA, SpD]. */
    private val natureStatTable = arrayOf(
        intArrayOf(0, 0, 0, 0, 0), intArrayOf(1, -1, 0, 0, 0), intArrayOf(1, 0, -1, 0, 0), intArrayOf(1, 0, 0, -1, 0),
        intArrayOf(1, 0, 0, 0, -1), intArrayOf(-1, 1, 0, 0, 0), intArrayOf(0, 0, 0, 0, 0), intArrayOf(0, 1, -1, 0, 0),
        intArrayOf(0, 1, 0, -1, 0), intArrayOf(0, 1, 0, 0, -1), intArrayOf(-1, 0, 1, 0, 0), intArrayOf(0, -1, 1, 0, 0),
        intArrayOf(0, 0, 0, 0, 0), intArrayOf(0, 0, 1, -1, 0), intArrayOf(0, 0, 1, 0, -1), intArrayOf(-1, 0, 0, 1, 0),
        intArrayOf(0, -1, 0, 1, 0), intArrayOf(0, 0, -1, 1, 0), intArrayOf(0, 0, 0, 0, 0), intArrayOf(0, 0, 0, 1, -1),
        intArrayOf(-1, 0, 0, 0, 1), intArrayOf(0, -1, 0, 0, 1), intArrayOf(0, 0, -1, 0, 1), intArrayOf(0, 0, 0, -1, 1),
        intArrayOf(0, 0, 0, 0, 0)
    )

    private val hiddenPowerTypes = listOf(
        "Lucha", "Volador", "Veneno", "Tierra", "Roca", "Bicho", "Fantasma", "Acero",
        "Fuego", "Agua", "Planta", "Eléctrico", "Psíquico", "Hielo", "Dragón", "Siniestro"
    )

    data class IvSpread(
        val hp: Int, val attack: Int, val defense: Int,
        val speed: Int, val spAttack: Int, val spDefense: Int
    ) {
        val hiddenPowerType: String
            get() {
                val value = ((hp and 1) + (attack and 1) * 2 + (defense and 1) * 4 +
                        (speed and 1) * 8 + (spAttack and 1) * 16 + (spDefense and 1) * 32) * 15 / 63
                return hiddenPowerTypes[value.coerceIn(0, 15)]
            }
    }

    data class DecodedPokemon(
        val speciesId: Int,
        val level: Int,
        val personality: Int,
        val status: Int,
        val nature: Int,
        val ivs: IvSpread,
        val evs: IvSpread?,
        val friendship: Int?,
        val moves: List<Int>,
        val heldItem: Int,
        val abilityNum: Int,
        val isEgg: Boolean,
        val currentHp: Int?,
        val maxHp: Int?,
        val attack: Int?,
        val defense: Int?,
        val speed: Int?,
        val spAttack: Int?,
        val spDefense: Int?
    )

    /** Decode a 100-byte `struct Pokemon` (party / enemy party). */
    fun decodePokemon(data: ByteArray): DecodedPokemon? {
        if (data.size < 100) return null
        val personality = readU32(data, 0)
        val otId = readU32(data, 4)
        val level = data[0x54].toInt() and 0xFF

        val secure = ByteArray(48)
        System.arraycopy(data, 0x20, secure, 0, 48)
        decrypt(secure, personality, otId)

        val order = substructOrder[personality.unsignedMod(24)]
        val type0 = secure.copyOfRange(order[0] * 12, order[0] * 12 + 12)
        val type1 = secure.copyOfRange(order[1] * 12, order[1] * 12 + 12)
        val type2 = secure.copyOfRange(order[2] * 12, order[2] * 12 + 12)
        val type3 = secure.copyOfRange(order[3] * 12, order[3] * 12 + 12)

        val species = readU16(type0, 0)
        val heldItem = readU16(type0, 2)
        val friendship = type0[9].toInt() and 0xFF
        val moves = listOf(readU16(type1, 0), readU16(type1, 2), readU16(type1, 4), readU16(type1, 6))
        val evs = IvSpread(
            type2[0].toInt() and 0xFF, type2[1].toInt() and 0xFF, type2[2].toInt() and 0xFF,
            type2[3].toInt() and 0xFF, type2[4].toInt() and 0xFF, type2[5].toInt() and 0xFF
        )
        val packedIvs = readU32(type3, 4)
        val ivs = unpackIvs(packedIvs)
        val abilityNum = (packedIvs ushr 31) and 1

        return DecodedPokemon(
            speciesId = species,
            level = level,
            personality = personality,
            status = readU32(data, 0x50),
            nature = personality.unsignedMod(25),
            ivs = ivs,
            evs = evs,
            friendship = friendship,
            moves = moves,
            heldItem = heldItem,
            abilityNum = abilityNum,
            isEgg = ((packedIvs ushr 30) and 1) == 1,
            currentHp = readU16(data, 0x56),
            maxHp = readU16(data, 0x58),
            attack = readU16(data, 0x5A),
            defense = readU16(data, 0x5C),
            speed = readU16(data, 0x5E),
            spAttack = readU16(data, 0x60),
            spDefense = readU16(data, 0x62)
        )
    }

    /** Decode an 88-byte `struct BattlePokemon` (current battle battler). EVs are not stored here. */
    fun decodeBattlePokemon(data: ByteArray): DecodedPokemon? {
        if (data.size < 0x58) return null
        val personality = readU32(data, 0x48)
        val packedIvs = readU32(data, 0x14)
        val ivs = unpackIvs(packedIvs)
        return DecodedPokemon(
            speciesId = readU16(data, 0x00),
            level = data[0x2A].toInt() and 0xFF,
            personality = personality,
            status = readU32(data, 0x4C),
            nature = personality.unsignedMod(25),
            ivs = ivs,
            evs = null,
            friendship = data[0x2B].toInt() and 0xFF,
            moves = listOf(readU16(data, 0x0C), readU16(data, 0x0E), readU16(data, 0x10), readU16(data, 0x12)),
            heldItem = readU16(data, 0x2E),
            abilityNum = (packedIvs ushr 31) and 1,
            isEgg = ((packedIvs ushr 30) and 1) == 1,
            currentHp = readU16(data, 0x28),
            maxHp = readU16(data, 0x2C),
            attack = readU16(data, 0x02),
            defense = readU16(data, 0x04),
            speed = readU16(data, 0x06),
            spAttack = readU16(data, 0x08),
            spDefense = readU16(data, 0x0A)
        )
    }

    /** Ability index stored directly in struct BattlePokemon.ability (offset 0x20). */
    fun battleAbilityId(data: ByteArray): Int? {
        if (data.size < 0x21) return null
        val ability = data[0x20].toInt() and 0xFF
        return if (ability == 0) null else ability
    }

    fun natureName(nature: Int): String = natureNames[nature.coerceIn(0, 24)]

    /** Human-readable stat modifier, e.g. "+Atq −Def", or "" for neutral natures. */
    fun natureModifier(nature: Int): String {
        val table = natureStatTable[nature.coerceIn(0, 24)]
        val labels = listOf("Atq", "Def", "Vel", "AtEsp", "DefEsp")
        val boosted = mutableListOf<String>()
        val hindered = mutableListOf<String>()
        for (i in table.indices) {
            when {
                table[i] > 0 -> boosted += labels[i]
                table[i] < 0 -> hindered += labels[i]
            }
        }
        return buildString {
            if (boosted.isNotEmpty()) append("+").append(boosted.joinToString("/"))
            if (boosted.isNotEmpty() && hindered.isNotEmpty()) append(" ")
            if (hindered.isNotEmpty()) append("−").append(hindered.joinToString("/"))
        }
    }

    private fun unpackIvs(packed: Int): IvSpread = IvSpread(
        packed and 0x1F,
        (packed ushr 5) and 0x1F,
        (packed ushr 10) and 0x1F,
        (packed ushr 15) and 0x1F,
        (packed ushr 20) and 0x1F,
        (packed ushr 25) and 0x1F
    )

    private fun decrypt(secure: ByteArray, personality: Int, otId: Int) {
        val key = personality xor otId
        for (i in 0 until 12) {
            val off = i * 4
            val word = readU32(secure, off) xor key
            writeU32(secure, off, word)
        }
    }

    /** Modulo on the unsigned 32-bit value (personality is a random u32, often with bit 31 set). */
    private fun Int.unsignedMod(divisor: Int): Int = ((toLong() and 0xFFFFFFFFL) % divisor).toInt()

    private fun readU16(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)

    private fun readU32(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)

    private fun writeU32(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        data[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}
