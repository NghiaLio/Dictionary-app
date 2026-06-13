package com.dictionary.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class AudioPlayerHelper(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "AudioPlayerHelper"

    fun play(
        url: String, 
        onPrepared: () -> Unit = {},
        onComplete: () -> Unit = {}, 
        onError: (String) -> Unit = {}
    ) {
        try {
            // Release existing player before creating a new one
            releasePlayer()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setDataSource(url)
                
                setOnPreparedListener { mp ->
                    onPrepared()
                    mp.start()
                    Log.d(TAG, "Audio playback started: $url")
                }
                
                setOnCompletionListener {
                    onComplete()
                    releasePlayer()
                    Log.d(TAG, "Audio playback completed: $url")
                }
                
                setOnErrorListener { _, what, extra ->
                    val errorMsg = "MediaPlayer error code: $what, extra: $extra"
                    onError(errorMsg)
                    Log.e(TAG, errorMsg)
                    releasePlayer()
                    true // Error handled
                }
                
                // Load audio asynchronously to not block the main UI thread
                prepareAsync()
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown media player initialization error"
            onError(errorMsg)
            Log.e(TAG, errorMsg, e)
            releasePlayer()
        }
    }

    fun stop() {
        releasePlayer()
    }

    private fun releasePlayer() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping media player", e)
            } finally {
                player.release()
            }
        }
        mediaPlayer = null
    }
}
