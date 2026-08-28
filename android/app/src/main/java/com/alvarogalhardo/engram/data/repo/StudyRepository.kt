package com.alvarogalhardo.engram.data.repo

import com.alvarogalhardo.engram.data.db.dao.CardDao
import com.alvarogalhardo.engram.data.db.dao.ReviewLogDao
import com.alvarogalhardo.engram.data.db.entity.Card
import com.alvarogalhardo.engram.data.db.entity.ReviewLog
import com.alvarogalhardo.engram.data.settings.SettingsStore
import com.alvarogalhardo.engram.domain.scheduler.Grade
import com.alvarogalhardo.engram.domain.scheduler.SchedulingState
import com.alvarogalhardo.engram.domain.scheduler.Sm2Scheduler
import com.alvarogalhardo.engram.domain.study.QueueBuilder
import com.alvarogalhardo.engram.util.TimeProvider
import java.time.Instant
import kotlinx.coroutines.flow.first

class StudyRepository(
    private val cardDao: CardDao,
    private val reviewLogDao: ReviewLogDao,
    private val settings: SettingsStore,
    private val clock: TimeProvider,
) {
    suspend fun buildQueue(deckId: Long): List<Card> {
        val now = clock.nowMillis()
        val zone = clock.zone()
        val startOfToday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfToday = Sm2Scheduler.dueAfterDays(now, 1, zone)

        val newPerDay = settings.newPerDay.first()
        val introducedToday = reviewLogDao.countNewIntroducedBetween(deckId, startOfToday, now)
        val allowance = (newPerDay - introducedToday).coerceAtLeast(0)

        return QueueBuilder.build(
            learningDue = cardDao.learningDue(deckId, now),
            reviewsDue = cardDao.reviewsDue(deckId, endOfToday),
            newCards = cardDao.newCards(deckId, allowance),
            newAllowance = allowance,
        )
    }

    suspend fun answer(card: Card, grade: Grade): Card {
        val now = clock.nowMillis()
        val result = Sm2Scheduler.schedule(card.toSchedulingState(), grade, now, clock.zone())
        val updated = card.copy(
            state = result.state,
            stepIndex = result.stepIndex,
            intervalDays = result.intervalDays,
            easeFactor = result.easeFactor,
            repetitions = result.repetitions,
            lapses = result.lapses,
            dueAt = result.dueAt,
        )
        cardDao.update(updated)
        reviewLogDao.insert(
            ReviewLog(
                cardId = card.id,
                deckId = card.deckId,
                reviewedAt = now,
                grade = grade.value,
                stateBefore = card.state,
                intervalBeforeDays = card.intervalDays,
                intervalAfterDays = result.intervalDays,
            )
        )
        return updated
    }

    fun previews(card: Card): Map<Grade, String> =
        Sm2Scheduler.preview(card.toSchedulingState(), clock.nowMillis(), clock.zone())

    suspend fun nextLearningDueAt(deckId: Long): Long? = cardDao.nextLearningDueAt(deckId)

    private fun Card.toSchedulingState() = SchedulingState(
        state = state,
        stepIndex = stepIndex,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        repetitions = repetitions,
        lapses = lapses,
    )
}
