package com.alvarogalhardo.engram.domain.scheduler

import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Anki-style SM-2, deterministic (no fuzz).
 *
 * - New/learning cards walk the learning steps (1 min, 10 min) before graduating.
 * - Review cards use the ease factor (permille) with the classic multipliers.
 * - Review due dates land on local midnight, so "due today" holds all day long.
 */
object Sm2Scheduler {
    val LEARNING_STEPS_MINUTES = listOf(1L, 10L)
    val RELEARNING_STEPS_MINUTES = listOf(10L)
    const val GRADUATE_INTERVAL_DAYS = 1
    const val EASY_INTERVAL_DAYS = 4
    const val START_EASE = 2500
    const val MIN_EASE = 1300
    const val MAX_INTERVAL_DAYS = 36500
    const val HARD_MULTIPLIER = 1.2
    const val EASY_BONUS = 1.3
    const val LAPSE_MULTIPLIER = 0.5
    const val LAPSE_EASE_PENALTY = 200
    const val HARD_EASE_PENALTY = 150
    const val EASY_EASE_BONUS = 150

    fun schedule(s: SchedulingState, grade: Grade, nowMillis: Long, zone: ZoneId): SchedulingResult =
        when (s.state) {
            CardState.NEW, CardState.LEARNING -> scheduleLearning(s, grade, nowMillis, zone)
            CardState.RELEARNING -> scheduleRelearning(s, grade, nowMillis, zone)
            else -> scheduleReview(s, grade, nowMillis, zone)
        }

    /** Next-interval label for each answer button ("1 min", "25 d", ...). */
    fun preview(s: SchedulingState, nowMillis: Long, zone: ZoneId): Map<Grade, String> =
        Grade.entries.associateWith { grade ->
            val r = schedule(s, grade, nowMillis, zone)
            if (r.state == CardState.REVIEW) {
                formatDays(r.intervalDays)
            } else {
                val minutes = max(1L, (r.dueAt - nowMillis) / 60_000L)
                "$minutes min"
            }
        }

    private fun formatDays(days: Int): String = when {
        days >= 365 -> "%.1f a".format(days / 365.0)
        days >= 30 -> "%.1f m".format(days / 30.0)
        else -> "$days d"
    }

    private fun scheduleLearning(s: SchedulingState, grade: Grade, now: Long, zone: ZoneId): SchedulingResult {
        val steps = LEARNING_STEPS_MINUTES
        // A NEW card behaves as if it were on step 0.
        val currentStep = if (s.state == CardState.NEW) 0 else s.stepIndex
        return when (grade) {
            Grade.AGAIN -> learningStep(s, 0, now)
            Grade.HARD -> learningStep(s, currentStep, now)
            Grade.GOOD -> {
                val next = currentStep + 1
                if (next >= steps.size) graduate(s, GRADUATE_INTERVAL_DAYS, s.easeFactor, now, zone)
                else learningStep(s, next, now)
            }
            Grade.EASY -> graduate(s, EASY_INTERVAL_DAYS, s.easeFactor, now, zone)
        }
    }

    private fun learningStep(s: SchedulingState, step: Int, now: Long): SchedulingResult {
        val idx = step.coerceIn(0, LEARNING_STEPS_MINUTES.size - 1)
        return SchedulingResult(
            state = CardState.LEARNING,
            stepIndex = idx,
            intervalDays = s.intervalDays,
            easeFactor = s.easeFactor,
            repetitions = s.repetitions,
            lapses = s.lapses,
            dueAt = now + LEARNING_STEPS_MINUTES[idx] * 60_000L,
        )
    }

    private fun graduate(s: SchedulingState, intervalDays: Int, ease: Int, now: Long, zone: ZoneId): SchedulingResult {
        val interval = intervalDays.coerceAtMost(MAX_INTERVAL_DAYS)
        return SchedulingResult(
            state = CardState.REVIEW,
            stepIndex = 0,
            intervalDays = interval,
            easeFactor = ease,
            repetitions = s.repetitions + 1,
            lapses = s.lapses,
            dueAt = dueAfterDays(now, interval, zone),
        )
    }

    private fun scheduleReview(s: SchedulingState, grade: Grade, now: Long, zone: ZoneId): SchedulingResult =
        when (grade) {
            Grade.AGAIN -> SchedulingResult(
                state = CardState.RELEARNING,
                stepIndex = 0,
                // The post-lapse interval is stored now and applied when the card re-graduates.
                intervalDays = max(1, (s.intervalDays * LAPSE_MULTIPLIER).roundToInt()),
                easeFactor = max(MIN_EASE, s.easeFactor - LAPSE_EASE_PENALTY),
                repetitions = s.repetitions,
                lapses = s.lapses + 1,
                dueAt = now + RELEARNING_STEPS_MINUTES[0] * 60_000L,
            )
            Grade.HARD -> reviewResult(
                s,
                interval = max(s.intervalDays + 1, (s.intervalDays * HARD_MULTIPLIER).roundToInt()),
                ease = max(MIN_EASE, s.easeFactor - HARD_EASE_PENALTY),
                now = now,
                zone = zone,
            )
            Grade.GOOD -> reviewResult(
                s,
                interval = max(s.intervalDays + 1, (s.intervalDays * (s.easeFactor / 1000.0)).roundToInt()),
                ease = s.easeFactor,
                now = now,
                zone = zone,
            )
            Grade.EASY -> reviewResult(
                s,
                interval = max(s.intervalDays + 1, (s.intervalDays * (s.easeFactor / 1000.0) * EASY_BONUS).roundToInt()),
                ease = s.easeFactor + EASY_EASE_BONUS,
                now = now,
                zone = zone,
            )
        }

    private fun reviewResult(s: SchedulingState, interval: Int, ease: Int, now: Long, zone: ZoneId): SchedulingResult {
        val clamped = interval.coerceAtMost(MAX_INTERVAL_DAYS)
        return SchedulingResult(
            state = CardState.REVIEW,
            stepIndex = 0,
            intervalDays = clamped,
            easeFactor = ease,
            repetitions = s.repetitions + 1,
            lapses = s.lapses,
            dueAt = dueAfterDays(now, clamped, zone),
        )
    }

    private fun scheduleRelearning(s: SchedulingState, grade: Grade, now: Long, zone: ZoneId): SchedulingResult =
        when (grade) {
            Grade.AGAIN, Grade.HARD -> SchedulingResult(
                state = CardState.RELEARNING,
                stepIndex = 0,
                intervalDays = s.intervalDays,
                easeFactor = s.easeFactor,
                repetitions = s.repetitions,
                lapses = s.lapses,
                dueAt = now + RELEARNING_STEPS_MINUTES[0] * 60_000L,
            )
            Grade.GOOD, Grade.EASY -> graduate(s, max(1, s.intervalDays), s.easeFactor, now, zone)
        }

    /** Local midnight + N days: anything due "today" stays due all day. */
    fun dueAfterDays(now: Long, days: Int, zone: ZoneId): Long =
        Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .plusDays(days.toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
}
