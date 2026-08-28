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
    fun `review Bom multiplica pelo ease`() {
        val r = Sm2Scheduler.schedule(review(), Grade.GOOD, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(25, r.intervalDays)
        assertEquals(2500, r.easeFactor)
        assertEquals(midnightPlusDays(25), r.dueAt)
        assertEquals(4, r.repetitions)
    }

    @Test
    fun `review Dificil usa 1_2 e penaliza ease`() {
        val r = Sm2Scheduler.schedule(review(), Grade.HARD, now, zone)
        assertEquals(12, r.intervalDays)
        assertEquals(2350, r.easeFactor)
    }

    @Test
    fun `review Facil aplica bonus e aumenta ease`() {
        val r = Sm2Scheduler.schedule(review(), Grade.EASY, now, zone)
        assertEquals(33, r.intervalDays) // 10 * 2.5 * 1.3 = 32.5 → 33
        assertEquals(2650, r.easeFactor)
    }

    @Test
    fun `review Errei vira relearning com intervalo pela metade`() {
        val r = Sm2Scheduler.schedule(review(), Grade.AGAIN, now, zone)
        assertEquals(CardState.RELEARNING, r.state)
        assertEquals(2300, r.easeFactor)
        assertEquals(5, r.intervalDays) // guardado para a re-graduação
        assertEquals(1, r.lapses)
        assertEquals(now + 10 * 60_000L, r.dueAt)
    }

    @Test
    fun `relearning Bom re-gradua com o intervalo guardado`() {
        val relearning = review(interval = 5, ease = 2300).copy(state = CardState.RELEARNING)
        val r = Sm2Scheduler.schedule(relearning, Grade.GOOD, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(5, r.intervalDays)
        assertEquals(midnightPlusDays(5), r.dueAt)
    }

    @Test
    fun `carta nova Bom avanca para o passo de 10 min`() {
        val r = Sm2Scheduler.schedule(newCard(), Grade.GOOD, now, zone)
        assertEquals(CardState.LEARNING, r.state)
        assertEquals(1, r.stepIndex)
        assertEquals(now + 10 * 60_000L, r.dueAt)
    }

    @Test
    fun `learning no ultimo passo Bom gradua com 1 dia`() {
        val learning = newCard().copy(state = CardState.LEARNING, stepIndex = 1)
        val r = Sm2Scheduler.schedule(learning, Grade.GOOD, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(1, r.intervalDays)
        assertEquals(midnightPlusDays(1), r.dueAt)
    }

    @Test
    fun `carta nova Facil gradua direto com 4 dias`() {
        val r = Sm2Scheduler.schedule(newCard(), Grade.EASY, now, zone)
        assertEquals(CardState.REVIEW, r.state)
        assertEquals(4, r.intervalDays)
        assertEquals(midnightPlusDays(4), r.dueAt)
    }

    @Test
    fun `carta nova Errei volta ao passo de 1 min`() {
        val r = Sm2Scheduler.schedule(newCard(), Grade.AGAIN, now, zone)
        assertEquals(CardState.LEARNING, r.state)
        assertEquals(0, r.stepIndex)
        assertEquals(now + 1 * 60_000L, r.dueAt)
    }

    @Test
    fun `ease nunca cai abaixo do piso`() {
        val r = Sm2Scheduler.schedule(review(ease = 1300), Grade.HARD, now, zone)
        assertEquals(1300, r.easeFactor)
        assertEquals(12, r.intervalDays)
    }

    @Test
    fun `intervalo sempre cresce pelo menos 1 dia em revisao`() {
        val r = Sm2Scheduler.schedule(review(interval = 1, ease = 1300), Grade.GOOD, now, zone)
        assertEquals(2, r.intervalDays) // max(1+1, round(1*1.3)=1)
    }

    @Test
    fun `preview mostra rotulos de minutos e dias`() {
        val p = Sm2Scheduler.preview(newCard(), now, zone)
        assertEquals("1 min", p[Grade.AGAIN])
        assertEquals("10 min", p[Grade.GOOD])
        assertEquals("4 d", p[Grade.EASY])
        val pr = Sm2Scheduler.preview(review(), now, zone)
        assertEquals("25 d", pr[Grade.GOOD])
    }
}
