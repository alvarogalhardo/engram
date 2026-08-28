package com.alvarogalhardo.engram.domain.stats

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReviewSample(val reviewedAt: Long, val grade: Int)

data class DayCount(val date: LocalDate, val count: Int)

data class StatsResult(
    val last30Days: List<DayCount>,
    val streakDays: Int,
    /** Fração de respostas != Errei; null se não houver revisões no período. */
    val accuracy30: Double?,
    val accuracyAll: Double?,
    val totalReviews: Int,
)

object StatsCalculator {
    fun calculate(samples: List<ReviewSample>, today: LocalDate, zone: ZoneId): StatsResult {
        val byDay = samples.groupBy { Instant.ofEpochMilli(it.reviewedAt).atZone(zone).toLocalDate() }

        val last30 = (29 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayCount(date, byDay[date]?.size ?: 0)
        }

        // Streak: dias consecutivos com >= 1 revisão, terminando hoje
        // (ou ontem, se hoje ainda não estudou — o streak ainda não quebrou).
        var streak = 0
        var cursor = if (byDay.containsKey(today)) today else today.minusDays(1)
        while (byDay.containsKey(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val cutoff30 = today.minusDays(29)
        val last30Samples = samples.filter {
            Instant.ofEpochMilli(it.reviewedAt).atZone(zone).toLocalDate() >= cutoff30
        }

        return StatsResult(
            last30Days = last30,
            streakDays = streak,
            accuracy30 = accuracy(last30Samples),
            accuracyAll = accuracy(samples),
            totalReviews = samples.size,
        )
    }

    private fun accuracy(samples: List<ReviewSample>): Double? =
        if (samples.isEmpty()) null
        else samples.count { it.grade != 0 }.toDouble() / samples.size
}
