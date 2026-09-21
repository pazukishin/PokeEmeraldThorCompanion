package com.thorcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Encounter(val name: String, val type: String, val rate: Int, val level: String, val spriteId: Int? = null)
enum class EncounterCategory { GRASS, FISHING, SURF }
data class CategorizedEncounter(
    val name: String,
    val type: String,
    val rate: Int,
    val level: String,
    val spriteId: Int?,
    val category: EncounterCategory
)
data class MapItem(val name: String, val quantity: Int = 1, val spriteKey: String = name)
data class TrainerPokemon(val name: String, val level: Int)
data class Trainer(val name: String, val pokemon: List<TrainerPokemon>, val isSpecial: Boolean = false, val spriteKey: String = "")
data class TradeOffer(val gives: String, val requests: String)
enum class CompanionTab { TEAM, ENCOUNTERS, ITEMS, TRAINERS }
data class EvolutionBranch(val condition: String, val target: EvolutionNode)
data class EvolutionNode(val species: String, val branches: List<EvolutionBranch> = emptyList())
data class PokemonDetails(
    val name: String,
    val types: List<String>,
    val abilities: List<String>,
    val evolution: EvolutionNode?,
    val description: String,
    val normalSpriteUrl: String?,
    val shinySpriteUrl: String?
)
data class ItemDetails(val name: String, val description: String, val effect: String, val spriteUrl: String?)
sealed interface DetailState {
    data class Pokemon(val value: PokemonDetails) : DetailState
    data class Item(val value: ItemDetails) : DetailState
    data class Trainer(val value: com.thorcompanion.Trainer) : DetailState
}

data class ConnectionLog(val timestamp: String, val message: String)

data class PokemonInfo(
    val speciesId: Int,
    val personality: Int,
    val status: Int = 0,
    val name: String,
    val level: Int,
    val nature: String,
    val natureModifier: String,
    val ability: String? = null,
    val gender: Gender = Gender.GENDERLESS,
    val catchRate: Int = 0,
    val heldItemName: String? = null,
    val moves: List<String> = emptyList(),
    val ivs: PokemonDecoder.IvSpread,
    val evs: PokemonDecoder.IvSpread? = null,
    val friendship: Int? = null,
    val hp: Int? = null,
    val maxHp: Int? = null,
    val attack: Int? = null,
    val defense: Int? = null,
    val speed: Int? = null,
    val spAttack: Int? = null,
    val spDefense: Int? = null
) {
    val hiddenPowerType: String get() = ivs.hiddenPowerType
}

data class BattleInfo(
    val isTrainer: Boolean,
    val opponent: PokemonInfo,
    val team: List<PokemonInfo>,
    val trainerName: String? = null,
    val trainerSprite: String? = null
)

data class CompanionState(
    val connected: Boolean = false,
    val host: String = "127.0.0.1",
    val port: String = "55355",
    val mapName: String = "Ruta 101",
    val mapId: String = "MAP_ROUTE101",
    val encounters: List<Encounter> = EncounterCatalog.route101,
    val categorizedEncounters: Map<EncounterCategory, List<CategorizedEncounter>> = EncounterCatalog.groupedFor("MAP_ROUTE101"),
    val items: List<MapItem> = EmeraldMapCatalog.itemsFor("MAP_ROUTE101"),
    val trainers: List<Trainer> = EmeraldMapCatalog.trainersFor("MAP_ROUTE101"),
    val trades: List<TradeOffer> = emptyList(),
    val selectedTab: CompanionTab = CompanionTab.TEAM,
    val detail: DetailState? = null,
    val detailLoading: Boolean = false,
    val message: String = "Sin conexión",
    val logs: List<ConnectionLog> = emptyList(),
    val showLogs: Boolean = false,
    val battle: BattleInfo? = null,
    val team: List<PokemonInfo> = emptyList()
)

