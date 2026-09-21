package com.thorcompanion

object ItemSprite {
    fun urlFor(item: MapItem): String {
        val slug = item.spriteKey.lowercase()
            .replace(". ", "-")
            .replace(" ", "-")
            .replace(".", "")
        return "file:///android_asset/items/$slug.png"
    }
}