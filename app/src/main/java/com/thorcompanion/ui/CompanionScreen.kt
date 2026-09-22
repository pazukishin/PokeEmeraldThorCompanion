package com.thorcompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.thorcompanion.BattleInfo
import com.thorcompanion.CompanionViewModel
import com.thorcompanion.CompanionTab
import com.thorcompanion.PokemonDecoder
import com.thorcompanion.PokemonInfo
import com.thorcompanion.CategorizedEncounter
import com.thorcompanion.ConnectionLog
import com.thorcompanion.Encounter
import com.thorcompanion.EncounterCategory
import com.thorcompanion.EvolutionNode
import com.thorcompanion.Gender
import com.thorcompanion.DetailState
import com.thorcompanion.MapItem
import com.thorcompanion.PokemonSprite
import com.thorcompanion.R
import com.thorcompanion.SpeciesCatalog
import com.thorcompanion.Trainer
import com.thorcompanion.TrainerPokemon
import com.thorcompanion.TrainerSprite
import com.thorcompanion.TradeOffer
import com.thorcompanion.ItemSprite

@Composable
fun CompanionScreen(viewModel: CompanionViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val selectedTeamMon = remember { mutableStateOf<PokemonInfo?>(null) }
    val teamEvolution = remember { mutableStateOf<EvolutionNode?>(null) }
    LaunchedEffect(selectedTeamMon.value?.speciesId) {
        val speciesId = selectedTeamMon.value?.speciesId
        teamEvolution.value = null
        teamEvolution.value = if (speciesId != null) viewModel.loadEvolutionForSpecies(speciesId) else null
    }
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
                    ) { Text(if (state.connected) "Disconnect" else "Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
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
                        selected = state.selectedTab == CompanionTab.TEAM,
                        onClick = { viewModel.selectTab(CompanionTab.TEAM) },
                        text = { Text("Team", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = state.selectedTab == CompanionTab.ENCOUNTERS,
                        onClick = { viewModel.selectTab(CompanionTab.ENCOUNTERS) },
                        text = { Text("Encounters", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = state.selectedTab == CompanionTab.ITEMS,
                        onClick = { viewModel.selectTab(CompanionTab.ITEMS) },
                        text = { Text("Items", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = state.selectedTab == CompanionTab.TRAINERS,
                        onClick = { viewModel.selectTab(CompanionTab.TRAINERS) },
                        text = { Text("Trainers", fontSize = 11.sp) }
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
                        label = { Text("Port") },
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
                    if (state.connected) {
                        when (state.selectedTab) {
                            CompanionTab.TEAM -> {
                                Text("TEAM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                TeamGrid(state.team, onSelect = { selectedTeamMon.value = it })
                            }
                            CompanionTab.ENCOUNTERS -> {
                                Text("ENCOUNTERS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                        item { Text("TRADES", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                                        items(state.trades) { TradeRow(it) }
                                    }
                                    if (state.categorizedEncounters.values.all { it.isEmpty() }) item { EmptyTab("No encounters catalogued") }
                                }
                            }
                            CompanionTab.ITEMS -> {
                                Text("ITEMS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (state.items.isEmpty()) item { EmptyTab("No items catalogued") }
                                    else items(state.items) { ItemRow(it) { viewModel.showItemDetails(it) } }
                                }
                            }
                            CompanionTab.TRAINERS -> {
                                Text("TRAINERS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (state.trainers.isEmpty()) item { EmptyTab("No trainers catalogued") }
                                    else items(state.trainers.sortedByDescending { it.isSpecial }) { TrainerRow(it) { viewModel.showTrainerDetails(it) } }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Connect to RetroArch to see live game info",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (state.connected) Column(Modifier.weight(0.28f).fillMaxHeight()) {
                    Text("MAP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surface).padding(9.dp)) {
                        Column {
                            Text(state.mapName, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(state.mapId, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("Source", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Emerald catalog (offline)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        state.detail?.let { DetailDialog(it, viewModel::closeDetails) }

        state.battle?.let { battle ->
            if (battle.isTrainer) TrainerBattleOverlay(battle) else WildBattleOverlay(battle)
        }

        selectedTeamMon.value?.let { mon ->
            PokemonDetailOverlay(mon, showStats = false, showMoves = false, evolution = teamEvolution.value, onDismiss = { selectedTeamMon.value = null })
        }

        if (state.showLogs) {
            Box(modifier = overlayScrim(Color(0xFF000000))) {
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
                        Text("CONNECTION LOGS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = viewModel::closeLogs,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                        ) {
                            Text("Close")
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    if (state.logs.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No events yet. Tap Connect to start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(encounter.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
        Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
    Text("${pokemon.name} · Lv. ${pokemon.level}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(encounter.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${encounter.type}  ·  ${encounter.level}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${encounter.rate}%", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryHeader(category: EncounterCategory) {
    Text(
        when (category) {
            EncounterCategory.GRASS -> "GRASS"
            EncounterCategory.FISHING -> "FISHING"
            EncounterCategory.SURF -> "SURF"
        },
        color = MaterialTheme.colorScheme.primary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
    )
}

@Composable
private fun overlayScrim(color: Color): Modifier = Modifier
    .fillMaxSize()
    .background(color)
    .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {}
    )

@Composable
private fun DetailPanel(
    title: String,
    spriteUrl: String?,
    onDismiss: () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    onSpriteClick: (() -> Unit)? = null,
    info: @Composable () -> Unit
) {
    Box(overlayScrim(Color(0xFF101418))) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss) { Text("Close") }
            }
            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val spriteModifier = if (onSpriteClick != null) Modifier.size(96.dp).clickable(onClick = onSpriteClick) else Modifier.size(96.dp)
                    AsyncImage(model = spriteUrl, contentDescription = title, modifier = spriteModifier)
                    Spacer(Modifier.height(6.dp))
                    subtitle?.invoke()
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    info()
                }
            }
        }
    }
}

@Composable
private fun DetailDialog(detail: DetailState, onDismiss: () -> Unit) {
    when (detail) {
        is DetailState.Pokemon -> PokemonDetailsPanel(detail.value, onDismiss)
        is DetailState.Item -> ItemDetailsPanel(detail.value, onDismiss)
        is DetailState.Trainer -> TrainerDetailsPanel(detail.value, onDismiss)
    }
}

@Composable
private fun PokemonDetailsPanel(details: com.thorcompanion.PokemonDetails, onDismiss: () -> Unit) {
    val context = LocalContext.current
    DetailPanel(
        title = details.name,
        spriteUrl = details.normalSpriteUrl,
        onDismiss = onDismiss
    ) {
        if (details.types.isNotEmpty()) {
            InfoSection("TYPES") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.types.forEach { TypeBadge(it) }
                }
            }
        }
        if (details.abilities.isNotEmpty()) {
            InfoSection("ABILITIES") {
                details.abilities.forEach { ability ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(9.dp)
                    ) {
                        Text(ability, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        SpeciesCatalog.abilityDescriptionFor(context, ability)?.let { description ->
                            Spacer(Modifier.height(2.dp))
                            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
        if (details.evolution != null && details.evolution.branches.isNotEmpty()) {
            InfoSection("EVOLUTIONS") {
                EvolutionNodeView(details.evolution)
            }
        }
        if (details.description.isNotBlank()) {
            InfoSection("DESCRIPTION") {
                Text(details.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ItemDetailsPanel(details: com.thorcompanion.ItemDetails, onDismiss: () -> Unit) {
    DetailPanel(title = details.name, spriteUrl = details.spriteUrl, onDismiss = onDismiss) {
        if (details.description.isNotBlank()) {
            InfoSection("DESCRIPTION") {
                Text(details.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (details.effect.isNotBlank()) {
            InfoSection("EFFECT") {
                Text(details.effect, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrainerDetailsPanel(trainer: Trainer, onDismiss: () -> Unit) {
    DetailPanel(title = trainer.name, spriteUrl = TrainerSprite.urlFor(trainer), onDismiss = onDismiss) {
        if (trainer.pokemon.isEmpty()) {
            Text("Team unavailable for this battle.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        } else {
            InfoSection("TEAM") {
                trainer.pokemon.forEach { pokemon ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        AsyncImage(model = PokemonSprite.urlForName(pokemon.name), contentDescription = pokemon.name, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(pokemon.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("Lv. ${pokemon.level}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeRow(trade: TradeOffer) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)).padding(9.dp)) {
        Text("You receive: ${trade.gives}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("You give: ${trade.requests}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WildBattleOverlay(battle: BattleInfo) {
    Box(overlayScrim(Color(0xFF101418))) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(Modifier.height(8.dp))
            Text("WILD BATTLE", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = PokemonSprite.urlForId(battle.opponent.speciesId),
                        contentDescription = battle.opponent.name,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(battle.opponent.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Lv. ${battle.opponent.level}", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        GenderIcon(battle.opponent.gender)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    PokemonInfoContent(battle.opponent, showEvs = false, showStats = false, showMoves = false, showBattle = true)
                }
            }
        }
    }
}

@Composable
private fun TrainerBattleOverlay(battle: BattleInfo) {
    val selected = remember { mutableStateOf<PokemonInfo?>(null) }
    val activeIndex = battle.team.indexOfFirst { it.personality == battle.opponent.personality }
    Box(overlayScrim(Color(0xFF101418))) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                battle.trainerSprite?.let { sprite ->
                    AsyncImage(model = sprite, contentDescription = battle.trainerName, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    battle.trainerName ?: "TRAINER BATTLE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            TeamGrid(battle.team, onSelect = { selected.value = it }, large = true, activeIndex = activeIndex)
        }
    }
    selected.value?.let { mon ->
        PokemonDetailOverlay(mon, showStats = true, showMoves = true, onDismiss = { selected.value = null })
    }
}

@Composable
private fun PokemonDetailOverlay(info: PokemonInfo, showStats: Boolean, showMoves: Boolean, evolution: EvolutionNode? = null, onDismiss: () -> Unit) {
    DetailPanel(
        title = info.name,
        spriteUrl = PokemonSprite.urlForId(info.speciesId),
        onDismiss = onDismiss,
        subtitle = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lv. ${info.level}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                GenderIcon(info.gender)
            }
        }
    ) {
        PokemonInfoContent(info, showEvs = true, showStats = showStats, showMoves = showMoves, showBattle = false, evolution = evolution)
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(
            title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun TypeBadge(type: String) {
    Box(
        Modifier
            .background(typeColor(type), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(type, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun typeColor(type: String): Color = when (type.lowercase()) {
    "normal" -> Color(0xFFA8A878)
    "fire" -> Color(0xFFF08030)
    "water" -> Color(0xFF6890F0)
    "electric" -> Color(0xFFF8D030)
    "grass" -> Color(0xFF78C850)
    "ice" -> Color(0xFF98D8D8)
    "fighting" -> Color(0xFFC03028)
    "poison" -> Color(0xFFA040A0)
    "ground" -> Color(0xFFE0C068)
    "flying" -> Color(0xFFA890F0)
    "psychic" -> Color(0xFFF85888)
    "bug" -> Color(0xFFA8B820)
    "rock" -> Color(0xFFB8A038)
    "ghost" -> Color(0xFF705898)
    "dragon" -> Color(0xFF7038F8)
    "dark" -> Color(0xFF705848)
    "steel" -> Color(0xFFB8B8D0)
    "fairy" -> Color(0xFFEE99AC)
    else -> Color(0xFF67727A)
}

private data class StatEntry(val label: String, val value: String, val highlighted: Boolean = false)

@Composable
private fun StatGrid(entries: List<StatEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { entry ->
                    Row(Modifier.weight(1f)) {
                        Text(entry.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
                        Text(
                            entry.value,
                            fontSize = 12.sp,
                            color = if (entry.highlighted) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (entry.highlighted) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PokemonInfoContent(info: PokemonInfo, showEvs: Boolean, showStats: Boolean, showMoves: Boolean, showBattle: Boolean, evolution: EvolutionNode? = null) {
    val context = LocalContext.current
    InfoSection("NATURE") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(info.nature, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(info.natureModifier.ifBlank { "Neutra" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
    info.ability?.let { ability ->
        InfoSection("ABILITY") {
            Text(ability, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            SpeciesCatalog.abilityDescriptionFor(context, ability)?.let { description ->
                Spacer(Modifier.height(2.dp))
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (evolution != null && evolution.branches.isNotEmpty()) {
        InfoSection("EVOLUCIONES") {
            EvolutionNodeView(evolution)
        }
    }
    info.heldItemName?.let { heldItem ->
        InfoSection("HELD ITEM") {
            Text(heldItem, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    if (showBattle && info.catchRate > 0) {
        InfoSection("BATTLE") {
            Text("Catch rate: ${info.catchRate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    if (showStats) {
        InfoSection("STATS") {
            StatGrid(
                listOf(
                    StatEntry("HP", info.maxHp?.let { "${info.hp ?: 0} / $it" } ?: "–"),
                    StatEntry("Atk", info.attack?.toString() ?: "–"),
                    StatEntry("Def", info.defense?.toString() ?: "–"),
                    StatEntry("Spe", info.speed?.toString() ?: "–"),
                    StatEntry("SpA", info.spAttack?.toString() ?: "–"),
                    StatEntry("SpD", info.spDefense?.toString() ?: "–")
                )
            )
        }
    }
    InfoSection("IVs") {
        IvSpreadRow(info.ivs)
    }
    if (showEvs) {
        info.evs?.let { evs ->
            InfoSection("EVs") { IvSpreadRow(evs) }
        }
        info.friendship?.let { friendship ->
            InfoSection("FRIENDSHIP") { Text("$friendship", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) }
        }
        InfoSection("HIDDEN POWER") { Text(info.hiddenPowerType, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) }
    }
    if (showMoves && info.moves.isNotEmpty()) {
        InfoSection("MOVES") {
            info.moves.forEach { move ->
                Text("• $move", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun EvolutionNodeView(node: EvolutionNode, depth: Int = 0) {
    val indent = (depth * 14).dp
    Text(
        node.species,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = indent)
    )
    node.branches.forEach { branch ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = indent, top = 1.dp)
        ) {
            Text("↳", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            Spacer(Modifier.width(5.dp))
            Text(branch.condition.ifBlank { "Evolves" }, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        EvolutionNodeView(branch.target, depth + 1)
    }
}

@Composable
private fun GenderIcon(gender: Gender) {
    val color = when (gender) {
        Gender.FEMALE -> Color(0xFFF48FB1)
        Gender.MALE -> Color(0xFF64B5F6)
        Gender.GENDERLESS -> null
    }
    val symbol = when (gender) {
        Gender.FEMALE -> "♀"
        Gender.MALE -> "♂"
        Gender.GENDERLESS -> null
    }
    if (color != null && symbol != null) {
        Box(Modifier.size(18.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private fun statusCode(status: Int): String? = when {
    (status and 0x07) != 0 -> "SLP"      // sleep (3-bit turn counter)
    (status and 0x80) != 0 -> "TOX"      // badly poisoned
    (status and 0x08) != 0 -> "PSN"      // poisoned
    (status and 0x10) != 0 -> "BRN"      // burned
    (status and 0x20) != 0 -> "FRZ"      // frozen
    (status and 0x40) != 0 -> "PAR"      // paralyzed
    else -> null
}

private fun statusColor(code: String): Color = when (code) {
    "SLP" -> Color(0xFF5C6BC0)
    "TOX" -> Color(0xFF7B1FA2)
    "PSN" -> Color(0xFF8E24AA)
    "BRN" -> Color(0xFFE53935)
    "FRZ" -> Color(0xFF0288D1)
    "PAR" -> Color(0xFFF9A825)
    else -> Color(0xFF90A4AE)
}

@Composable
private fun TeamGrid(team: List<PokemonInfo>, onSelect: (PokemonInfo) -> Unit, large: Boolean = false, activeIndex: Int = -1) {
    val slots = (0 until 6).map { team.getOrNull(it) }
    val gap = if (large) 8.dp else 6.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxSize()) {
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.weight(1f)) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    val mon = slots[index]
                    TeamSlot(mon, onClick = { mon?.let(onSelect) }, large = large, isActive = index == activeIndex, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TeamSlot(mon: PokemonInfo?, onClick: () -> Unit, modifier: Modifier = Modifier, large: Boolean = false, isActive: Boolean = false) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .then(if (isActive) Modifier.border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(10.dp)) else Modifier)
            .clickable(enabled = mon != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (mon != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val spriteSize = if (large) 88.dp else 44.dp
                Box(Modifier.size(spriteSize)) {
                    AsyncImage(
                        model = PokemonSprite.urlForId(mon.speciesId),
                        contentDescription = mon.name,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (mon.heldItemName != null) {
                        AsyncImage(
                            model = "file:///android_asset/items/poke-ball.png",
                            contentDescription = "Held item",
                            modifier = Modifier.size(if (large) 24.dp else 14.dp).align(Alignment.BottomEnd)
                        )
                    }
                }
                Text(mon.name, fontSize = if (large) 14.sp else 10.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text("Lv. ${mon.level}", fontSize = if (large) 13.sp else 10.sp, color = MaterialTheme.colorScheme.primary)
                val status = statusCode(mon.status)
                if (status != null || mon.maxHp != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (status != null) {
                            Box(
                                Modifier
                                    .background(statusColor(status), RoundedCornerShape(3.dp))
                                    .padding(horizontal = if (large) 4.dp else 3.dp, vertical = if (large) 1.dp else 0.dp)
                            ) {
                                Text(status, fontSize = if (large) 9.sp else 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(3.dp))
                        }
                        mon.maxHp?.let { maxHp ->
                            val current = mon.hp ?: 0
                            val hpColor = when {
                                current * 100 / maxOf(maxHp, 1) > 50 -> Color(0xFF65D58A)
                                current * 100 / maxOf(maxHp, 1) > 20 -> Color(0xFFFFD54F)
                                else -> Color(0xFFE57373)
                            }
                            Text("HP $current/$maxHp", fontSize = if (large) 12.sp else 9.sp, color = hpColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IvSpreadRow(ivs: PokemonDecoder.IvSpread) {
    StatGrid(
        listOf(
            StatEntry("HP", ivs.hp.toString(), ivs.hp == 31),
            StatEntry("Atk", ivs.attack.toString(), ivs.attack == 31),
            StatEntry("Def", ivs.defense.toString(), ivs.defense == 31),
            StatEntry("Spe", ivs.speed.toString(), ivs.speed == 31),
            StatEntry("SpA", ivs.spAttack.toString(), ivs.spAttack == 31),
            StatEntry("SpD", ivs.spDefense.toString(), ivs.spDefense == 31)
        )
    )
}