class CompanionViewModel(application: Application) : AndroidViewModel(application) {
    private val client = RetroArchClient()
    private val repository = EncounterRepository(application)
    private val _state = MutableStateFlow(CompanionState())
    val state: StateFlow<CompanionState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    private fun addLog(message: String) {
        val now = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = ConnectionLog(timestamp = now, message = message)
        val updatedLogs = (listOf(entry) + _state.value.logs).take(200)
        _state.value = _state.value.copy(logs = updatedLogs)
    }

    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }

    fun toggleLogs() {
        _state.value = _state.value.copy(showLogs = !_state.value.showLogs)
    }

    fun closeLogs() {
        _state.value = _state.value.copy(showLogs = false)
    }

    fun updateHost(host: String) {
        _state.value = _state.value.copy(host = host.trim())
    }

    fun updatePort(port: String) {
        _state.value = _state.value.copy(port = port.trim())
    }

    fun connect() {
        if (refreshJob?.isActive == true) return
        val host = _state.value.host
        val port = _state.value.port.toIntOrNull() ?: 55355
        refreshJob = viewModelScope.launch {
            _state.value = _state.value.copy(message = "Conectando con RetroArch…")
            addLog("Intentando conectar a $host:$port")
            var lastMapId: String? = null
            while (true) {
                val result = client.readMap(host, port) { logLine -> addLog(logLine) }
                if (result != null) {
                    val mapChanged = result.mapId != lastMapId
                    val mapData = if (mapChanged) repository.refreshMap(result.mapId) else null
                    _state.value = _state.value.copy(
                        connected = true,
                        mapId = result.mapId,
                        mapName = result.mapName,
                        encounters = mapData?.encounters ?: _state.value.encounters,
                        categorizedEncounters = mapData?.categorizedEncounters ?: _state.value.categorizedEncounters,
                        items = mapData?.items ?: _state.value.items,
                        trainers = mapData?.trainers ?: _state.value.trainers,
                        trades = mapData?.trades ?: _state.value.trades,
                        message = if (mapChanged) "UDP conectado · mapa actualizado" else "UDP conectado · esperando cambios"
                    )
                    if (mapChanged) {
                        addLog("Mapa recibido: ${result.mapId} / ${result.mapName}")
                        lastMapId = result.mapId
                    }
                } else if (lastMapId == null) {
                    _state.value = _state.value.copy(message = "No responde RetroArch · revisa UDP Control")
                    addLog("No se recibió respuesta válida desde RetroArch")
                    break
                }
                pollBattleAndSummary(host, port)
                delay(1500)
            }
        }
    }

    fun selectTab(tab: CompanionTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun showPokemonDetails(encounter: Encounter) {
        viewModelScope.launch {
            _state.value = _state.value.copy(detailLoading = true)
            repository.loadPokemonDetails(encounter)?.let { details ->
                _state.value = _state.value.copy(detail = DetailState.Pokemon(details), detailLoading = false)
            } ?: run { _state.value = _state.value.copy(detailLoading = false) }
        }
    }

    fun showItemDetails(item: MapItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(detailLoading = true)
            repository.loadItemDetails(item)?.let { details ->
                _state.value = _state.value.copy(detail = DetailState.Item(details), detailLoading = false)
            } ?: run { _state.value = _state.value.copy(detailLoading = false) }
        }
    }

    fun showTrainerDetails(trainer: Trainer) {
        _state.value = _state.value.copy(detail = DetailState.Trainer(trainer))
    }

    fun closeDetails() {
        _state.value = _state.value.copy(detail = null, detailLoading = false)
    }

    suspend fun loadEvolutionForSpecies(speciesId: Int): EvolutionNode? =
        repository.loadEvolutionForSpecies(speciesId)

    fun closeBattle() {
        _state.value = _state.value.copy(battle = null)
    }

    private fun ByteArray.toUInt32(): Int =
        (this[0].toInt() and 0xFF) or ((this[1].toInt() and 0xFF) shl 8) or
            ((this[2].toInt() and 0xFF) shl 16) or ((this[3].toInt() and 0xFF) shl 24)

    private suspend fun pollBattleAndSummary(host: String, port: Int) {
        // "In battle" = battle type flags are set and no outcome has been recorded yet.
        // Both live in EWRAM (IWRAM is not exposed by V.GBA-Next's READ_CORE_MEMORY).
        val flagsBytes = client.readBytes(host, port, EmeraldAddresses.BATTLE_TYPE_FLAGS, 4)
        val outcomeBytes = client.readBytes(host, port, EmeraldAddresses.BATTLE_OUTCOME, 1)
        val inBattle = flagsBytes != null && flagsBytes.size >= 4 && flagsBytes.toUInt32() != 0 &&
            outcomeBytes != null && outcomeBytes.isNotEmpty() && (outcomeBytes[0].toInt() and 0xFF) == 0

        if (inBattle) {
            _state.value = _state.value.copy(battle = readBattle(host, port, flagsBytes.toUInt32()))
        } else {
            _state.value = _state.value.copy(battle = null)
        }

        _state.value = _state.value.copy(team = readTeam(host, port))
    }

    private suspend fun readBattle(host: String, port: Int, flags: Int): BattleInfo? {
        val isTrainer = (flags and EmeraldAddresses.BATTLE_TYPE_TRAINER) != 0

        val enemyAddress = EmeraldAddresses.BATTLE_MONS + EmeraldAddresses.ENEMY_BATTLER * EmeraldAddresses.BATTLE_POKEMON_SIZE
        val opponentBytes = client.readBytes(host, port, enemyAddress, EmeraldAddresses.BATTLE_POKEMON_SIZE) ?: return null
        val opponent = PokemonDecoder.decodeBattlePokemon(opponentBytes) ?: return null
        if (opponent.speciesId == 0) return null

        val team = if (isTrainer) readEnemyParty(host, port) else emptyList()
        val trainer = if (isTrainer) readTrainer(host, port) else null
        return BattleInfo(isTrainer, toPokemonInfo(opponent), team, trainer?.first, trainer?.second)
    }

    private suspend fun readTrainer(host: String, port: Int): Pair<String?, String?>? {
        val bytes = client.readBytes(host, port, EmeraldAddresses.TRAINER_OPPONENT_A, 2) ?: return null
        if (bytes.size < 2) return null
        val trainerId = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        if (trainerId == 0) return null
        val app = getApplication<Application>()
        return SpeciesCatalog.trainerNameFor(app, trainerId) to SpeciesCatalog.trainerSpriteUrlFor(app, trainerId)
    }

    private suspend fun readEnemyParty(host: String, port: Int): List<PokemonInfo> {
        // gEnemyPartyCount is computed on demand and can be stale during battle, so read all 6 slots.
        val party = client.readBytes(host, port, EmeraldAddresses.ENEMY_PARTY, 6 * EmeraldAddresses.POKEMON_SIZE) ?: return emptyList()
        return (0 until 6).mapNotNull { i ->
            val slice = party.copyOfRange(i * EmeraldAddresses.POKEMON_SIZE, (i + 1) * EmeraldAddresses.POKEMON_SIZE)
            PokemonDecoder.decodePokemon(slice)?.takeIf { it.speciesId != 0 }?.let { toPokemonInfo(it) }
        }
    }

    private suspend fun readTeam(host: String, port: Int): List<PokemonInfo> {
        val party = client.readBytes(host, port, EmeraldAddresses.PLAYER_PARTY, 6 * EmeraldAddresses.POKEMON_SIZE) ?: return emptyList()
        return (0 until 6).mapNotNull { i ->
            val slice = party.copyOfRange(i * EmeraldAddresses.POKEMON_SIZE, (i + 1) * EmeraldAddresses.POKEMON_SIZE)
            PokemonDecoder.decodePokemon(slice)?.takeIf { it.speciesId != 0 }?.let { toPokemonInfo(it) }
        }
    }

    private fun toPokemonInfo(decoded: PokemonDecoder.DecodedPokemon): PokemonInfo {
        val app = getApplication<Application>()
        val nationalDex = SpeciesCatalog.nationalDexFor(app, decoded.speciesId)
        val name = SpeciesCatalog.nameFor(app, nationalDex) ?: "N.º $nationalDex"
        return PokemonInfo(
            speciesId = nationalDex,
            personality = decoded.personality,
            status = decoded.status,
            name = name,
            level = decoded.level,
            nature = PokemonDecoder.natureName(decoded.nature),
            natureModifier = PokemonDecoder.natureModifier(decoded.nature),
            ability = SpeciesCatalog.abilityFor(app, nationalDex, decoded.abilityNum),
            gender = SpeciesCatalog.genderFor(app, nationalDex, decoded.personality),
            catchRate = SpeciesCatalog.catchRateFor(app, nationalDex),
            heldItemName = decoded.heldItem.takeIf { it != 0 }?.let { SpeciesCatalog.itemNameFor(app, it) },
            moves = decoded.moves.mapNotNull { SpeciesCatalog.moveNameFor(app, it) },
            ivs = decoded.ivs,
            evs = decoded.evs,
            friendship = decoded.friendship,
            hp = decoded.currentHp,
            maxHp = decoded.maxHp,
            attack = decoded.attack,
            defense = decoded.defense,
            speed = decoded.speed,
            spAttack = decoded.spAttack,
            spDefense = decoded.spDefense
        )
    }

    fun disconnect() {
        refreshJob?.cancel()
        refreshJob = null
        client.close()
        addLog("Conexión cerrada por el usuario")
        _state.value = _state.value.copy(
            connected = false,
            selectedTab = CompanionTab.TEAM,
            message = "Desconectado · datos guardados",
            battle = null,
            team = emptyList()
        )
    }
}