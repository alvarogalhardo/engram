package com.alvarogalhardo.engram.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alvarogalhardo.engram.data.db.entity.ReviewLog

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLog)

    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :since ORDER BY reviewedAt")
    suspend fun logsSince(since: Long): List<ReviewLog>

    @Query(
        """
        SELECT COUNT(DISTINCT cardId) FROM review_logs
        WHERE stateBefore = 0 AND deckId = :deckId AND reviewedAt BETWEEN :start AND :end
        """
    )
    suspend fun countNewIntroducedBetween(deckId: Long, start: Long, end: Long): Int
}
