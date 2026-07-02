package cz.sobtech.hapBeer.ui.screens.kegdetail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.sobtech.hapBeer.HapBeerApp
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import cz.sobtech.hapBeer.ui.util.FunnyMessages
import cz.sobtech.hapBeer.ui.util.KegConsumptionStats
import cz.sobtech.hapBeer.ui.util.LITERS_PER_BEER
import cz.sobtech.hapBeer.ui.util.PersonDetailDialog
import cz.sobtech.hapBeer.ui.util.computePersonEventStats
import cz.sobtech.hapBeer.ui.util.fmtLiters
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

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

private val czFmt0 = NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ")).apply {
    maximumFractionDigits = 0
}

private fun buildKegShareText(
    kegName: String,
    kegPrice: Double,
    kegSizeLiters: Double,
    totalBeers: Int,
    consumed: Double,
    remaining: Double,
    sortedDrinkers: List<Pair<String, Int>>
): String = buildString {
    appendLine("🍺 $kegName – vyúčtování")
    appendLine()
    appendLine("Bečka: ${czFmt0.format(kegPrice.roundToInt())} Kč | ${czFmt.format(kegSizeLiters)} l")
    if (totalBeers > 0) {
        appendLine("Vypito: $totalBeers piv (${czFmt.format(consumed)} l)")
        appendLine("Zbývá: ${czFmt.format(remaining)} l")
        appendLine()
        val pricePerBeer = kegPrice / totalBeers
        val pricePerLiter = kegPrice / consumed
        appendLine("Cena/pivo: ${czFmt0.format(pricePerBeer.roundToInt())} Kč")
        appendLine("Cena/l (spotřeba): ${czFmt.format(pricePerLiter)} Kč/l")
        appendLine()
        appendLine("Kolik kdo zaplatí:")
        sortedDrinkers.forEachIndexed { i, (name, count) ->
            val owes = (count.toDouble() / totalBeers * kegPrice).roundToInt()
            val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}." }
            appendLine("$medal $name – $count piv (${czFmt.format(count * LITERS_PER_BEER)} l) → ${czFmt0.format(owes)} Kč")
        }
    } else {
        appendLine("Zatím nikdo nepil.")
    }
    appendLine()
    append("Vygenerováno appkou Beer Counter")
}

