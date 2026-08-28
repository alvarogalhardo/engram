package com.alvarogalhardo.engram.domain.study

object QueueBuilder {
    /** Study order: due learning cards → today's reviews → new cards up to the daily quota. */
    fun <T> build(learningDue: List<T>, reviewsDue: List<T>, newCards: List<T>, newAllowance: Int): List<T> =
        learningDue + reviewsDue + newCards.take(newAllowance.coerceAtLeast(0))
}
