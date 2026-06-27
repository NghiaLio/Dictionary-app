package com.dictionary.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dictionary.app.util.SpeechToTextHelper
import com.dictionary.app.util.TtsHelper
import com.dictionary.app.viewmodel.TranslationViewModel

@Composable
fun TranslationScreen(
    viewModel: TranslationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Initialize TTS Helper
    val ttsHelper = remember { TtsHelper(context) }

    var isListening by remember { mutableStateOf(false) }

    // Initialize STT Helper
    val sttHelper = remember {
        SpeechToTextHelper(
            context = context,
            onResult = { text -> viewModel.onSourceTextChange(text) },
            onListeningStateChange = { listening -> isListening = listening },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    val languages = listOf(
        Language("English", "en"),
        Language("Vietnamese", "vi"),
        Language("Japanese", "ja"),
        Language("Chinese", "zh"),
        Language("French", "fr"),
        Language("Korean", "ko"),
        Language("German", "de")
    )

    var sourceLang by remember { mutableStateOf(languages[0]) }
    var targetLang by remember { mutableStateOf(languages[1]) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sttHelper.startListening(sourceLang.code)
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
            sttHelper.destroy()
        }
    }

    // Mic pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val onTranslateClick = {
        if (uiState.sourceText.isNotBlank()) {
            viewModel.translate(sourceLang.code, targetLang.code)
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Language Selector Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source Language
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { sourceExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(sourceLang.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.name) },
                                onClick = {
                                    sourceLang = lang
                                    sourceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Swap Button
                IconButton(
                    onClick = {
                        val tempLang = sourceLang
                        sourceLang = targetLang
                        targetLang = tempLang
                        viewModel.swapLanguages(uiState.sourceText, uiState.targetText)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap Languages",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Target Language
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { targetExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(targetLang.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = targetExpanded,
                        onDismissRequest = { targetExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.name) },
                                onClick = {
                                    targetLang = lang
                                    targetExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Text Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)) {
                TextField(
                    value = uiState.sourceText,
                    onValueChange = { viewModel.onSourceTextChange(it) },
                    placeholder = { Text("Enter text to translate...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onTranslateClick() }
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isListening) {
                                sttHelper.stopListening()
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    sttHelper.startListening(sourceLang.code)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.then(
                            if (isListening) Modifier.scale(micScale) else Modifier
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = { 
                        if (uiState.sourceText.isNotBlank()) {
                            ttsHelper.speak(uiState.sourceText, sourceLang.code)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Output Text Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)) {
                        Text(
                            text = if (uiState.error != null) uiState.error!! else uiState.targetText.ifEmpty { "Translation will appear here" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = when {
                                    uiState.error != null -> MaterialTheme.colorScheme.error
                                    uiState.targetText.isEmpty() -> MaterialTheme.colorScheme.outline
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { 
                                if (uiState.targetText.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(uiState.targetText))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { 
                                if (uiState.targetText.isNotBlank()) {
                                    ttsHelper.speak(uiState.targetText, targetLang.code)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onTranslateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = uiState.sourceText.isNotBlank() && !uiState.isLoading
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Translate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

data class Language(val name: String, val code: String)
