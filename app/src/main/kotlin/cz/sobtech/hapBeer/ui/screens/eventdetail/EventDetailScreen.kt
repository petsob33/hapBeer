package cz.sobtech.hapBeer.ui.screens.eventdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.sobtech.hapBeer.HapBeerApp
import cz.sobtech.hapBeer.R
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val czFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val czFmt0 = NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ")).apply {
    maximumFractionDigits = 0
}

private fun fmtPrice(price: Double) = "${czFmt.format(price)} Kč"
private fun fmtPriceInt(price: Double) = "${czFmt0.format(price.roundToInt())} Kč"
private fun fmtPricePerLiter(price: Double, sizeLiters: Double) =
    "${czFmt.format(price / sizeLiters)} Kč/l"

// ═════════════════════════════════════════════════════════════════════════════
// EventDetailScreen
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    onBack: () -> Unit,
    onKegClick: (kegId: Long) -> Unit,
    viewModel: EventDetailViewModel = viewModel(
        factory = EventDetailViewModel.provideFactory(
            repository = (LocalContext.current.applicationContext as HapBeerApp).appContainer.pivoRepository,
            eventId = eventId
        )
    )
) {
    val event by viewModel.event.collectAsState()
    val kegs by viewModel.kegs.collectAsState()
    val allPeople by viewModel.allPeople.collectAsState()
    val beerCounts by viewModel.beerCounts.collectAsState()

    var showAddKegDialog by remember { mutableStateOf(false) }
    var kegToDelete by remember { mutableStateOf<KegEntity?>(null) }
    var showSummary by remember { mutableStateOf(false) }

    // ── Dialogy ──────────────────────────────────────────────────────────────

    if (showAddKegDialog) {
        AddKegDialog(
            onDismiss = { showAddKegDialog = false },
            onConfirm = { name, price, sizeLiters ->
                viewModel.addKeg(name, price, sizeLiters)
                showAddKegDialog = false
            }
        )
    }

    kegToDelete?.let { keg ->
        DeleteKegDialog(
            kegName = keg.name,
            onDismiss = { kegToDelete = null },
            onConfirm = { viewModel.deleteKeg(keg); kegToDelete = null }
        )
    }

    // ── Souhrn akce ──────────────────────────────────────────────────────────

    if (showSummary) {
        val peopleThatDrank = allPeople.filter { (beerCounts[it.id] ?: 0) > 0 }
        EventSummaryBottomSheet(
            eventName = event?.name ?: "",
            people = peopleThatDrank,
            beerCounts = beerCounts,
            kegs = kegs,
            onDismiss = { showSummary = false }
        )
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = { showSummary = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Souhrn akce")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddKegDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Přidat bečku")
            }
        }
    ) { innerPadding ->
        if (kegs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_beer_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Žádné bečky.\nPřidej první bečku!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(kegs, key = { it.id }) { keg ->
                    SwipeableKegCard(
                        keg = keg,
                        onClick = { onKegClick(keg.id) },
                        onDeleteRequest = { kegToDelete = keg }
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Karta bečky se swipe-to-delete
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableKegCard(keg: KegEntity, onClick: () -> Unit, onDeleteRequest: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
            false
        },
        positionalThreshold = { it * 0.4f }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.shapes.medium
                    )
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Smazat",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = keg.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(fmtPrice(keg.price), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${czFmt.format(keg.sizeLiters)} l",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        fmtPricePerLiter(keg.price, keg.sizeLiters),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Souhrn akce – bottom sheet
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventSummaryBottomSheet(
    eventName: String,
    people: List<PersonEntity>,
    beerCounts: Map<Long, Int>,
    kegs: List<KegEntity>,
    onDismiss: () -> Unit
) {
    val totalCost = kegs.sumOf { it.price }
    val totalBeers = beerCounts.values.sum()

    val leaderboard = people
        .map { person ->
            val count = beerCounts[person.id] ?: 0
            val cost = if (totalBeers > 0)
                (count.toDouble() / totalBeers * totalCost).roundToInt()
            else 0
            Triple(person, count, cost)
        }
        .sortedByDescending { it.second }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Souhrn – $eventName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item { HorizontalDivider() }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryCell("Celkem piv", "$totalBeers")
                    SummaryCell("Útrata za bečky", fmtPriceInt(totalCost), alignEnd = true)
                }
            }

            if (leaderboard.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text("Žebříček a útrata", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Útrata je počítána podle podílu vypitých piv.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(leaderboard.size) { index ->
                    val (person, count, cost) = leaderboard[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.widthIn(min = 26.dp)
                            )
                            Text(person.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$count piv",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$cost Kč",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.widthIn(min = 76.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Dialogy
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddKegDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, sizeLiters: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("") }

    val priceValue = priceText.replace(",", ".").toDoubleOrNull()
    val sizeValue = sizeText.replace(",", ".").toDoubleOrNull()
    val priceError = priceText.isNotEmpty() && (priceValue == null || priceValue <= 0)
    val sizeError = sizeText.isNotEmpty() && (sizeValue == null || sizeValue <= 0)
    val isValid = name.isNotBlank() &&
            priceValue != null && priceValue > 0 &&
            sizeValue != null && sizeValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nová bečka") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Název bečky") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText, onValueChange = { priceText = it },
                    label = { Text("Cena v Kč") }, singleLine = true,
                    isError = priceError,
                    supportingText = if (priceError) ({ Text("Zadej kladné číslo") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sizeText, onValueChange = { sizeText = it },
                    label = { Text("Objem v litrech") }, singleLine = true,
                    isError = sizeError,
                    supportingText = if (sizeError) ({ Text("Zadej kladné číslo") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), priceValue!!, sizeValue!!) },
                enabled = isValid
            ) { Text("Přidat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } }
    )
}

@Composable
private fun DeleteKegDialog(kegName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smazat bečku") },
        text = { Text("Opravdu smazat bečku \"$kegName\" a všechna piva z ní?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Smazat", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } }
    )
}
