package com.alvarogalhardo.engram.domain.study

object QueueBuilder {
    /** Ordem de estudo: aprendendo vencidas → revisões de hoje → novas até a cota diária. */
    fun <T> build(learningDue: List<T>, reviewsDue: List<T>, newCards: List<T>, newAllowance: Int): List<T> =
        learningDue + reviewsDue + newCards.take(newAllowance.coerceAtLeast(0))
}
