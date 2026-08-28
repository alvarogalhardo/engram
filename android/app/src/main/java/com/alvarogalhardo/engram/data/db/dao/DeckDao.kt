package com.alvarogalhardo.engram.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alvarogalhardo.engram.data.db.entity.Deck
import kotlinx.coroutines.flow.Flow

data class DeckWithCounts(
    val deckId: Long,
    val name: String,
    val total: Int,
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
)

@Dao
interface DeckDao {
    @Query(
        """
        SELECT d.id AS deckId, d.name AS name,
            COUNT(c.id) AS total,
            COALESCE(SUM(CASE WHEN c.state = 0 THEN 1 ELSE 0 END), 0) AS newCount,
            COALESCE(SUM(CASE WHEN c.state IN (1, 3) AND c.dueAt <= :now THEN 1 ELSE 0 END), 0) AS learningCount,
            COALESCE(SUM(CASE WHEN c.state = 2 AND c.dueAt < :endOfToday THEN 1 ELSE 0 END), 0) AS reviewCount
        FROM decks d LEFT JOIN cards c ON c.deckId = d.id
        GROUP BY d.id
        ORDER BY d.name COLLATE NOCASE
        """
    )
    fun observeDecksWithCounts(now: Long, endOfToday: Long): Flow<List<DeckWithCounts>>

    @Insert
    suspend fun insert(deck: Deck): Long

    @Query("UPDATE decks SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun byId(id: Long): Deck?

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observeById(id: Long): Flow<Deck?>

    @Query("SELECT name FROM decks")
    suspend fun allNames(): List<String>
}
