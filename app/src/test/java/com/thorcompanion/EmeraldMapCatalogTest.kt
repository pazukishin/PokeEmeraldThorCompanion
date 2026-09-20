package com.thorcompanion

import org.junit.Assert.assertEquals
import org.junit.Test

class EmeraldMapCatalogTest {
    @Test
    fun parseMap_retroArchResponse_oldaleTownUsesGroupAndNumber() {
        val map = RetroArchClient().parseMap("READ_CORE_MEMORY 20322e4 00 0A")

        assertEquals("MAP_OLDALE_TOWN", map?.mapId)
        assertEquals("Pueblo Escaso", map?.mapName)
    }

    @Test
    fun parseMap_retroArchResponse_route101UsesGroupAndNumber() {
        val map = RetroArchClient().parseMap("READ_CORE_MEMORY 20322e4 00 10")

        assertEquals("MAP_ROUTE_101", map?.mapId)
        assertEquals("Ruta 101", map?.mapName)
    }

    @Test
    fun fromMemoryValue_route101_mapsToEmeraldRoute() {
        val map = EmeraldMapCatalog.fromMemoryValue(0x0010)
        assertEquals("MAP_ROUTE_101", map.id)
        assertEquals("Ruta 101", map.name)
    }

    @Test
    fun resolveMapId_route102_mapsCorrectly() {
        val map = EmeraldMapCatalog.resolveMapId(0, 17)
        assertEquals("MAP_ROUTE_102", map.id)
        assertEquals("Ruta 102", map.name)
    }

    @Test
    fun resolveMapId_cityInterior_usesGroupAndNumber() {
        val map = EmeraldMapCatalog.resolveMapId(2, 2)
        assertEquals("MAP_OLDALE_TOWN_POKEMON_CENTER_1F", map.id)
        assertEquals("Oldale Town Pokemon Center 1F", map.name)
    }

    @Test
    fun resolveMapId_dungeon_usesDungeonGroup() {
        val map = EmeraldMapCatalog.resolveMapId(24, 11)
        assertEquals("MAP_PETALBURG_WOODS", map.id)
        assertEquals("Petalburg Woods", map.name)
    }

    @Test
    fun route103_hasOfflineGrassEncounters() {
        val encounters = EncounterCatalog.groupedFor("MAP_ROUTE_103")[EncounterCategory.GRASS].orEmpty()

        assertEquals(4, encounters.size)
        assertEquals("Poochyena", encounters.first().name)
    }
}
