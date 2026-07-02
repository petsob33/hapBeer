package cz.sobtech.hapBeer.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.sobtech.hapBeer.data.entity.BeerRecordEntity
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val LITERS_PER_BEER = 0.5

private val czFmt2 = NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private val czFmt1 = NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ")).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}

fun fmtLiters(liters: Double): String = "${czFmt2.format(liters)} l"

data class PersonEventStats(
    val personId: Long,
    val personName: String,
    val totalBeers: Int,
    val totalLiters: Double,
    val rank: Int,
    val rankTotal: Int,
    val avgBeersPerHour: Double?,
    val firstBeerTimestamp: Long?,
    val beersByKeg: List<Pair<String, Int>>
)

fun computePersonEventStats(
    personId: Long,
    allPeople: List<PersonEntity>,
    eventRecords: List<BeerRecordEntity>,
    eventKegs: List<KegEntity>,
    allPersonCounts: Map<Long, Int>
): PersonEventStats? {
    val person = allPeople.find { it.id == personId } ?: return null
    val personRecords = eventRecords.filter { it.personId == personId }
    val totalBeers = personRecords.size

    val sortedIds = allPersonCounts.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
        .map { it.key }
    val rank = (sortedIds.indexOf(personId) + 1).let { if (it == 0) sortedIds.size + 1 else it }
    val rankTotal = sortedIds.size.coerceAtLeast(1)

    val firstBeerTimestamp: Long?
    val avgBeersPerHour: Double?
    if (personRecords.size <= 1) {
        firstBeerTimestamp = personRecords.firstOrNull()?.timestamp
        avgBeersPerHour = null
    } else {
        firstBeerTimestamp = personRecords.minOf { it.timestamp }
        val lastTimestamp = personRecords.maxOf { it.timestamp }
        val hours = ((lastTimestamp - firstBeerTimestamp) / 3_600_000.0).coerceAtLeast(1.0)
        avgBeersPerHour = totalBeers / hours
    }

    val kegMap = eventKegs.associateBy { it.id }
    val beersByKeg = personRecords
        .groupingBy { it.kegId }
        .eachCount()
        .mapNotNull { (kegId, count) -> kegMap[kegId]?.name?.let { name -> Pair(name, count) } }
        .sortedByDescending { it.second }

    return PersonEventStats(
        personId = personId,
        personName = person.name,
        totalBeers = totalBeers,
        totalLiters = totalBeers * LITERS_PER_BEER,
        rank = rank,
        rankTotal = rankTotal,
        avgBeersPerHour = avgBeersPerHour,
        firstBeerTimestamp = firstBeerTimestamp,
        beersByKeg = beersByKeg
    )
}

@Composable
fun PersonDetailDialog(
    stats: PersonEventStats,
    onDismiss: () -> Unit
) {
    val timeFmt = SimpleDateFormat("HH:mm", Locale.forLanguageTag("cs-CZ"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                if (stats.rank == 1) {
                    Text(
                        text = FunnyMessages.pickRandom(FunnyMessages.PRVNI_MISTO),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFB347)
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = stats.personName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${stats.rank}. místo z ${stats.rankTotal}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stats.totalBeers} piv  ·  ${fmtLiters(stats.totalLiters)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (stats.avgBeersPerHour != null) {
                    Text(
                        text = "Průměr: ${czFmt1.format(stats.avgBeersPerHour)} piv/hod",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (stats.firstBeerTimestamp != null) {
                    Text(
                        text = "První pivo: ${timeFmt.format(Date(stats.firstBeerTimestamp))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (stats.beersByKeg.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Po bečkách:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    stats.beersByKeg.forEach { (kegName, count) ->
                        Text(
                            text = "• $kegName – $count piv (${fmtLiters(count * LITERS_PER_BEER)})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zavřít") }
        }
    )
}
