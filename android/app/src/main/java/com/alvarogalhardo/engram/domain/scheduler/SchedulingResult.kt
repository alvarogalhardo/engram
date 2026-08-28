package com.alvarogalhardo.engram.domain.scheduler

/** A card's scheduling state, decoupled from the Room entity. */
data class SchedulingState(
    val state: Int,
    val stepIndex: Int,
    val intervalDays: Int,
    val easeFactor: Int,
    val repetitions: Int,
    val lapses: Int,
)

data class SchedulingResult(
    val state: Int,
    val stepIndex: Int,
    val intervalDays: Int,
    val easeFactor: Int,
    val repetitions: Int,
    val lapses: Int,
    val dueAt: Long,
)
