package com.alvarogalhardo.engram

import com.alvarogalhardo.engram.domain.stats.ReviewSample
import com.alvarogalhardo.engram.domain.stats.StatsCalculator
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsCalculatorTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 8, 18)

    private fun at(date: LocalDate, hour: Int = 12, grade: Int = 2) =
        ReviewSample(date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(), grade)

    @Test
    fun `streak counts consecutive days ending today`() {
        val samples = listOf(
            at(today), at(today, 20),
            at(today.minusDays(1)),
            at(today.minusDays(2)),
        )
        val r = StatsCalculator.calculate(samples, today, zone)
        assertEquals(3, r.streakDays)
    }

    @Test
    fun `streak survives a today with no reviews yet`() {
        val samples = listOf(at(today.minusDays(1)), at(today.minusDays(2)))
        val r = StatsCalculator.calculate(samples, today, zone)
        assertEquals(2, r.streakDays)
    }

    @Test
    fun `a one day gap breaks the streak`() {
        val samples = listOf(at(today), at(today.minusDays(2)))
        val r = StatsCalculator.calculate(samples, today, zone)
        assertEquals(1, r.streakDays)
    }

    @Test
    fun `accuracy counts everything except Again`() {
        val samples = listOf(
            at(today, grade = 2),
            at(today, grade = 0),
            at(today, grade = 3),
        )
        val r = StatsCalculator.calculate(samples, today, zone)
        assertEquals(2.0 / 3.0, r.accuracyAll!!, 1e-9)
        assertEquals(2.0 / 3.0, r.accuracy30!!, 1e-9)
        assertEquals(3, r.totalReviews)
    }

    @Test
    fun `chart covers exactly 30 days ending today`() {
        val samples = listOf(at(today), at(today), at(today.minusDays(29)))
        val r = StatsCalculator.calculate(samples, today, zone)
        assertEquals(30, r.last30Days.size)
        assertEquals(today, r.last30Days.last().date)
        assertEquals(2, r.last30Days.last().count)
        assertEquals(today.minusDays(29), r.last30Days.first().date)
        assertEquals(1, r.last30Days.first().count)
    }

    @Test
    fun `a review outside the 30 day window leaves accuracy30 but not the overall`() {
        val samples = listOf(at(today.minusDays(40), grade = 0), at(today, grade = 2))
        val r = StatsCalculator.calculate(samples, today, zone)
        assertEquals(1.0, r.accuracy30!!, 1e-9)
        assertEquals(0.5, r.accuracyAll!!, 1e-9)
    }

    @Test
    fun `no data yields zeros and nulls`() {
        val r = StatsCalculator.calculate(emptyList(), today, zone)
        assertEquals(0, r.streakDays)
        assertEquals(0, r.totalReviews)
        assertNull(r.accuracy30)
        assertNull(r.accuracyAll)
        assertEquals(30, r.last30Days.size)
    }
}
