package com.studyontv.tv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    private val handler = Handler(Looper.getMainLooper())
    private var originallyMuted = false
    private var originalVolume = 0
    private var fallbackMuted = false
    private val silence = object : Runnable {
        override fun run() {
            if (!holding) return
            try {
                if (manager.isMusicActive && !originallyMuted) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (!manager.isStreamMute(AudioManager.STREAM_MUSIC)) {
                            manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                            fallbackMuted = true
                        }
                    } else if (!fallbackMuted) {
                        manager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                        fallbackMuted = true
                    }
                }
            } catch (e: Exception) { Log.w("StudyOnTV", "Gate audio fallback failed", e) }
            handler.postDelayed(this, 1000)
        }
    }

    fun pauseForGate() {
        if (holding) return
        holding = true
        originalVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        originallyMuted = if (Build.VERSION.SDK_INT >= 23) manager.isStreamMute(AudioManager.STREAM_MUSIC) else originalVolume == 0
        fallbackMuted = false

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
        handler.postDelayed(silence, 600)
    }

    fun release() {
        if (!holding) return
        holding = false
        handler.removeCallbacks(silence)
        if (fallbackMuted && !originallyMuted) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                } else manager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            } catch (e: Exception) { Log.w("StudyOnTV", "Gate audio restore failed", e) }
        }
        fallbackMuted = false
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
