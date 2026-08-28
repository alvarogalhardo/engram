package com.alvarogalhardo.engram

import com.alvarogalhardo.engram.domain.scheduler.CardState
import com.alvarogalhardo.engram.domain.scheduler.Grade
import com.alvarogalhardo.engram.domain.scheduler.SchedulingState
import com.alvarogalhardo.engram.domain.scheduler.Sm2Scheduler
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class Sm2SchedulerTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = LocalDateTime.of(2026, 8, 18, 15, 0).atZone(zone).toInstant().toEpochMilli()

    private fun review(interval: Int = 10, ease: Int = 2500) = SchedulingState(
        state = CardState.REVIEW,
        stepIndex = 0,
        intervalDays = interval,
        easeFactor = ease,
        repetitions = 3,
        lapses = 0,
    )

    private fun newCard() = SchedulingState(
        state = CardState.NEW,
        stepIndex = 0,
        intervalDays = 0,
        easeFactor = 2500,
        repetitions = 0,
        lapses = 0,
    )

    private fun midnightPlusDays(days: Int): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `review Good multiplies by the ease factor`() {
        val r = Sm2Scheduler.schedule(review(), Grade.GOOD, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(25, r.intervalDays)
        assertEquals(2500, r.easeFactor)
        assertEquals(midnightPlusDays(25), r.dueAt)
        assertEquals(4, r.repetitions)
    }

    @Test
    fun `review Hard uses 1_2 and penalises the ease`() {
        val r = Sm2Scheduler.schedule(review(), Grade.HARD, now, zone)
        assertEquals(12, r.intervalDays)
        assertEquals(2350, r.easeFactor)
    }

    @Test
    fun `review Easy applies the bonus and raises the ease`() {
        val r = Sm2Scheduler.schedule(review(), Grade.EASY, now, zone)
        assertEquals(33, r.intervalDays) // 10 * 2.5 * 1.3 = 32.5 → 33
        assertEquals(2650, r.easeFactor)
    }

    @Test
    fun `review Again goes to relearning with a halved interval`() {
        val r = Sm2Scheduler.schedule(review(), Grade.AGAIN, now, zone)
        assertEquals(CardState.RELEARNING, r.state)
        assertEquals(2300, r.easeFactor)
        assertEquals(5, r.intervalDays) // stored for the re-graduation
        assertEquals(1, r.lapses)
        assertEquals(now + 10 * 60_000L, r.dueAt)
    }

    @Test
    fun `relearning Good re-graduates with the stored interval`() {
        val relearning = review(interval = 5, ease = 2300).copy(state = CardState.RELEARNING)
        val r = Sm2Scheduler.schedule(relearning, Grade.GOOD, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(5, r.intervalDays)
        assertEquals(midnightPlusDays(5), r.dueAt)
    }

    @Test
    fun `new card Good advances to the 10 min step`() {
        val r = Sm2Scheduler.schedule(newCard(), Grade.GOOD, now, zone)
        assertEquals(CardState.LEARNING, r.state)
        assertEquals(1, r.stepIndex)
        assertEquals(now + 10 * 60_000L, r.dueAt)
    }

    @Test
    fun `learning on the last step Good graduates with 1 day`() {
        val learning = newCard().copy(state = CardState.LEARNING, stepIndex = 1)
        val r = Sm2Scheduler.schedule(learning, Grade.GOOD, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(1, r.intervalDays)
        assertEquals(midnightPlusDays(1), r.dueAt)
    }

    @Test
    fun `new card Easy graduates straight to 4 days`() {
        val r = Sm2Scheduler.schedule(newCard(), Grade.EASY, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(4, r.intervalDays)
        assertEquals(midnightPlusDays(4), r.dueAt)
    }

    @Test
    fun `new card Again returns to the 1 min step`() {
        val r = Sm2Scheduler.schedule(newCard(), Grade.AGAIN, now, zone)
        assertEquals(CardState.LEARNING, r.state)
        assertEquals(0, r.stepIndex)
        assertEquals(now + 1 * 60_000L, r.dueAt)
    }

    @Test
    fun `ease never drops below the floor`() {
        val r = Sm2Scheduler.schedule(review(ease = 1300), Grade.HARD, now, zone)
        assertEquals(1300, r.easeFactor)
        assertEquals(12, r.intervalDays)
    }

    @Test
    fun `a review interval always grows by at least 1 day`() {
        val r = Sm2Scheduler.schedule(review(interval = 1, ease = 1300), Grade.GOOD, now, zone)
        assertEquals(2, r.intervalDays) // max(1+1, round(1*1.3)=1)
    }

    @Test
    fun `preview shows minute and day labels`() {
        val p = Sm2Scheduler.preview(newCard(), now, zone)
        assertEquals("1 min", p[Grade.AGAIN])
        assertEquals("10 min", p[Grade.GOOD])
        assertEquals("4 d", p[Grade.EASY])
        val pr = Sm2Scheduler.preview(review(), now, zone)
        assertEquals("25 d", pr[Grade.GOOD])
    }
}
