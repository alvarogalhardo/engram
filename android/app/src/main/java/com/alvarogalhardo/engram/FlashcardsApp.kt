package com.alvarogalhardo.engram

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.alvarogalhardo.engram.data.db.AppDatabase
import com.alvarogalhardo.engram.data.media.MediaFiles
import com.alvarogalhardo.engram.data.repo.DeckRepository
import com.alvarogalhardo.engram.data.repo.StatsRepository
import com.alvarogalhardo.engram.data.repo.StudyRepository
import com.alvarogalhardo.engram.data.settings.SettingsStore
import com.alvarogalhardo.engram.importer.ApkgImporter
import com.alvarogalhardo.engram.util.SystemTimeProvider

class FlashcardsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val clock = SystemTimeProvider()
    val db: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "flashcards.db").build()
    val settings = SettingsStore(context)
    val mediaFiles = MediaFiles(context)
    val deckRepo = DeckRepository(db.deckDao(), db.cardDao(), clock)
    val studyRepo = StudyRepository(db.cardDao(), db.reviewLogDao(), settings, clock)
    val statsRepo = StatsRepository(db.reviewLogDao(), clock)
    val importer = ApkgImporter(context, db, mediaFiles, clock)
}
