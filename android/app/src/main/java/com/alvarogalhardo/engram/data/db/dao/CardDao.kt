package com.alvarogalhardo.engram.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alvarogalhardo.engram.data.db.entity.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: Card): Long

    @Insert
    suspend fun insertAll(cards: List<Card>)

    @Update
    suspend fun update(card: Card)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun byId(id: Long): Card?

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt")
    fun observeCardsInDeck(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND state IN (1, 3) AND dueAt <= :now ORDER BY dueAt")
    suspend fun learningDue(deckId: Long, now: Long): List<Card>

    @Query("SELECT MIN(dueAt) FROM cards WHERE deckId = :deckId AND state IN (1, 3)")
    suspend fun nextLearningDueAt(deckId: Long): Long?

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND state = 2 AND dueAt < :endOfToday ORDER BY dueAt")
    suspend fun reviewsDue(deckId: Long, endOfToday: Long): List<Card>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND state = 0 ORDER BY dueAt LIMIT :limit")
    suspend fun newCards(deckId: Long, limit: Int): List<Card>
}
