package com.voicediary.app.data.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class RecordingManager @Inject constructor(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null

    private val recordingsDir: File
        get() = File(context.filesDir, "recordings").apply { mkdirs() }

    fun startRecording(type: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${type}_${timestamp}.m4a"
        val file = File(recordingsDir, fileName)
        currentFilePath = file.absolutePath

        recorder = createMediaRecorder().apply {
            // VOICE_COMMUNICATION을 사용하여 SpeechRecognizer와 마이크 공유 가능
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        return file.absolutePath
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            currentFilePath
        } catch (e: RuntimeException) {
            recorder?.release()
            recorder = null
            currentFilePath?.let { File(it).delete() }
            null
        }
    }

    fun release() {
        try {
            recorder?.release()
        } catch (_: Exception) { }
        recorder = null
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
