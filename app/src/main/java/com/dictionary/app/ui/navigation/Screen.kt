package com.dictionary.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SavedWords : Screen("saved_words")
    object WordOfTheDay : Screen("word_of_the_day")
    object Settings : Screen("settings")
    object WordDetail : Screen("word_detail/{word}") {
        fun createRoute(word: String): String = "word_detail/$word"
    }
}
