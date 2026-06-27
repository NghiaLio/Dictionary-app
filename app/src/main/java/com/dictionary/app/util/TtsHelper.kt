package com.dictionary.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
        } else {
            Log.e("TtsHelper", "Initialization failed")
        }
    }

    fun speak(text: String, languageCode: String) {
        if (!isReady) return
        
        val locale = when (languageCode) {
            "vi" -> Locale("vi", "VN")
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "fr" -> Locale.FRENCH
            "ko" -> Locale.KOREAN
            "de" -> Locale.GERMAN
            else -> Locale.getDefault()
        }

        tts?.let {
            it.language = locale
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
