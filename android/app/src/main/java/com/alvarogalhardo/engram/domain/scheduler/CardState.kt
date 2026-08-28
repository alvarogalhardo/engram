package com.alvarogalhardo.engram.domain.scheduler

object CardState {
    const val NEW = 0
    const val LEARNING = 1
    const val REVIEW = 2
    const val RELEARNING = 3
}

enum class Grade(val value: Int) {
    AGAIN(0),
    HARD(1),
    GOOD(2),
    EASY(3),
}
