package com.alvarogalhardo.engram.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alvarogalhardo.engram.AppContainer
import com.alvarogalhardo.engram.FlashcardsApp
import com.alvarogalhardo.engram.ui.deckdetail.DeckDetailScreen
import com.alvarogalhardo.engram.ui.decks.DeckListScreen
import com.alvarogalhardo.engram.ui.editor.EditorScreen
import com.alvarogalhardo.engram.ui.review.ReviewScreen
import com.alvarogalhardo.engram.ui.settings.SettingsScreen
import com.alvarogalhardo.engram.ui.stats.StatsScreen

object Routes {
    const val DECKS = "decks"
    const val DECK = "deck/{deckId}"
    const val EDITOR = "editor/{deckId}?cardId={cardId}"
    const val REVIEW = "review/{deckId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun deck(id: Long) = "deck/$id"
    fun editor(deckId: Long, cardId: Long? = null) =
        "editor/$deckId" + (cardId?.let { "?cardId=$it" } ?: "")
    fun review(deckId: Long) = "review/$deckId"
}

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as FlashcardsApp).container

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DECKS) {
        composable(Routes.DECKS) {
            DeckListScreen(
                onOpenDeck = { nav.navigate(Routes.deck(it)) },
                onOpenStats = { nav.navigate(Routes.STATS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.DECK,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val deckId = entry.arguments!!.getLong("deckId")
            DeckDetailScreen(
                deckId = deckId,
                onBack = { nav.popBackStack() },
                onStudy = { nav.navigate(Routes.review(deckId)) },
                onAddCard = { nav.navigate(Routes.editor(deckId)) },
                onEditCard = { cardId -> nav.navigate(Routes.editor(deckId, cardId)) },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType },
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            EditorScreen(
                deckId = entry.arguments!!.getLong("deckId"),
                cardId = entry.arguments!!.getLong("cardId"),
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            Routes.REVIEW,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            ReviewScreen(
                deckId = entry.arguments!!.getLong("deckId"),
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.STATS) { StatsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
