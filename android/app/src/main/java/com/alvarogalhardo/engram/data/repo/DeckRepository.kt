package com.alvarogalhardo.engram.data.repo

import com.alvarogalhardo.engram.data.db.dao.CardDao
import com.alvarogalhardo.engram.data.db.dao.DeckDao
import com.alvarogalhardo.engram.data.db.dao.DeckWithCounts
import com.alvarogalhardo.engram.data.db.entity.Card
import com.alvarogalhardo.engram.data.db.entity.Deck
import com.alvarogalhardo.engram.domain.markdown.MarkdownConverter
import com.alvarogalhardo.engram.domain.scheduler.Sm2Scheduler
import com.alvarogalhardo.engram.util.TimeProvider
import kotlinx.coroutines.flow.Flow

class DeckRepository(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val clock: TimeProvider,
) {
    fun observeDecksWithCounts(): Flow<List<DeckWithCounts>> {
        val now = clock.nowMillis()
        val endOfToday = Sm2Scheduler.dueAfterDays(now, 1, clock.zone())
        return deckDao.observeDecksWithCounts(now, endOfToday)
    }

    fun observeDeck(id: Long): Flow<Deck?> = deckDao.observeById(id)

    fun observeCardsInDeck(deckId: Long): Flow<List<Card>> = cardDao.observeCardsInDeck(deckId)

    suspend fun createDeck(name: String): Long =
        deckDao.insert(Deck(name = name.trim(), createdAt = clock.nowMillis()))

    suspend fun renameDeck(id: Long, name: String) = deckDao.rename(id, name.trim())

    suspend fun deleteDeck(id: Long) = deckDao.delete(id)

    suspend fun cardById(id: Long): Card? = cardDao.byId(id)

    suspend fun deleteCard(id: Long) = cardDao.delete(id)

    /** Creates a card authored in Markdown inside the app. */
    suspend fun addCard(deckId: Long, frontMd: String, backMd: String) {
        val now = clock.nowMillis()
        cardDao.insert(
            Card(
                deckId = deckId,
                front = MarkdownConverter.toHtml(frontMd),
                back = MarkdownConverter.toHtml(backMd),
                frontSrc = frontMd,
                backSrc = backMd,
                dueAt = now,
                createdAt = now,
            )
        )
    }

    /**
     * Saves an edit preserving how the card was authored: Markdown (frontSrc != null)
     * is re-converted, while imported cards are edited as raw HTML.
     */
    suspend fun updateCardContent(card: Card, front: String, back: String) {
        val updated = if (card.frontSrc != null) {
            card.copy(
                front = MarkdownConverter.toHtml(front),
                back = MarkdownConverter.toHtml(back),
                frontSrc = front,
                backSrc = back,
            )
        } else {
            card.copy(front = front, back = back)
        }
        cardDao.update(updated)
    }
}
