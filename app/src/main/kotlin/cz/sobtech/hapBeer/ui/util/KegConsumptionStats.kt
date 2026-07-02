package cz.sobtech.hapBeer.ui.util

import cz.sobtech.hapBeer.data.entity.BeerRecordEntity

data class KegConsumptionStats(
    val lastHourBeers: Int,
    val lastHourLiters: Double,
    val avgLitersPerHour: Double,
    val estimatedHoursRemaining: Double?
)

fun computeKegConsumptionStats(
    records: List<BeerRecordEntity>,
    sizeLiters: Double,
    nowMs: Long
): KegConsumptionStats {
    val oneHourAgo = nowMs - 3_600_000L
    val lastHourBeers = records.count { it.timestamp >= oneHourAgo }
    val lastHourLiters = lastHourBeers * LITERS_PER_BEER

    val totalLiters = records.size * LITERS_PER_BEER
    val remainingLiters = (sizeLiters - totalLiters).coerceAtLeast(0.0)

    val avgLitersPerHour = if (records.isEmpty()) {
        0.0
    } else {
        val firstTimestamp = records.minOf { it.timestamp }
        val elapsedHours = ((nowMs - firstTimestamp) / 3_600_000.0).coerceAtLeast(1.0)
        totalLiters / elapsedHours
    }

    val estimatedHoursRemaining = if (avgLitersPerHour > 0.0) {
        remainingLiters / avgLitersPerHour
    } else {
        null
    }

    return KegConsumptionStats(
        lastHourBeers = lastHourBeers,
        lastHourLiters = lastHourLiters,
        avgLitersPerHour = avgLitersPerHour,
        estimatedHoursRemaining = estimatedHoursRemaining
    )
}
