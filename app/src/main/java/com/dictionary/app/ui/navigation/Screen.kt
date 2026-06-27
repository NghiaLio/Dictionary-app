package com.dictionary.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SavedWords : Screen("saved_words")
    object Settings : Screen("settings")
    object Translation : Screen("translation")
    object WordDetail : Screen("word_detail/{word}?saveHistory={saveHistory}") {
        fun createRoute(word: String, saveHistory: Boolean = true): String = 
            "word_detail/$word?saveHistory=$saveHistory"
    }
}
