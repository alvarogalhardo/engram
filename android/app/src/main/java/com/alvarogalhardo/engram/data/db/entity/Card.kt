package com.alvarogalhardo.engram.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alvarogalhardo.engram.domain.scheduler.CardState

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("deckId"), Index("state"), Index("dueAt")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    /** Renderable content, always HTML. */
    val front: String,
    val back: String,
    /** Markdown source for cards authored in the app; null for imported ones (edited as raw HTML). */
    val frontSrc: String? = null,
    val backSrc: String? = null,
    val state: Int = CardState.NEW,
    val stepIndex: Int = 0,
    val intervalDays: Int = 0,
    /** Ease factor in permille (2500 = 250%). */
    val easeFactor: Int = 2500,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val dueAt: Long,
    val createdAt: Long,
)
