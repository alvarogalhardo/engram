package com.alvarogalhardo.engram.domain.scheduler

/** Estado de agendamento de uma carta, desacoplado da entidade Room. */
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
