package com.voicediary.app.data.recording

import android.media.MediaPlayer
import javax.inject.Inject

class AudioPlayerManager @Inject constructor() {

    private var player: MediaPlayer? = null
    private var onCompletionCallback: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    fun play(filePath: String, onCompletion: () -> Unit = {}) {
        stop()
        onCompletionCallback = onCompletion
        player = MediaPlayer().apply {
            setDataSource(filePath)
            setOnCompletionListener {
                onCompletionCallback?.invoke()
            }
            prepare()
            start()
        }
    }

    fun stop() {
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) { }
        player = null
    }

    fun release() {
        stop()
        onCompletionCallback = null
    }
}