// Jeden animovaný bublinový objekt; jeho progress (0→1) řídí pohyb a průhlednost.
private class BubbleAnim(val xFrac: Float, val radiusDp: Float) {
    val progress = Animatable(0f)
}

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
    val context = LocalContext.current

    val keg by viewModel.keg.collectAsState()
    val allPeople by viewModel.allPeople.collectAsState()
    val beerCountsByPerson by viewModel.beerCountsByPerson.collectAsState()
    val eventRecords by viewModel.eventRecords.collectAsState()
    val eventKegs by viewModel.eventKegs.collectAsState()
    val eventBeerCounts by viewModel.eventBeerCounts.collectAsState()
    val consumptionStats by viewModel.consumptionStats.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var milestoneMsg by remember { mutableStateOf<String?>(null) }
    var selectedPersonId by remember { mutableStateOf<Long?>(null) }
    var pendingUndoPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var bubbleTrigger by remember { mutableIntStateOf(0) }

    // ── Výpočet naplnění ──────────────────────────────────────────────────────
    val totalBeers = beerCountsByPerson.values.sum()
    val consumed = totalBeers * LITERS_PER_BEER
    val sizeLiters = keg?.sizeLiters ?: 1.0
    val remaining = (sizeLiters - consumed).coerceAtLeast(0.0)
    val fraction = (remaining / sizeLiters).toFloat().coerceIn(0f, 1f)
    val percentInt = (fraction * 100).roundToInt()

    val progressColor = when {
        fraction > 0.5f -> Color(0xFF388E3C)
        fraction > 0.2f -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.error
    }

    val situationalMsg = remember(percentInt) {
        when {
            percentInt == 0 -> FunnyMessages.pickRandom(FunnyMessages.BECKA_PRAZDNA)
            percentInt < 20 -> FunnyMessages.pickRandom(FunnyMessages.BECKA_DOCHAZI)
            else -> null
        }
    }

    // ── Seřazený seznam osob + ranky ──────────────────────────────────────────
    val sortedPeople = remember(allPeople, beerCountsByPerson) {
        allPeople.sortedByDescending { beerCountsByPerson[it.id] ?: 0 }
    }
    val rankByPerson: Map<Long, Int> = remember(sortedPeople, beerCountsByPerson) {
        val result = mutableMapOf<Long, Int>()
        sortedPeople.forEachIndexed { index, person ->
            val rank = if (index == 0) {
                1
            } else {
                val prevCount = beerCountsByPerson[sortedPeople[index - 1].id] ?: 0
                val currCount = beerCountsByPerson[person.id] ?: 0
                if (currCount == prevCount) result[sortedPeople[index - 1].id]!! else index + 1
            }
            result[person.id] = rank
        }
        result
    }
    val drinkers = remember(sortedPeople, beerCountsByPerson) {
        sortedPeople.filter { (beerCountsByPerson[it.id] ?: 0) > 0 }
    }
    val nonDrinkers = remember(sortedPeople, beerCountsByPerson) {
        sortedPeople.filter { (beerCountsByPerson[it.id] ?: 0) == 0 }
    }

    // ── Dialogy ───────────────────────────────────────────────────────────────
    milestoneMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { milestoneMsg = null },
            title = { Text("Milník!") },
            text = { Text(msg, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { milestoneMsg = null }) { Text("Skvělé!") }
            }
        )
    }

    selectedPersonId?.let { pid ->
        val stats = computePersonEventStats(
            personId = pid,
            allPeople = allPeople,
            eventRecords = eventRecords,
            eventKegs = eventKegs,
            allPersonCounts = eventBeerCounts
        )
        if (stats != null) {
            PersonDetailDialog(stats = stats, onDismiss = { selectedPersonId = null })
        }
    }

    pendingUndoPerson?.let { person ->
        AlertDialog(
            onDismissRequest = { pendingUndoPerson = null },
            title = { Text("Odebrat pivo?") },
            text = {
                Text(
                    "Opravdu chceš odebrat poslední pivo osobě ${person.name} u této bečky?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.undoLastBeer(person.id)
                    pendingUndoPerson = null
                }) { Text("Odebrat") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUndoPerson = null }) { Text("Zrušit") }
            }
        )
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(keg?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sortedDrinkers = allPeople
                            .filter { (beerCountsByPerson[it.id] ?: 0) > 0 }
                            .sortedByDescending { beerCountsByPerson[it.id] ?: 0 }
                            .map { it.name to (beerCountsByPerson[it.id] ?: 0) }
                        val text = buildKegShareText(
                            kegName = keg?.name ?: "",
                            kegPrice = keg?.price ?: 0.0,
                            kegSizeLiters = keg?.sizeLiters ?: 0.0,
                            totalBeers = totalBeers,
                            consumed = consumed,
                            remaining = remaining,
                            sortedDrinkers = sortedDrinkers
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Sdílet vyúčtování")
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
            keg?.let { k ->
                KegProgressHeader(
                    keg = k,
                    remaining = remaining,
                    fraction = fraction,
                    percentInt = percentInt,
                    progressColor = progressColor,
                    situationalMsg = situationalMsg,
                    consumptionStats = consumptionStats,
                    bubbleTrigger = bubbleTrigger,
                    totalBeers = totalBeers,
                    consumed = consumed
                )
            }

            HorizontalDivider()

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
                        TextButton(onClick = onNavigateToPeople) { Text("Přidat lidi") }
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
                    items(drinkers, key = { it.id }) { person ->
                        val beerCount = beerCountsByPerson[person.id] ?: 0
                        val rank = rankByPerson[person.id] ?: 1
                        PersonKegCard(
                            rank = rank,
                            person = person,
                            beerCount = beerCount,
                            onPersonClick = { selectedPersonId = person.id },
                            onRecordBeer = {
                                val newCount = beerCount + 1
                                viewModel.recordBeer(person.id)
                                bubbleTrigger++
                                if (newCount % 5 == 0) {
                                    milestoneMsg = FunnyMessages.milnikMessage(person.name, newCount)
                                } else {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = FunnyMessages.pickRandom(FunnyMessages.PO_PIVO),
                                            actionLabel = "Zpět",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoLastBeer(person.id)
                                        }
                                    }
                                }
                            },
                            onUndoBeer = { pendingUndoPerson = person }
                        )
                    }

                    if (nonDrinkers.isNotEmpty() && drinkers.isNotEmpty()) {
                        item(key = "separator_no_beers") {
                            Text(
                                text = "Bez piva u této bečky",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                    }

                    items(nonDrinkers, key = { it.id }) { person ->
                        val beerCount = 0
                        val rank = rankByPerson[person.id] ?: (drinkers.size + 1)
                        PersonKegCard(
                            rank = rank,
                            person = person,
                            beerCount = beerCount,
                            onPersonClick = { selectedPersonId = person.id },
                            onRecordBeer = {
                                viewModel.recordBeer(person.id)
                                bubbleTrigger++
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = FunnyMessages.pickRandom(FunnyMessages.PO_PIVO),
                                        actionLabel = "Zpět",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoLastBeer(person.id)
                                    }
                                }
                            },
                            onUndoBeer = { pendingUndoPerson = person }
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Vertikální ukazatel stavu bečky s animací bublinek
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun VerticalKegIndicator(
    fraction: Float,
    bubbleTrigger: Int,
    modifier: Modifier = Modifier
) {
    val beerColor = MaterialTheme.colorScheme.primary
    val foamColor = MaterialTheme.colorScheme.tertiary
    val woodColor = MaterialTheme.colorScheme.secondary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    val bubbles = remember { mutableStateListOf<BubbleAnim>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bubbleTrigger) {
        if (bubbleTrigger == 0) return@LaunchedEffect
        val newBubbles = (0 until 4).map {
            BubbleAnim(
                xFrac = Random.nextFloat() * 0.65f + 0.17f,
                radiusDp = Random.nextFloat() * 5f + 3f
            )
        }
        bubbles.addAll(newBubbles)
        scope.launch {
            try {
                val jobs = newBubbles.map { bubble ->
                    launch {
                        bubble.progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 500 + Random.nextInt(400),
                                easing = LinearEasing
                            )
                        )
                    }
                }
                jobs.forEach { it.join() }
            } finally {
                newBubbles.forEach { bubbles.remove(it) }
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cr = 14.dp.toPx()
        val strokeW = 3.5f.dp.toPx()

        drawRoundRect(color = emptyColor, cornerRadius = CornerRadius(cr))

        if (fraction > 0f) {
            val fillH = h * fraction
            val fillTop = h - fillH
            val foamH = (10.dp.toPx()).coerceAtMost(fillH * 0.12f).coerceAtLeast(3.dp.toPx())

            val clipPath = Path().apply {
                addRoundRect(RoundRect(0f, 0f, w, h, cr, cr))
            }
            clipPath(clipPath) {
                drawRect(
                    color = beerColor,
                    topLeft = Offset(0f, fillTop + foamH),
                    size = Size(w, (fillH - foamH).coerceAtLeast(0f))
                )
                drawRect(
                    color = foamColor,
                    topLeft = Offset(0f, fillTop),
                    size = Size(w, foamH)
                )
                bubbles.forEach { bubble ->
                    val prog = bubble.progress.value
                    val bx = w * bubble.xFrac
                    val by = h - prog * (fillH - foamH)
                    val r = bubble.radiusDp.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = (1f - prog) * 0.65f),
                        radius = r,
                        center = Offset(bx, by)
                    )
                }
            }
        }

        drawRoundRect(
            color = woodColor,
            cornerRadius = CornerRadius(cr),
            style = Stroke(width = strokeW)
        )

        val ringColor = woodColor.copy(alpha = 0.22f)
        val ringW = 2.dp.toPx()
        listOf(0.3f, 0.7f).forEach { pos ->
            drawLine(
                color = ringColor,
                start = Offset(strokeW, h * pos),
                end = Offset(w - strokeW, h * pos),
                strokeWidth = ringW
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Statistiky spotřeby bečky
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KegConsumptionStatsPanel(
    stats: KegConsumptionStats,
    modifier: Modifier = Modifier
) {
    val hasRecords = stats.avgLitersPerHour > 0.0

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Spotřeba bečky",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Za posledních 60 min: ${stats.lastHourBeers} piv (${fmtLiters(stats.lastHourLiters)})",
                style = MaterialTheme.typography.bodySmall
            )
            if (hasRecords) {
                Text(
                    text = "Průměr: ${czFmt.format(stats.avgLitersPerHour)} l/hod",
                    style = MaterialTheme.typography.bodySmall
                )
                val est = stats.estimatedHoursRemaining
                if (est != null) {
                    val h = est.toInt()
                    val min = ((est - h) * 60).roundToInt()
                    Text(
                        text = "Vydrží ještě přibližně ${h} h ${min} min",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Nelze odhadnout",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Zatím se nepilo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Hlavička bečky s vertikálním ukazatelem
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KegProgressHeader(
    keg: KegEntity,
    remaining: Double,
    fraction: Float,
    percentInt: Int,
    progressColor: Color,
    situationalMsg: String?,
    consumptionStats: KegConsumptionStats?,
    bubbleTrigger: Int,
    totalBeers: Int,
    consumed: Double
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerticalKegIndicator(
                fraction = fraction,
                bubbleTrigger = bubbleTrigger,
                modifier = Modifier
                    .width(110.dp)
                    .height(220.dp)
            )

            Spacer(Modifier.width(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Zbývá",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = czFmt.format(remaining) + " l",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "z ${czFmt.format(keg.sizeLiters)} l",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$percentInt %",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = fmtPrice(keg.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalBeers > 0) {
                    Text(
                        text = "${czFmt0.format((keg.price / totalBeers).roundToInt())} Kč/pivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = fmtPricePerLiter(keg.price, consumed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = fmtPricePerLiter(keg.price, keg.sizeLiters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                situationalMsg?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (fraction == 0f) MaterialTheme.colorScheme.error
                                else Color(0xFFF57C00)
                    )
                }
            }
        }

        if (consumptionStats != null && remaining > 0.0) {
            KegConsumptionStatsPanel(
                stats = consumptionStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Karta osoby v detailu bečky
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PersonKegCard(
    rank: Int,
    person: PersonEntity,
    beerCount: Int,
    onPersonClick: () -> Unit,
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
                text = "$rank.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPersonClick)
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
