package com.thorcompanion

/** Map group/number order from pokeemerald's data/maps/map_groups.json. */
object EmeraldMapCatalog {
    private val entries = mutableMapOf<Pair<Int, Int>, EmeraldMap>().apply {
        addGroup(0, "Petalburg City", "Slateport City", "Mauville City", "Rustboro City", "Fortree City", "Lilycove City", "Mossdeep City", "Sootopolis City", "Ever Grande City", "Littleroot Town", "Oldale Town", "Dewford Town", "Lavaridge Town", "Fallarbor Town", "Verdanturf Town", "Pacifidlog Town", *routes(101..134), "Underwater Route 124", "Underwater Route 126", "Underwater Route 127", "Underwater Route 128", "Underwater Route 129", "Underwater Route 105", "Underwater Route 125")
        addGroup(1, "Littleroot Town Brendan's House 1F", "Littleroot Town Brendan's House 2F", "Littleroot Town May's House 1F", "Littleroot Town May's House 2F", "Littleroot Town Professor Birch's Lab")
        addGroup(2, "Oldale Town House 1", "Oldale Town House 2", "Oldale Town Pokemon Center 1F", "Oldale Town Pokemon Center 2F", "Oldale Town Mart")
        addGroup(3, "Dewford Town House 1", "Dewford Town Pokemon Center 1F", "Dewford Town Pokemon Center 2F", "Dewford Town Gym", "Dewford Town Hall", "Dewford Town House 2")
        addGroup(4, "Lavaridge Town Herb Shop", "Lavaridge Town Gym 1F", "Lavaridge Town Gym B1F", "Lavaridge Town House", "Lavaridge Town Mart", "Lavaridge Town Pokemon Center 1F", "Lavaridge Town Pokemon Center 2F")
        addGroup(5, "Fallarbor Town Mart", "Fallarbor Town Battle Tent Lobby", "Fallarbor Town Battle Tent Corridor", "Fallarbor Town Battle Tent Battle Room", "Fallarbor Town Pokemon Center 1F", "Fallarbor Town Pokemon Center 2F", "Fallarbor Town Cozmo's House", "Fallarbor Town Move Relearner's House")
        addGroup(6, "Verdanturf Town Battle Tent Lobby", "Verdanturf Town Battle Tent Corridor", "Verdanturf Town Battle Tent Battle Room", "Verdanturf Town Mart", "Verdanturf Town Pokemon Center 1F", "Verdanturf Town Pokemon Center 2F", "Verdanturf Town Wanda's House", "Verdanturf Town Friendship Rater's House", "Verdanturf Town House")
        addGroup(7, "Pacifidlog Town Pokemon Center 1F", "Pacifidlog Town Pokemon Center 2F", "Pacifidlog Town House 1", "Pacifidlog Town House 2", "Pacifidlog Town House 3", "Pacifidlog Town House 4", "Pacifidlog Town House 5")
        addGroup(8, "Petalburg City Wally's House", "Petalburg City Gym", "Petalburg City House 1", "Petalburg City House 2", "Petalburg City Pokemon Center 1F", "Petalburg City Pokemon Center 2F", "Petalburg City Mart")
        addGroup(9, "Slateport City Stern's Shipyard 1F", "Slateport City Stern's Shipyard 2F", "Slateport City Battle Tent Lobby", "Slateport City Battle Tent Corridor", "Slateport City Battle Tent Battle Room", "Slateport City Name Rater's House", "Slateport City Pokemon Fan Club", "Slateport City Oceanic Museum 1F", "Slateport City Oceanic Museum 2F", "Slateport City Harbor", "Slateport City House", "Slateport City Pokemon Center 1F", "Slateport City Pokemon Center 2F", "Slateport City Mart")
        addGroup(10, "Mauville City Gym", "Mauville City Bike Shop", "Mauville City House 1", "Mauville City Game Corner", "Mauville City House 2", "Mauville City Pokemon Center 1F", "Mauville City Pokemon Center 2F", "Mauville City Mart")
        addGroup(11, "Rustboro City Devon Corp 1F", "Rustboro City Devon Corp 2F", "Rustboro City Devon Corp 3F", "Rustboro City Gym", "Rustboro City Pokemon School", "Rustboro City Pokemon Center 1F", "Rustboro City Pokemon Center 2F", "Rustboro City Mart", "Rustboro City Flat 1 1F", "Rustboro City Flat 1 2F", "Rustboro City House 1", "Rustboro City Cutter's House", "Rustboro City House 2", "Rustboro City Flat 2 1F", "Rustboro City Flat 2 2F", "Rustboro City Flat 2 3F", "Rustboro City House 3")
        addGroup(12, "Fortree City House 1", "Fortree City Gym", "Fortree City Pokemon Center 1F", "Fortree City Pokemon Center 2F", "Fortree City Mart", "Fortree City House 2", "Fortree City House 3", "Fortree City House 4", "Fortree City House 5", "Fortree City Decoration Shop")
        addGroup(13, "Lilycove City Cove Lily Motel 1F", "Lilycove City Cove Lily Motel 2F", "Lilycove City Museum 1F", "Lilycove City Museum 2F", "Lilycove City Contest Lobby", "Lilycove City Contest Hall", "Lilycove City Pokemon Center 1F", "Lilycove City Pokemon Center 2F", "Lilycove City Unused Mart", "Lilycove City Pokemon Trainer Fan Club", "Lilycove City Harbor", "Lilycove City Move Deleter's House", "Lilycove City House 1", "Lilycove City House 2", "Lilycove City House 3", "Lilycove City House 4", "Lilycove City Department Store 1F", "Lilycove City Department Store 2F", "Lilycove City Department Store 3F", "Lilycove City Department Store 4F", "Lilycove City Department Store 5F", "Lilycove City Department Store Rooftop", "Lilycove City Department Store Elevator")
        addGroup(14, "Mossdeep City Gym", "Mossdeep City House 1", "Mossdeep City House 2", "Mossdeep City Pokemon Center 1F", "Mossdeep City Pokemon Center 2F", "Mossdeep City Mart", "Mossdeep City House 3", "Mossdeep City Steven's House", "Mossdeep City House 4", "Mossdeep City Space Center 1F", "Mossdeep City Space Center 2F", "Mossdeep City Game Corner 1F", "Mossdeep City Game Corner B1F")
        addGroup(15, "Sootopolis City Gym 1F", "Sootopolis City Gym B1F", "Sootopolis City Pokemon Center 1F", "Sootopolis City Pokemon Center 2F", "Sootopolis City Mart", "Sootopolis City House 1", "Sootopolis City House 2", "Sootopolis City House 3", "Sootopolis City House 4", "Sootopolis City House 5", "Sootopolis City House 6", "Sootopolis City House 7", "Sootopolis City Lotad and Seedot House", "Sootopolis City Mystery Events House 1F", "Sootopolis City Mystery Events House B1F")
        addGroup(16, "Ever Grande City Sidney's Room", "Ever Grande City Phoebe's Room", "Ever Grande City Glacia's Room", "Ever Grande City Drake's Room", "Ever Grande City Champion's Room", "Ever Grande City Hall 1", "Ever Grande City Hall 2", "Ever Grande City Hall 3", "Ever Grande City Hall 4", "Ever Grande City Hall 5", "Ever Grande City Pokemon League 1F", "Ever Grande City Hall of Fame", "Ever Grande City Pokemon Center 1F", "Ever Grande City Pokemon Center 2F", "Ever Grande City Pokemon League 2F")
        addGroup(17, "Route 104 Mr. Briney's House", "Route 104 Pretty Petal Flower Shop")
        addGroup(18, "Route 111 Winstrate Family's House", "Route 111 Old Lady's Rest Stop")
        addGroup(19, "Route 112 Cable Car Station", "Mt. Chimney Cable Car Station")
        addGroup(20, "Route 114 Fossil Maniac's House", "Route 114 Fossil Maniac's Tunnel", "Route 114 Lanette's House")
        addGroup(21, "Route 116 Tunneler's Rest House")
        addGroup(22, "Route 117 Pokemon Day Care")
        addGroup(23, "Route 121 Safari Zone Entrance")
        addGroup(24, "Meteor Falls 1F 1R", "Meteor Falls 1F 2R", "Meteor Falls B1F 1R", "Meteor Falls B1F 2R", "Rusturf Tunnel", "Underwater Sootopolis City", "Desert Ruins", "Granite Cave 1F", "Granite Cave B1F", "Granite Cave B2F", "Granite Cave Steven's Room", "Petalburg Woods", "Mt. Chimney", "Jagged Pass", "Fiery Path", "Mt. Pyre 1F", "Mt. Pyre 2F", "Mt. Pyre 3F", "Mt. Pyre 4F", "Mt. Pyre 5F", "Mt. Pyre 6F", "Mt. Pyre Exterior", "Mt. Pyre Summit", "Aqua Hideout 1F", "Aqua Hideout B1F", "Aqua Hideout B2F", "Underwater Seafloor Cavern", "Seafloor Cavern Entrance", "Seafloor Cavern Room 1", "Seafloor Cavern Room 2", "Seafloor Cavern Room 3", "Seafloor Cavern Room 4", "Seafloor Cavern Room 5", "Seafloor Cavern Room 6", "Seafloor Cavern Room 7", "Seafloor Cavern Room 8", "Seafloor Cavern Room 9", "Cave of Origin Entrance", "Cave of Origin 1F", "Cave of Origin Unused Ruby Sapphire Map 1", "Cave of Origin Unused Ruby Sapphire Map 2", "Cave of Origin Unused Ruby Sapphire Map 3", "Cave of Origin B1F", "Victory Road 1F", "Victory Road B1F", "Victory Road B2F", "Shoal Cave Low Tide Entrance Room", "Shoal Cave Low Tide Inner Room", "Shoal Cave Low Tide Stairs Room", "Shoal Cave Low Tide Lower Room", "Shoal Cave High Tide Entrance Room", "Shoal Cave High Tide Inner Room", "New Mauville Entrance", "New Mauville Inside", "Abandoned Ship Deck", "Abandoned Ship Corridors 1F", "Abandoned Ship Rooms 1F", "Abandoned Ship Corridors B1F", "Abandoned Ship Rooms B1F", "Abandoned Ship Rooms 2 B1F", "Abandoned Ship Underwater 1", "Abandoned Ship Room B1F", "Abandoned Ship Rooms 2 1F", "Abandoned Ship Captain's Office", "Abandoned Ship Underwater 2", "Abandoned Ship Hidden Floor Corridors", "Abandoned Ship Hidden Floor Rooms", "Island Cave", "Ancient Tomb", "Underwater Route 134", "Underwater Sealed Chamber", "Sealed Chamber Outer Room", "Sealed Chamber Inner Room", "Scorched Slab", "Sky Pillar Entrance", "Sky Pillar Outside", "Sky Pillar 1F", "Sky Pillar 2F", "Sky Pillar 3F", "Sky Pillar 4F", "Sky Pillar 5F", "Sky Pillar Top", "Magma Hideout 1F", "Magma Hideout 2F 1R", "Magma Hideout 2F 2R", "Magma Hideout 3F 1R", "Magma Hideout 3F 2R", "Magma Hideout 4F", "Magma Hideout 3F 3R", "Magma Hideout 2F 3R", "Mirage Tower 1F", "Mirage Tower 2F", "Mirage Tower 3F", "Mirage Tower 4F", "Desert Underpass", "Artisan Cave B1F", "Artisan Cave 1F", "Underwater Marine Cave", "Marine Cave Entrance", "Marine Cave End", "Terra Cave Entrance", "Terra Cave End", "Altering Cave", "Meteor Falls Steven's Cave")
        addGroup(26, "Safari Zone Northwest", "Safari Zone North", "Safari Zone Southwest", "Safari Zone South", "Battle Frontier Outside West", "Southern Island Exterior", "Southern Island Interior", "Safari Zone Rest House", "Safari Zone Northeast", "Safari Zone Southeast", "Battle Frontier Outside East", "Faraway Island Entrance", "Faraway Island Interior", "Birth Island Exterior", "Birth Island Harbor", "Trainer Hill Entrance", "Trainer Hill 1F", "Trainer Hill 2F", "Trainer Hill 3F", "Trainer Hill 4F", "Trainer Hill Roof", "Navel Rock Exterior", "Navel Rock Harbor", "Navel Rock Entrance", "Navel Rock B1F", "Navel Rock Fork", "Navel Rock Up 1", "Navel Rock Up 2", "Navel Rock Up 3", "Navel Rock Up 4", "Navel Rock Top", "Navel Rock Down 01", "Navel Rock Down 02", "Navel Rock Down 03", "Navel Rock Down 04", "Navel Rock Down 05", "Navel Rock Down 06", "Navel Rock Down 07", "Navel Rock Down 08", "Navel Rock Down 09", "Navel Rock Down 10", "Navel Rock Down 11", "Navel Rock Bottom", "Trainer Hill Elevator")
    }

