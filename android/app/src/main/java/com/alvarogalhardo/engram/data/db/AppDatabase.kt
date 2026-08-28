package com.alvarogalhardo.engram.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alvarogalhardo.engram.data.db.dao.CardDao
import com.alvarogalhardo.engram.data.db.dao.DeckDao
import com.alvarogalhardo.engram.data.db.dao.ReviewLogDao
import com.alvarogalhardo.engram.data.db.entity.Card
import com.alvarogalhardo.engram.data.db.entity.Deck
import com.alvarogalhardo.engram.data.db.entity.ReviewLog

@Database(
    entities = [Deck::class, Card::class, ReviewLog::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao
}
