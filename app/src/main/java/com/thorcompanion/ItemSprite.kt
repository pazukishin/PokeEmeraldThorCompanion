package com.thorcompanion

object ItemSprite {
    fun urlFor(item: MapItem): String {
        val slug = item.spriteKey.lowercase()
            .replace(". ", "-")
            .replace(" ", "-")
            .replace(".", "")
        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/$slug.png"
    }
}