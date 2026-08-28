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
    /** Conteúdo renderizável, sempre HTML. */
    val front: String,
    val back: String,
    /** Fonte em Markdown para cartas criadas no app; null para importadas (editadas como HTML cru). */
    val frontSrc: String? = null,
    val backSrc: String? = null,
    val state: Int = CardState.NEW,
    val stepIndex: Int = 0,
    val intervalDays: Int = 0,
    /** Fator de facilidade em permile (2500 = 250%). */
    val easeFactor: Int = 2500,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val dueAt: Long,
    val createdAt: Long,
)
