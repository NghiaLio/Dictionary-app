package com.dictionary.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dictionary.app.ui.screen.HomeScreen
import com.dictionary.app.ui.screen.SavedWordsScreen
import com.dictionary.app.ui.screen.WordDetailScreen
import com.dictionary.app.ui.screen.WordOfTheDayScreen
import com.dictionary.app.ui.screen.SettingsScreen
import com.dictionary.app.viewmodel.DictionaryViewModel
import com.dictionary.app.viewmodel.SavedWordsViewModel
import com.dictionary.app.viewmodel.WordOfTheDayViewModel
import com.dictionary.app.viewmodel.SettingsViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    dictionaryViewModel: DictionaryViewModel,
    savedWordsViewModel: SavedWordsViewModel,
    wordOfTheDayViewModel: WordOfTheDayViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            val apiKey = settingsViewModel.geminiApiKey.collectAsState().value
            LaunchedEffect(apiKey) {
                wordOfTheDayViewModel.loadWordOfTheDay(apiKey)
            }
            HomeScreen(
                viewModel = dictionaryViewModel,
                wordOfTheDayViewModel = wordOfTheDayViewModel,
                onNavigateToDetail = { word ->
                    navController.navigate(Screen.WordDetail.createRoute(word))
                }
            )
        }
        
        composable(route = Screen.SavedWords.route) {
            SavedWordsScreen(
                viewModel = savedWordsViewModel,
                onNavigateToDetail = { word ->
                    navController.navigate(Screen.WordDetail.createRoute(word))
                }
            )
        }


        composable(route = Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel
            )
        }
        
        composable(
            route = Screen.WordDetail.route,
            arguments = listOf(
                navArgument("word") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val word = backStackEntry.arguments?.getString("word") ?: ""
            val apiKey = settingsViewModel.geminiApiKey.collectAsState().value

            // Sync API Key to DictionaryViewModel so AI requests work in WordDetailScreen
            LaunchedEffect(word, apiKey) {
                dictionaryViewModel.geminiApiKey = apiKey
                if (word.isNotBlank()) {
                    dictionaryViewModel.searchWord(word)
                }
            }

            WordDetailScreen(
                viewModel = dictionaryViewModel,
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }
    }
}
