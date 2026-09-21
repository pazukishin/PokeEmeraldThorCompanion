package com.thorcompanion

object PokemonSprite {
    private val localIds = mapOf(
        "poochyena" to 261,
        "zigzagoon" to 263,
        "wurmple" to 265,
        "ralts" to 280,
        "seedot" to 273
    )

    fun urlFor(encounter: Encounter): String? {
        val id = encounter.spriteId ?: localIds[encounter.name.lowercase()]
        return urlForId(id)
    }

    fun urlForName(name: String): String? = urlForId(localIds[name.lowercase()])

    fun urlForId(id: Int?): String? = id?.let { "file:///android_asset/pokemon/$it.png" }
}