package com.dictionary.app.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dictionary.app.ui.component.SearchBar
import com.dictionary.app.ui.component.SuggestionItem
import com.dictionary.app.ui.component.WordOfTheDayCard
import com.dictionary.app.ui.component.SuggestionWordSkeleton
import com.dictionary.app.util.SpeechToTextHelper
import com.dictionary.app.viewmodel.DictionaryViewModel
import com.dictionary.app.viewmodel.WordOfTheDayViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@Composable
fun HomeScreen(
    viewModel: DictionaryViewModel,
    wordOfTheDayViewModel: WordOfTheDayViewModel,
    onNavigateToDetail: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val historyList by viewModel.recentSearches.collectAsState()
    val dailyWordState by wordOfTheDayViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var isListening by remember { mutableStateOf(false) }
    
    val sttHelper = remember {
        SpeechToTextHelper(
            context = context,
            onResult = { text -> viewModel.onQueryChange(text) },
            onListeningStateChange = { listening -> isListening = listening },
            onError = { /* Handle error */ }
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sttHelper.startListening("en")
        }
    }

    DisposableEffect(Unit) {
        onDispose { sttHelper.destroy() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // Content with standard padding
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dictionary",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Search words, meanings and examples",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar with reduced horizontal padding to make it "longer"
        SearchBar(
            query = uiState.query,
            onQueryChange = { viewModel.onQueryChange(it) },
            onSearch = { word ->
                onNavigateToDetail(word, true)
            },
            onMicClick = {
                if (isListening) {
                    sttHelper.stopListening()
                } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        sttHelper.startListening("en")
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            isListening = isListening,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Suggestions Dropdown or Screen Content Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when {
                uiState.suggestions.isNotEmpty() -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(uiState.suggestions) { suggestion ->
                                SuggestionItem(
                                    word = suggestion,
                                    onClick = {
                                        viewModel.clearSuggestions()
                                        onNavigateToDetail(suggestion, true)
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
                
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "Not found",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error ?: "Search failed.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Fuzzy spelling correction
                        uiState.fuzzySuggestion?.let { suggestion ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Did you mean: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.onQueryChange(suggestion)
                                        onNavigateToDetail(suggestion, true)
                                    }
                                )
                            }
                        }
                    }
                }
                
                else -> {
                    // Default State: Suggestion Chips, History and Word of the Day
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Category suggestion chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Idioms", "Phrasal Verbs", "Slang").forEach { label ->
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(99.dp))
                                        .clickable {
                                            viewModel.onQueryChange(label)
                                            onNavigateToDetail(label, true)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Recent Searches Title
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        if (historyList.isEmpty()) {
                            Text(
                                text = "Start searching to build your history",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                historyList.take(5).forEach { historyItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onNavigateToDetail(historyItem.word, true)
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "History Icon",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = historyItem.word,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { viewModel.deleteHistoryItem(historyItem.word) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove History",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Suggestion Word Section
                        Text(
                            text = "Suggestion Word",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (dailyWordState.isLoading) {
                            SuggestionWordSkeleton()
                        } else if (dailyWordState.error != null) {
                            // Show AI error if it occurred
                            Text(
                                text = "AI Error: ${dailyWordState.error}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        } else if (dailyWordState.word.isNotBlank()) {
                            WordOfTheDayCard(
                                word = dailyWordState.word,
                                phonetic = dailyWordState.phonetic,
                                meaning = dailyWordState.meaning,
                                example = dailyWordState.example,
                                onClick = { onNavigateToDetail(dailyWordState.word, false) },
                                onPlayAudio = {
                                    viewModel.playAudio(dailyWordState.audioUrl)
                                }
                            )
                        } else {
                            Text(
                                text = "Loading Suggestion Word...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
