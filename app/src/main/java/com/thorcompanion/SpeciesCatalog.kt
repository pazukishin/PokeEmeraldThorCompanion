package com.thorcompanion

import android.content.Context
import org.json.JSONObject

enum class Gender { MALE, FEMALE, GENDERLESS }

/**
 * Resolves a species id to a display name, national-dex number, sprite and other
 * per-species data, offline. The in-RAM species value is pokeemerald's internal id
 * (e.g. Treecko = 277), so it must be mapped to the national-dex number (252) first.
 */
object SpeciesCatalog {
    private var byId: Map<Int, String>? = null          // national dex -> lowercase name
    private var byName: Map<String, Int>? = null        // lowercase name -> national dex
    private var internalToNational: Map<Int, Int>? = null
    private var speciesDetails: Map<Int, JSONObject>? = null
    private var itemNames: Map<Int, String>? = null
    private var moveNames: Map<Int, String>? = null
    private var trainers: Map<Int, JSONObject>? = null
    private var abilityDescriptions: Map<String, String>? = null

    fun nameFor(context: Context, nationalDex: Int): String? {
        val map = byId ?: loadById(context).also { byId = it }
        return map[nationalDex]?.replaceFirstChar { c -> c.uppercase() }
    }

    fun spriteUrlFor(nationalDex: Int): String? = PokemonSprite.urlForId(nationalDex)

    fun nationalDexFor(context: Context, internalId: Int): Int {
        val map = internalToNational ?: loadInternalToNational(context).also { internalToNational = it }
        return map[internalId] ?: internalId
    }

    fun spriteIdForName(context: Context, name: String): Int? {
        val map = byName ?: loadByName(context).also { byName = it }
        return map[name.lowercase()]
    }

    fun genderFor(context: Context, nationalDex: Int, personality: Int): Gender {
        val ratio = detailsFor(context, nationalDex)?.optInt("genderRatio", 255) ?: 255
        return when {
            ratio >= 255 -> Gender.GENDERLESS
            ratio == 0 -> Gender.MALE
            ratio == 254 -> Gender.FEMALE
            (personality and 0xFF) < ratio -> Gender.FEMALE
            else -> Gender.MALE
        }
    }

    fun catchRateFor(context: Context, nationalDex: Int): Int =
        detailsFor(context, nationalDex)?.optInt("catchRate", 0) ?: 0

    fun abilityFor(context: Context, nationalDex: Int, abilityNum: Int): String? =
        detailsFor(context, nationalDex)
            ?.optJSONArray("abilities")
            ?.optString(abilityNum)
            ?.takeIf { it.isNotBlank() }

    fun itemNameFor(context: Context, itemId: Int): String? =
        (itemNames ?: loadItemNames(context).also { itemNames = it })[itemId]

    fun moveNameFor(context: Context, moveId: Int): String? =
        (moveNames ?: loadMoveNames(context).also { moveNames = it })[moveId]

    fun abilityDescriptionFor(context: Context, abilityName: String): String? =
        (abilityDescriptions ?: loadAbilityDescriptions(context).also { abilityDescriptions = it })[abilityName.lowercase()]

    fun trainerNameFor(context: Context, trainerId: Int): String? =
        (trainers ?: loadTrainers(context).also { trainers = it })[trainerId]
            ?.optString("name")?.takeIf { it.isNotBlank() }

    fun trainerSpriteUrlFor(context: Context, trainerId: Int): String? =
        (trainers ?: loadTrainers(context).also { trainers = it })[trainerId]
            ?.optString("spriteKey")?.takeIf { it.isNotBlank() }
            ?.let { "file:///android_asset/trainers/$it.png" }

    private fun detailsFor(context: Context, nationalDex: Int): JSONObject? =
        (speciesDetails ?: loadSpeciesDetails(context).also { speciesDetails = it })[nationalDex]

    private fun loadById(context: Context): Map<Int, String> = runCatching {
        val json = context.assets.open("emerald_species_ids.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val results = json.getJSONArray("results")
        buildMap {
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val id = item.getString("url").trimEnd('/').substringAfterLast('/').toIntOrNull() ?: continue
                put(id, item.getString("name"))
            }
        }
    }.getOrDefault(emptyMap())

    private fun loadByName(context: Context): Map<String, Int> =
        loadById(context).entries.associate { (id, name) -> name to id }

    private fun loadInternalToNational(context: Context): Map<Int, Int> = runCatching {
        val json = context.assets.open("species_to_national.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val map = mutableMapOf<Int, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.toInt()] = json.getInt(key)
        }
        map
    }.getOrDefault(emptyMap())

    private fun loadSpeciesDetails(context: Context): Map<Int, JSONObject> = runCatching {
        val json = context.assets.open("species_details.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val map = mutableMapOf<Int, JSONObject>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.toInt()] = json.getJSONObject(key)
        }
        map
    }.getOrDefault(emptyMap())

    private fun loadItemNames(context: Context): Map<Int, String> = runCatching {
        val json = context.assets.open("items.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val map = mutableMapOf<Int, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.toInt()] = json.getString(key)
        }
        map
    }.getOrDefault(emptyMap())

    private fun loadMoveNames(context: Context): Map<Int, String> = runCatching {
        val json = context.assets.open("moves.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val map = mutableMapOf<Int, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.toInt()] = json.getString(key)
        }
        map
    }.getOrDefault(emptyMap())

    private fun loadAbilityDescriptions(context: Context): Map<String, String> = runCatching {
        val json = context.assets.open("ability_descriptions.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val map = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.getString(key)
        }
        map
    }.getOrDefault(emptyMap())

    private fun loadTrainers(context: Context): Map<Int, JSONObject> = runCatching {
        val json = context.assets.open("trainers.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val map = mutableMapOf<Int, JSONObject>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.toInt()] = json.getJSONObject(key)
        }
        map
    }.getOrDefault(emptyMap())
}
