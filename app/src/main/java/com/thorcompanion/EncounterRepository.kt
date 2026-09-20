package com.thorcompanion

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class EncounterRepository(private val context: Context) {
    private var offlineCatalog: JSONObject? = null
    private var speciesCatalog: JSONObject? = null
    private var mapContentCatalog: JSONObject? = null

    fun forMap(mapId: String): List<Encounter> = EncounterCatalog.forMap(mapId)

    suspend fun loadPokemonDetails(encounter: Encounter): PokemonDetails? = withContext(Dispatchers.IO) {
        runCatching {
            val slug = encounter.name.lowercase().replace(' ', '-')
            val root = getJson("https://pokeapi.co/api/v2/pokemon/$slug/")
            val species = getJson(root.getJSONObject("species").getString("url"))
            val types = root.getJSONArray("types").let { array -> (0 until array.length()).map { array.getJSONObject(it).getJSONObject("type").getString("name").replace('-', ' ').replaceFirstChar { c -> c.uppercase() } } }
            val abilities = root.getJSONArray("abilities").let { array -> (0 until array.length()).map { array.getJSONObject(it).getJSONObject("ability").getString("name").replace('-', ' ').replaceFirstChar { c -> c.uppercase() } } }
            val description = species.getJSONArray("flavor_text_entries").let { array ->
                (0 until array.length()).firstOrNull { array.getJSONObject(it).getJSONObject("language").getString("name") == "en" }
                    ?.let { array.getJSONObject(it).getString("flavor_text").replace('\n', ' ').replace('\u000c', ' ') } ?: ""
            }
            val chain = getJson(species.getJSONObject("evolution_chain").getString("url"))
            val evolutions = buildList {
                var node: JSONObject? = chain.getJSONObject("chain")
                while (node != null) {
                    add(node.getJSONObject("species").getString("name").replace('-', ' ').replaceFirstChar { c -> c.uppercase() })
                    node = node.optJSONArray("evolves_to")?.optJSONObject(0)
                }
            }
            PokemonDetails(encounter.name, types, abilities, evolutions, description, root.getJSONObject("sprites").optString("front_default").ifBlank { null }, root.getJSONObject("sprites").optString("front_shiny").ifBlank { null })
        }.getOrNull()
    }

    suspend fun loadItemDetails(item: MapItem): ItemDetails? = withContext(Dispatchers.IO) {
        runCatching {
            val root = getJson("https://pokeapi.co/api/v2/item/${item.spriteKey}/")
            val effectEntries = root.getJSONArray("effect_entries")
            val effect = (0 until effectEntries.length()).firstOrNull { effectEntries.getJSONObject(it).getJSONObject("language").getString("name") == "en" }?.let { effectEntries.getJSONObject(it).getString("effect") } ?: ""
            val descriptions = root.getJSONArray("flavor_text_entries")
            val description = (0 until descriptions.length()).firstOrNull { descriptions.getJSONObject(it).getJSONObject("language").getString("name") == "en" }?.let { descriptions.getJSONObject(it).getString("text").replace('\n', ' ') } ?: ""
            ItemDetails(item.name, description, effect, root.getJSONObject("sprites").optString("default").ifBlank { null })
        }.getOrNull()
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 2500
        connection.readTimeout = 2500
        return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
    }

    suspend fun refreshFromWeb(mapId: String): List<Encounter> = withContext(Dispatchers.IO) {
        runCatching {
            val slug = apiAreaSlug(mapId)
            val connection = URL("https://pokeapi.co/api/v2/location-area/$slug/").openConnection() as HttpURLConnection
            connection.connectTimeout = 2500
            connection.readTimeout = 2500
            connection.requestMethod = "GET"
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching emptyList()
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val encounters = root.getJSONArray("pokemon_encounters")
            buildList {
                for (index in 0 until encounters.length()) {
                    val item = encounters.getJSONObject(index)
                    val versions = item.getJSONArray("version_details")
                    for (versionIndex in 0 until versions.length()) {
                        val version = versions.getJSONObject(versionIndex)
                        if (version.getJSONObject("version").getString("name") != "emerald") continue
                        val details = version.getJSONArray("encounter_details")
                        if (details.length() == 0) continue
                        val detail = details.getJSONObject(0)
                        val method = detail.getJSONObject("method").getString("name")
                        val label = if (method == "surf") "Agua" else "Hierba"
                        add(Encounter(
                            item.getJSONObject("pokemon").getString("name").replace('-', ' ').replaceFirstChar { it.uppercase() },
                            label,
                            detail.getInt("chance"),
                            "Nv. ${detail.getInt("min_level")}–${detail.getInt("max_level")}",
                            item.getJSONObject("pokemon").getString("url").trimEnd('/').substringAfterLast('/').toIntOrNull()
                        ))
                        break
                    }
                }
            }.distinctBy { it.name to it.type }
        }.getOrDefault(emptyList()).ifEmpty { forMap(mapId) }
    }

    suspend fun refreshMap(mapId: String): MapContent = withContext(Dispatchers.IO) {
        val categorized = offlineCategorized(mapId).ifEmpty { refreshCategorizedFromWeb(mapId) }
        val flattened = categorized.values.flatten().map { Encounter(it.name, it.type, it.rate, it.level, it.spriteId) }
        val mapContent = offlineMapContent(mapId)
        MapContent(
            encounters = flattened.ifEmpty { forMap(mapId) },
            categorizedEncounters = categorized,
            items = mapContent?.first ?: EmeraldMapCatalog.itemsFor(mapId),
            trainers = mapContent?.second ?: EmeraldMapCatalog.trainersFor(mapId),
            trades = mapContent?.third.orEmpty()
        )
    }

    private fun offlineMapContent(mapId: String): Triple<List<MapItem>, List<Trainer>, List<TradeOffer>>? {
        val catalog = runCatching {
            mapContentCatalog ?: context.assets.open("emerald_map_content.json")
                .bufferedReader()
                .use { JSONObject(it.readText()).also { parsed -> mapContentCatalog = parsed } }
        }.getOrNull() ?: return null
        val sourceId = mapId
            .replace("MAP_UNDERWATER_ROUTE_", "MAP_UNDERWATER_ROUTE")
            .replace("MAP_ROUTE_", "MAP_ROUTE")
        val map = catalog.optJSONObject("maps")?.optJSONObject(sourceId) ?: return null
        val items = buildList {
            val values = map.optJSONArray("items") ?: return@buildList
            for (index in 0 until values.length()) {
                val item = values.getJSONObject(index)
                add(MapItem(item.optString("name"), item.optInt("quantity", 1), item.optString("spriteKey")))
            }
        }
        val trainers = buildList {
            val values = map.optJSONArray("trainers") ?: return@buildList
            for (index in 0 until values.length()) {
                val trainer = values.getJSONObject(index)
                val pokemon = buildList {
                    val party = trainer.optJSONArray("pokemon") ?: return@buildList
                    for (partyIndex in 0 until party.length()) {
                        val member = party.getJSONObject(partyIndex)
                        add(TrainerPokemon(member.optString("name"), member.optInt("level")))
                    }
                }
                add(Trainer(trainer.optString("name"), pokemon, trainer.optBoolean("isSpecial"), trainer.optString("spriteKey")))
            }
        }
        val trades = buildList {
            val values = map.optJSONArray("trades") ?: return@buildList
            for (index in 0 until values.length()) {
                val trade = values.getJSONObject(index)
                add(TradeOffer(trade.optString("offered"), trade.optString("requested")))
            }
        }
        return Triple(items, trainers, trades)
    }

    private fun offlineCategorized(mapId: String): Map<EncounterCategory, List<CategorizedEncounter>> {
        val catalog = runCatching {
            offlineCatalog ?: context.assets.open("emerald_wild_encounters.json")
                .bufferedReader()
                .use { JSONObject(it.readText()).also { parsed -> offlineCatalog = parsed } }
        }.getOrNull() ?: return EncounterCatalog.groupedFor(mapId)
        val speciesIds = runCatching {
            speciesCatalog ?: context.assets.open("emerald_species_ids.json")
                .bufferedReader()
                .use { JSONObject(it.readText()).also { parsed -> speciesCatalog = parsed } }
        }.getOrNull()

        val sourceId = mapId
            .replace("MAP_UNDERWATER_ROUTE_", "MAP_UNDERWATER_ROUTE")
            .replace("MAP_ROUTE_", "MAP_ROUTE")
        val encounter = findMapEncounter(catalog, sourceId) ?: return EncounterCatalog.groupedFor(mapId)
        val grouped = linkedMapOf<EncounterCategory, List<CategorizedEncounter>>()
        addOfflineMethod(grouped, encounter, speciesIds, "land_mons", EncounterCategory.GRASS, intArrayOf(20, 20, 10, 10, 10, 10, 5, 5, 4, 4, 1, 1))
        addOfflineMethod(grouped, encounter, speciesIds, "fishing_mons", EncounterCategory.FISHING, intArrayOf(70, 30, 60, 20, 20, 40, 40, 15, 4, 1))
        addOfflineMethod(grouped, encounter, speciesIds, "water_mons", EncounterCategory.SURF, intArrayOf(60, 30, 5, 4, 1))
        return grouped
    }

    private fun findMapEncounter(catalog: JSONObject, sourceId: String): JSONObject? {
        val groups = catalog.optJSONArray("wild_encounter_groups") ?: return null
        for (groupIndex in 0 until groups.length()) {
            val group = groups.getJSONObject(groupIndex)
            if (!group.optBoolean("for_maps", false)) continue
            val encounters = group.optJSONArray("encounters") ?: continue
            for (mapIndex in 0 until encounters.length()) {
                val encounter = encounters.getJSONObject(mapIndex)
                if (encounter.optString("map") == sourceId) return encounter
            }
        }
        return null
    }

    private fun addOfflineMethod(
        grouped: MutableMap<EncounterCategory, List<CategorizedEncounter>>,
        encounter: JSONObject,
        speciesIds: JSONObject?,
        field: String,
        category: EncounterCategory,
        slotRates: IntArray
    ) {
        val method = encounter.optJSONObject(field) ?: return
        val mons = method.optJSONArray("mons") ?: return
        val bySpecies = linkedMapOf<String, CategorizedEncounter>()
        for (index in 0 until mons.length()) {
            val mon = mons.getJSONObject(index)
            val species = mon.optString("species").removePrefix("SPECIES_")
            val candidate = CategorizedEncounter(
                name = species.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { character -> character.uppercase() } },
                type = category.displayName(),
                rate = slotRates.getOrElse(index) { 1 },
                level = "Nv. ${mon.optInt("min_level")}–${mon.optInt("max_level")}",
                spriteId = speciesIds?.optJSONArray("results")?.let { results ->
                    val apiName = species.lowercase().replace('_', '-').replace(' ', '-')
                    (0 until results.length()).firstOrNull { index -> results.getJSONObject(index).optString("name") == apiName }?.plus(1)
                },
                category = category
            )
            val existing = bySpecies[species]
            if (existing == null || candidate.rate > existing.rate) bySpecies[species] = candidate
        }
        if (bySpecies.isNotEmpty()) grouped[category] = bySpecies.values.sortedByDescending { it.rate }
    }

    private fun EncounterCategory.displayName(): String = when (this) {
        EncounterCategory.GRASS -> "Hierba"
        EncounterCategory.FISHING -> "Pesca"
        EncounterCategory.SURF -> "Surf"
    }

    private suspend fun refreshCategorizedFromWeb(mapId: String): Map<EncounterCategory, List<CategorizedEncounter>> = withContext(Dispatchers.IO) {
        runCatching {
            val slug = apiAreaSlug(mapId)
            val connection = URL("https://pokeapi.co/api/v2/location-area/$slug/").openConnection() as HttpURLConnection
            connection.connectTimeout = 2500
            connection.readTimeout = 2500
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val grouped = mutableMapOf<EncounterCategory, MutableMap<String, CategorizedEncounter>>()
            val all = root.getJSONArray("pokemon_encounters")
            for (index in 0 until all.length()) {
                val item = all.getJSONObject(index)
                val pokemon = item.getJSONObject("pokemon")
                val name = pokemon.getString("name").replace('-', ' ').replaceFirstChar { it.uppercase() }
                val spriteId = pokemon.getString("url").trimEnd('/').substringAfterLast('/').toIntOrNull()
                val versions = item.getJSONArray("version_details")
                for (versionIndex in 0 until versions.length()) {
                    val version = versions.getJSONObject(versionIndex)
                    if (version.getJSONObject("version").getString("name") != "emerald") continue
                    val details = version.getJSONArray("encounter_details")
                    for (detailIndex in 0 until details.length()) {
                        val detail = details.getJSONObject(detailIndex)
                        val category = when (detail.getJSONObject("method").getString("name")) {
                            "surf" -> EncounterCategory.SURF
                            "old-rod", "good-rod", "super-rod" -> EncounterCategory.FISHING
                            "walk" -> EncounterCategory.GRASS
                            else -> null
                        } ?: continue
                        val candidate = CategorizedEncounter(name, category.name, detail.getInt("chance"), "Nv. ${detail.getInt("min_level")}–${detail.getInt("max_level")}", spriteId, category)
                        val existing = grouped.getOrPut(category) { mutableMapOf() }[name]
                        if (existing == null || candidate.rate > existing.rate) grouped.getValue(category)[name] = candidate
                    }
                    break
                }
            }
            grouped.mapValues { (_, values) -> values.values.sortedByDescending { it.rate } }
        }.getOrElse { EncounterCatalog.groupedFor(mapId) }
    }

    private fun apiAreaSlug(mapId: String): String {
        val base = mapId.removePrefix("MAP_").lowercase().replace('_', '-')
        return if (base.startsWith("route-")) "hoenn-$base-area" else "$base-area"
    }
}

