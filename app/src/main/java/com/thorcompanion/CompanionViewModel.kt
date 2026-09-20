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
enum class CompanionTab { ENCOUNTERS, ITEMS, TRAINERS }
data class PokemonDetails(
    val name: String,
    val types: List<String>,
    val abilities: List<String>,
    val evolutions: List<String>,
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

data class CompanionState(
    val connected: Boolean = false,
    val host: String = "127.0.0.1",
    val port: String = "55355",
    val mapName: String = "Ruta 101",
    val mapId: String = "MAP_ROUTE_101",
    val encounters: List<Encounter> = EncounterCatalog.route101,
    val categorizedEncounters: Map<EncounterCategory, List<CategorizedEncounter>> = EncounterCatalog.groupedFor("MAP_ROUTE_101"),
    val items: List<MapItem> = EmeraldMapCatalog.itemsFor("MAP_ROUTE_101"),
    val trainers: List<Trainer> = EmeraldMapCatalog.trainersFor("MAP_ROUTE_101"),
    val trades: List<TradeOffer> = emptyList(),
    val selectedTab: CompanionTab = CompanionTab.ENCOUNTERS,
    val detail: DetailState? = null,
    val detailLoading: Boolean = false,
    val message: String = "Sin conexión · mostrando datos guardados",
    val logs: List<ConnectionLog> = emptyList(),
    val showLogs: Boolean = false
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
                        trades = mapData?.trades ?: emptyList(),
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

    fun disconnect() {
        refreshJob?.cancel()
        refreshJob = null
        client.close()
        addLog("Conexión cerrada por el usuario")
        _state.value = _state.value.copy(
            connected = false,
            selectedTab = CompanionTab.ENCOUNTERS,
            message = "Desconectado · datos guardados"
        )
    }
}