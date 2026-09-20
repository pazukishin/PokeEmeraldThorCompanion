package com.thorcompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.thorcompanion.CompanionViewModel
import com.thorcompanion.CompanionTab
import com.thorcompanion.CategorizedEncounter
import com.thorcompanion.ConnectionLog
import com.thorcompanion.Encounter
import com.thorcompanion.EncounterCategory
import com.thorcompanion.DetailState
import com.thorcompanion.MapItem
import com.thorcompanion.PokemonSprite
import com.thorcompanion.R
import com.thorcompanion.Trainer
import com.thorcompanion.TrainerPokemon
import com.thorcompanion.TrainerSprite
import com.thorcompanion.TradeOffer
import com.thorcompanion.ItemSprite

@Composable
fun CompanionScreen(viewModel: CompanionViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    Box(modifier) {
        Column(modifier.background(MaterialTheme.colorScheme.background).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("EMERALD COMPANION", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("v${stringResource(R.string.thor_companion_version_name)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    if (state.connected) {
                        Text(state.mapName, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                StatusDot(state.connected)
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (state.connected) viewModel.disconnect() else viewModel.connect() },
                        contentPadding = ButtonDefaults.ContentPadding,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF171A1D))
                    ) { Text(if (state.connected) "Cortar" else "Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = viewModel::toggleLogs,
                        contentPadding = ButtonDefaults.ContentPadding,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color(0xFF171A1D))
                    ) { Text("Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(5.dp))
            if (state.connected) {
                TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                    Tab(
                        selected = state.selectedTab == CompanionTab.ENCOUNTERS,
                        onClick = { viewModel.selectTab(CompanionTab.ENCOUNTERS) },
                        text = { Text("Encuentros", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = state.selectedTab == CompanionTab.ITEMS,
                        onClick = { viewModel.selectTab(CompanionTab.ITEMS) },
                        text = { Text("Objetos", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = state.selectedTab == CompanionTab.TRAINERS,
                        onClick = { viewModel.selectTab(CompanionTab.TRAINERS) },
                        text = { Text("Entrenadores", fontSize = 11.sp) }
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.host,
                        onValueChange = viewModel::updateHost,
                        label = { Text("Host") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.port,
                        onValueChange = viewModel::updatePort,
                        label = { Text("Puerto") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column((if (state.connected) Modifier.weight(0.72f) else Modifier.fillMaxWidth()).fillMaxHeight()) {
                    when (state.selectedTab) {
                        CompanionTab.ENCOUNTERS -> {
                            Text("ENCUENTROS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                EncounterCategory.values().forEach { category ->
                                    val encounters = state.categorizedEncounters[category].orEmpty()
                                    if (encounters.isNotEmpty()) {
                                        item { CategoryHeader(category) }
                                        items(encounters) { EncounterRow(it) { viewModel.showPokemonDetails(Encounter(it.name, it.type, it.rate, it.level, it.spriteId)) } }
                                    }
                                }
                                if (state.trades.isNotEmpty()) {
                                    item { Text("INTERCAMBIOS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                                    items(state.trades) { TradeRow(it) }
                                }
                                if (state.categorizedEncounters.values.all { it.isEmpty() }) item { EmptyTab("Sin encuentros catalogados") }
                            }
                        }
                        CompanionTab.ITEMS -> {
                            Text("OBJETOS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (state.items.isEmpty()) item { EmptyTab("No hay objetos catalogados") }
                                else items(state.items) { ItemRow(it) { viewModel.showItemDetails(it) } }
                            }
                        }
                        CompanionTab.TRAINERS -> {
                            Text("ENTRENADORES", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (state.trainers.isEmpty()) item { EmptyTab("No hay entrenadores catalogados") }
                                else items(state.trainers) { TrainerRow(it) { viewModel.showTrainerDetails(it) } }
                            }
                        }
                    }
                }
                if (state.connected) Column(Modifier.weight(0.28f).fillMaxHeight()) {
                    Text("MAPA", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surface).padding(9.dp)) {
                        Column {
                            Text(state.mapName, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(state.mapId, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("Fuente", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("PokéAPI + catálogo Emerald", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        state.detail?.let { DetailDialog(it, viewModel::closeDetails) }

        if (state.showLogs) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LOGS DE CONEXIÓN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = viewModel::closeLogs,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                        ) {
                            Text("Cerrar")
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    if (state.logs.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("Sin eventos aún. Pulsa Conectar para iniciar la prueba.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.logs.reversed()) { log -> LogRow(log) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncounterRow(encounter: Encounter, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(MaterialTheme.colorScheme.surface).padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = PokemonSprite.urlFor(encounter),
                contentDescription = encounter.name,
                modifier = Modifier.width(32.dp).height(32.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(encounter.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${encounter.type}  ·  ${encounter.level}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${encounter.rate}%", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LogRow(log: ConnectionLog) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        Text(log.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
        Text(log.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StatusDot(connected: Boolean) {
    Box(Modifier.width(9.dp).height(9.dp).background(if (connected) Color(0xFF65D58A) else Color(0xFF67727A)))
}

@Composable
private fun ItemRow(item: MapItem, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(MaterialTheme.colorScheme.surface).padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = ItemSprite.urlFor(item), contentDescription = item.name, modifier = Modifier.width(32.dp).height(32.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
        Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("x${item.quantity}", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrainerRow(trainer: Trainer, onClick: () -> Unit = {}) {
    val background = if (trainer.isSpecial) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).background(background, RoundedCornerShape(6.dp)).padding(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = TrainerSprite.urlFor(trainer), contentDescription = trainer.name, modifier = Modifier.width(36.dp).height(36.dp))
            Spacer(Modifier.width(7.dp))
            Text(trainer.name, color = if (trainer.isSpecial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        trainer.pokemon.forEach { pokemon -> TrainerPokemonRow(pokemon) }
    }
}

@Composable
private fun TrainerPokemonRow(pokemon: TrainerPokemon) {
    Text("${pokemon.name} · Nv. ${pokemon.level}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmptyTab(message: String) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(9.dp))
}

@Composable
private fun EncounterRow(encounter: CategorizedEncounter, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(MaterialTheme.colorScheme.surface).padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.width(32.dp).height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = PokemonSprite.urlFor(Encounter(encounter.name, encounter.type, encounter.rate, encounter.level, encounter.spriteId)),
                contentDescription = encounter.name,
                modifier = Modifier.width(32.dp).height(32.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(encounter.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${encounter.type}  ·  ${encounter.level}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${encounter.rate}%", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryHeader(category: EncounterCategory) {
    Text(
        when (category) {
            EncounterCategory.GRASS -> "HIERBA"
            EncounterCategory.FISHING -> "PESCA"
            EncounterCategory.SURF -> "SURF"
        },
        color = MaterialTheme.colorScheme.primary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
    )
}

@Composable
private fun DetailDialog(detail: DetailState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = {
            Text(when (detail) {
                is DetailState.Pokemon -> detail.value.name
                is DetailState.Item -> detail.value.name
                is DetailState.Trainer -> detail.value.name
            })
        },
        text = {
            when (detail) {
                is DetailState.Pokemon -> PokemonDetailContent(detail.value)
                is DetailState.Item -> ItemDetailContent(detail.value)
                is DetailState.Trainer -> TrainerDetailContent(detail.value)
            }
        }
    )
}

@Composable
private fun PokemonDetailContent(details: com.thorcompanion.PokemonDetails) {
    val shiny = remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        AsyncImage(
            model = if (shiny.value) details.shinySpriteUrl ?: details.normalSpriteUrl else details.normalSpriteUrl,
            contentDescription = details.name,
            modifier = Modifier.width(120.dp).height(120.dp).clickable { shiny.value = !shiny.value }
        )
        Text("Tipos: ${details.types.joinToString()}")
        Text("Habilidades: ${details.abilities.joinToString()}")
        Text("Evoluciones: ${details.evolutions.joinToString(" -> ")}")
        if (details.description.isNotBlank()) Text(details.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun ItemDetailContent(details: com.thorcompanion.ItemDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AsyncImage(model = details.spriteUrl, contentDescription = details.name, modifier = Modifier.width(96.dp).height(96.dp))
        if (details.description.isNotBlank()) Text(details.description)
        if (details.effect.isNotBlank()) Text(details.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun TrainerDetailContent(trainer: Trainer) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    AsyncImage(model = TrainerSprite.urlFor(trainer), contentDescription = trainer.name, modifier = Modifier.width(120.dp).height(120.dp))
        trainer.pokemon.forEach { pokemon ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = PokemonSprite.urlForName(pokemon.name), contentDescription = pokemon.name, modifier = Modifier.width(32.dp).height(32.dp))
                Text("${pokemon.name} · Nv. ${pokemon.level}", fontSize = 12.sp)
            }
        }
        if (trainer.pokemon.isEmpty()) Text("Equipo no disponible para este combate.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TradeRow(trade: TradeOffer) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)).padding(9.dp)) {
        Text("Recibes: ${trade.gives}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("Entregas: ${trade.requests}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}