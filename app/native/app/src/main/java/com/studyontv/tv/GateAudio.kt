package com.studyontv.tv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent

/**
 * Keep background video quiet while an actual quiz gate is visible.
 * Use an idempotent PAUSE rather than PLAY_PAUSE, which could restart a paused video.
 */
internal class GateAudio(context: Context) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val listener = AudioManager.OnAudioFocusChangeListener { }
    private var holding = false
    private var focusGranted = false
    private var focusRequest: AudioFocusRequest? = null

    fun pauseForGate() {
        if (holding) return
        holding = true

        try {
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        } catch (e: Exception) {
            Log.w("StudyOnTV", "Media pause could not be dispatched", e)
        }

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                    .setOnAudioFocusChangeListener(listener)
                    .build()
                if (manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    focusRequest = request
                    focusGranted = true
                }
            } else {
                @Suppress("DEPRECATION")
                val result = manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                focusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w("StudyOnTV", "Audio focus could not be acquired", e)
        }
    }

    fun release() {
        if (!holding) return
        holding = false
        if (!focusGranted) return
        focusGranted = false
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                focusRequest?.let { manager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(listener)
            }
        } catch (e: Exception) {
            Log.w("StudyOnTV", "Audio focus release failed", e)
        }
    }
}
