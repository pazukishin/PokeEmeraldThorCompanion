package com.thorcompanion

object EmeraldMapContentCatalog {
    private val items = mapOf(
        "MAP_ROUTE102" to listOf(MapItem("Poción", spriteKey = "potion"), MapItem("Baya Aranja", spriteKey = "oran-berry"), MapItem("Baya Meloc", spriteKey = "pecha-berry")),
        "MAP_ROUTE103" to listOf(
            MapItem("Protec. Esp.", 1, "guard-spec"),
            MapItem("Más PP", 1, "pp-up"),
            MapItem("Baya Safre", 2, "cheri-berry"),
            MapItem("Baya Zanama", 1, "leppa-berry")
        )
    )

    private val trainers = mapOf(
        "MAP_ROUTE102" to listOf(
            Trainer("Joven Calvin", listOf(TrainerPokemon("Zigzagoon", 5))),
            Trainer("Chica Tiana", listOf(TrainerPokemon("Zigzagoon", 4), TrainerPokemon("Poochyena", 4))),
            Trainer("Joven Scott", listOf(TrainerPokemon("Poochyena", 5)))
        ),
        "MAP_ROUTE103" to listOf(
            Trainer("Rival", listOf(TrainerPokemon("Treecko", 5), TrainerPokemon("Torchic", 5), TrainerPokemon("Mudkip", 5)), true),
            Trainer("Chica Daisy", listOf(TrainerPokemon("Zigzagoon", 4))),
            Trainer("Gemelas Amy y Liv", listOf(TrainerPokemon("Lotad", 6), TrainerPokemon("Seedot", 6))),
            Trainer("Pescador Andrew", listOf(TrainerPokemon("Magikarp", 5), TrainerPokemon("Magikarp", 5))),
            Trainer("Pescador Miguel", listOf(TrainerPokemon("Magikarp", 6))),
            Trainer("Entrenador Marcos", listOf(TrainerPokemon("Poochyena", 6))),
            Trainer("Nadadora Isabelle", listOf(TrainerPokemon("Wingull", 6))),
            Trainer("Nadador Pete", listOf(TrainerPokemon("Wingull", 6))),
            Trainer("Karateka Rhett", listOf(TrainerPokemon("Makuhita", 7)))
        )
    )

    fun itemsFor(mapId: String): List<MapItem> = items[mapId].orEmpty()

    fun trainersFor(mapId: String): List<Trainer> = trainers[mapId].orEmpty()
}

fun EmeraldMapCatalog.itemsFor(mapId: String): List<MapItem> = EmeraldMapContentCatalog.itemsFor(mapId)

fun EmeraldMapCatalog.trainersFor(mapId: String): List<Trainer> = EmeraldMapContentCatalog.trainersFor(mapId)