data class MapContent(
    val encounters: List<Encounter>,
    val categorizedEncounters: Map<EncounterCategory, List<CategorizedEncounter>>,
    val items: List<MapItem>,
    val trainers: List<Trainer>,
    val trades: List<TradeOffer>
)
object EncounterCatalog {
    val route101 = listOf(
        Encounter("Poochyena", "Siniestro", 45, "Nv. 2–3"),
        Encounter("Zigzagoon", "Normal", 45, "Nv. 2–3"),
        Encounter("Wurmple", "Bicho", 10, "Nv. 2–3")
    )
    private val route102 = listOf(
        Encounter("Zigzagoon", "Normal", 30, "Nv. 3–4"),
        Encounter("Poochyena", "Siniestro", 30, "Nv. 3–4"),
        Encounter("Ralts", "Psíquico", 4, "Nv. 4–5"),
        Encounter("Seedot", "Planta", 20, "Nv. 3–5")
    )
        private val route103 = listOf(
            Encounter("Poochyena", "Siniestro", 30, "Nv. 2–4", 261),
            Encounter("Zigzagoon", "Normal", 20, "Nv. 2–4", 263),
            Encounter("Wingull", "Agua/Volador", 30, "Nv. 2–4", 278),
            Encounter("Lotad", "Agua/Planta", 20, "Nv. 2–4", 270)
        )

    fun forMap(mapId: String): List<Encounter> = when (mapId) {
        "MAP_ROUTE_101" -> route101
        "MAP_ROUTE_102" -> route102
        "MAP_ROUTE_103" -> route103
        else -> emptyList()
    }

    fun nameFor(mapId: String): String = when (mapId) {
        "MAP_ROUTE_101" -> "Ruta 101"
        "MAP_ROUTE_102" -> "Ruta 102"
        else -> "Mapa desconocido"
    }

    fun groupedFor(mapId: String): Map<EncounterCategory, List<CategorizedEncounter>> = when (mapId) {
        "MAP_ROUTE_101" -> mapOf(EncounterCategory.GRASS to route101.map { it.toCategorized(EncounterCategory.GRASS) })
        "MAP_ROUTE_102" -> mapOf(EncounterCategory.GRASS to route102.map { it.toCategorized(EncounterCategory.GRASS) })
        "MAP_ROUTE_103" -> mapOf(EncounterCategory.GRASS to route103.map { it.toCategorized(EncounterCategory.GRASS) })
        else -> emptyMap()
    }

    private fun Encounter.toCategorized(category: EncounterCategory) = CategorizedEncounter(name, type, rate, level, spriteId, category)
}