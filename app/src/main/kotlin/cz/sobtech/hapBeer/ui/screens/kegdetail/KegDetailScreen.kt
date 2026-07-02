package cz.sobtech.hapBeer.ui.screens.kegdetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.sobtech.hapBeer.HapBeerApp
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private const val BEER_VOLUME_LITERS = 0.5

private val czFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private fun fmtPrice(price: Double): String {
    val fmt0 = NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ")).apply {
        maximumFractionDigits = 0
    }
    return "${fmt0.format(price.roundToInt())} Kč"
}

private fun fmtPricePerLiter(price: Double, sizeLiters: Double) =
    "${czFmt.format(price / sizeLiters)} Kč/l"

// ═════════════════════════════════════════════════════════════════════════════
// KegDetailScreen
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KegDetailScreen(
    kegId: Long,
    onBack: () -> Unit,
    onNavigateToPeople: () -> Unit,
    viewModel: KegDetailViewModel = viewModel(
        factory = KegDetailViewModel.provideFactory(
            repository = (LocalContext.current.applicationContext as HapBeerApp).appContainer.pivoRepository,
            kegId = kegId
        )
    )
) {
    val keg by viewModel.keg.collectAsState()
    val allPeople by viewModel.allPeople.collectAsState()
    val beerCountsByPerson by viewModel.beerCountsByPerson.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Počítání naplnění ─────────────────────────────────────────────────────
    val totalBeers = beerCountsByPerson.values.sum()
    val consumed = totalBeers * BEER_VOLUME_LITERS
    val sizeLiters = keg?.sizeLiters ?: 1.0
    val remaining = (sizeLiters - consumed).coerceAtLeast(0.0)
    val fraction = (remaining / sizeLiters).toFloat().coerceIn(0f, 1f)
    val percentInt = (fraction * 100).roundToInt()

    val progressColor = when {
        fraction > 0.5f -> Color(0xFF388E3C) // zelená – bečka plná
        fraction > 0.2f -> Color(0xFFF57C00) // oranžová – ubývá
        else -> MaterialTheme.colorScheme.error // červená – skoro prázdná
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(keg?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Grafický ukazatel naplnění ────────────────────────────────────
            keg?.let { k ->
                KegProgressHeader(
                    keg = k,
                    remaining = remaining,
                    fraction = fraction,
                    percentInt = percentInt,
                    progressColor = progressColor
                )
            }

            HorizontalDivider()

            // ── Seznam lidí ───────────────────────────────────────────────────
            if (allPeople.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Zatím žádní lidé.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onNavigateToPeople) {
                            Text("Přidat lidi")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allPeople, key = { it.id }) { person ->
                        val beerCount = beerCountsByPerson[person.id] ?: 0
                        PersonKegCard(
                            person = person,
                            beerCount = beerCount,
                            onRecordBeer = {
                                val newCount = beerCount + 1
                                viewModel.recordBeer(person.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "${person.name}: $newCount. pivo zapsáno",
                                        actionLabel = "Zpět",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoLastBeer(person.id)
                                    }
                                }
                            },
                            onUndoBeer = { viewModel.undoLastBeer(person.id) }
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Ukazatel naplnění
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KegProgressHeader(
    keg: KegEntity,
    remaining: Double,
    fraction: Float,
    percentInt: Int,
    progressColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cena, objem, cena/l
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${czFmt.format(keg.sizeLiters)} l",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                fmtPrice(keg.price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                fmtPricePerLiter(keg.price, keg.sizeLiters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // "Zbývá X,XX l z Y,YY l (Z %)"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Zbývá ${czFmt.format(remaining)} l z ${czFmt.format(keg.sizeLiters)} l",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$percentInt %",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Prázdná bečka
        if (fraction == 0f) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Bečka je prázdná",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Karta osoby v detailu bečky
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PersonKegCard(
    person: PersonEntity,
    beerCount: Int,
    onRecordBeer: () -> Unit,
    onUndoBeer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onUndoBeer,
                enabled = beerCount > 0,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    "−",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = beerCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp)
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onRecordBeer,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("+ Pivo")
            }
        }
    }
}
