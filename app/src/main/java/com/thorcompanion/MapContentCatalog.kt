package com.thorcompanion

object EmeraldMapContentCatalog {
    private val items = mapOf(
        "MAP_ROUTE102" to listOf(MapItem("Potion", spriteKey = "potion"), MapItem("Oran Berry", spriteKey = "oran-berry"), MapItem("Pecha Berry", spriteKey = "pecha-berry")),
        "MAP_ROUTE103" to listOf(
            MapItem("Guard Spec.", 1, "guard-spec"),
            MapItem("PP Up", 1, "pp-up"),
            MapItem("Cheri Berry", 2, "cheri-berry"),
            MapItem("Leppa Berry", 1, "leppa-berry")
        )
    )

    private val trainers = mapOf(
        "MAP_ROUTE102" to listOf(
            Trainer("Youngster Calvin", listOf(TrainerPokemon("Zigzagoon", 5))),
            Trainer("Lass Tiana", listOf(TrainerPokemon("Zigzagoon", 4), TrainerPokemon("Poochyena", 4))),
            Trainer("Youngster Scott", listOf(TrainerPokemon("Poochyena", 5)))
        ),
        "MAP_ROUTE103" to listOf(
            Trainer("Rival", listOf(TrainerPokemon("Treecko", 5), TrainerPokemon("Torchic", 5), TrainerPokemon("Mudkip", 5)), true),
            Trainer("Lass Daisy", listOf(TrainerPokemon("Zigzagoon", 4))),
            Trainer("Twins Amy & Liv", listOf(TrainerPokemon("Lotad", 6), TrainerPokemon("Seedot", 6))),
            Trainer("Fisherman Andrew", listOf(TrainerPokemon("Magikarp", 5), TrainerPokemon("Magikarp", 5))),
            Trainer("Fisherman Miguel", listOf(TrainerPokemon("Magikarp", 6))),
            Trainer("Trainer Marcos", listOf(TrainerPokemon("Poochyena", 6))),
            Trainer("Swimmer Isabelle", listOf(TrainerPokemon("Wingull", 6))),
            Trainer("Swimmer Pete", listOf(TrainerPokemon("Wingull", 6))),
            Trainer("Black Belt Rhett", listOf(TrainerPokemon("Makuhita", 7)))
        )
    )

    fun itemsFor(mapId: String): List<MapItem> = items[mapId].orEmpty()

    fun trainersFor(mapId: String): List<Trainer> = trainers[mapId].orEmpty()
}

fun EmeraldMapCatalog.itemsFor(mapId: String): List<MapItem> = EmeraldMapContentCatalog.itemsFor(mapId)

fun EmeraldMapCatalog.trainersFor(mapId: String): List<Trainer> = EmeraldMapContentCatalog.trainersFor(mapId)
