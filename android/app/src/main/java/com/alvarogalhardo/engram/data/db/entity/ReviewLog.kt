package com.alvarogalhardo.engram.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("cardId"), Index("reviewedAt")],
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    val reviewedAt: Long,
    /** 0=Errei, 1=Difícil, 2=Bom, 3=Fácil */
    val grade: Int,
    val stateBefore: Int,
    val intervalBeforeDays: Int,
    val intervalAfterDays: Int,
)
