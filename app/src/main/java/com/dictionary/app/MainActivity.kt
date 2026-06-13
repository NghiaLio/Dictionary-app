package com.dictionary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dictionary.app.data.local.database.AppDatabase
import com.dictionary.app.data.remote.api.RetrofitInstance
import com.dictionary.app.data.repository.DictionaryRepository
import com.dictionary.app.data.datastore.SettingsDataStore
import com.dictionary.app.ui.navigation.NavGraph
import com.dictionary.app.ui.navigation.Screen
import com.dictionary.app.ui.theme.DictionaryTheme
import com.dictionary.app.viewmodel.DictionaryViewModel
import com.dictionary.app.viewmodel.SavedWordsViewModel
import com.dictionary.app.viewmodel.WordOfTheDayViewModel
import com.dictionary.app.viewmodel.SettingsViewModel
import com.dictionary.app.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize local and network dependencies
        val database = AppDatabase.getDatabase(applicationContext)
        val settingsDataStore = SettingsDataStore(applicationContext)
        val repository = DictionaryRepository(
            dictionaryApi = RetrofitInstance.dictionaryApi,
            datamuseApi = RetrofitInstance.datamuseApi,
            geminiApi = RetrofitInstance.geminiApi,
            savedWordDao = database.savedWordDao,
            recentSearchDao = database.recentSearchDao,
            cachedWordDao = database.cachedWordDao
        )

        // 2. Build ViewModels using Custom Factory
        val factory = ViewModelFactory(application, repository, settingsDataStore)
        val dictionaryViewModel = ViewModelProvider(this, factory)[DictionaryViewModel::class.java]
        val savedWordsViewModel = ViewModelProvider(this, factory)[SavedWordsViewModel::class.java]
        val wordOfTheDayViewModel = ViewModelProvider(this, factory)[WordOfTheDayViewModel::class.java]
        val settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        // 3. Render Compose layout
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            DictionaryTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        dictionaryViewModel = dictionaryViewModel,
                        savedWordsViewModel = savedWordsViewModel,
                        wordOfTheDayViewModel = wordOfTheDayViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dictionaryViewModel: DictionaryViewModel,
    savedWordsViewModel: SavedWordsViewModel,
    wordOfTheDayViewModel: WordOfTheDayViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if bottom bar should be visible (visible on the 4 main screens)
    val showBottomBar = currentRoute == Screen.Home.route || 
            currentRoute == Screen.SavedWords.route || 
            currentRoute == Screen.WordOfTheDay.route || 
            currentRoute == Screen.Settings.route

    val navigationItems = listOf(
        NavigationItem(
            title = "Search",
            route = Screen.Home.route,
            selectedIcon = Icons.Default.Search,
            unselectedIcon = Icons.Default.Search
        ),
        NavigationItem(
            title = "Saved",
            route = Screen.SavedWords.route,
            selectedIcon = Icons.Default.Bookmark,
            unselectedIcon = Icons.Default.BookmarkBorder
        ),
        NavigationItem(
            title = "Settings",
            route = Screen.Settings.route,
            selectedIcon = Icons.Default.Settings,
            unselectedIcon = Icons.Default.Settings
        )
    )

    Scaffold(
        topBar = {
            if (showBottomBar) {
                val title = if (currentRoute == Screen.Settings.route) "Settings" else "Linguist AI"
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Open drawer or Menu */ }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Open Profile */ }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    navigationItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            label = { 
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            icon = {
                                Icon(
                                    imageVector = item.selectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                unselectedIconColor = MaterialTheme.colorScheme.outline,
                                unselectedTextColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            dictionaryViewModel = dictionaryViewModel,
            savedWordsViewModel = savedWordsViewModel,
            wordOfTheDayViewModel = wordOfTheDayViewModel,
            settingsViewModel = settingsViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