    private fun MutableMap<Pair<Int, Int>, EmeraldMap>.addGroup(group: Int, vararg names: String) {
        names.forEachIndexed { number, name -> put(group to number, EmeraldMap(toMapId(name), displayName(name))) }
    }

    private fun routes(range: IntRange): Array<String> = range.map { "Route $it" }.toTypedArray()

    private fun toMapId(name: String): String = "MAP_" + name.uppercase()
        .replace("'", "")
        .replace(".", "")
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')

    private fun displayName(name: String): String = when {
        name.startsWith("Route ") -> name.replace("Route ", "Ruta ")
        name == "Petalburg City" -> "Ciudad Petalburg"
        name == "Slateport City" -> "Ciudad Portual"
        name == "Mauville City" -> "Ciudad Malvalona"
        name == "Rustboro City" -> "Ciudad Férrica"
        name == "Littleroot Town" -> "Pueblo Raíz"
        name == "Oldale Town" -> "Pueblo Escaso"
        name == "Dewford Town" -> "Pueblo Azuliza"
        name == "Lavaridge Town" -> "Pueblo Lavacalda"
        name == "Fallarbor Town" -> "Pueblo Pardal"
        name == "Verdanturf Town" -> "Pueblo Verdegal"
        name == "Pacifidlog Town" -> "Pueblo Oromar"
        else -> name
    }

    fun resolveMapId(mapGroup: Int, mapNumber: Int): EmeraldMap = entries[mapGroup to mapNumber]
        ?: EmeraldMap("UNKNOWN_GROUP_${mapGroup}_MAP_${mapNumber}", "Mapa desconocido ($mapGroup:$mapNumber)")

    fun fromMemoryValue(value: Int): EmeraldMap = resolveMapId((value ushr 8) and 0xFF, value and 0xFF)
}
