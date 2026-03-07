package com.voicediary.app.data.recording

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * SpeechRecognizer 래퍼.
 * 침묵으로 인한 자동 종료 시 자동 재시작하여 정지 버튼을 누를 때까지 계속 인식.
 */
class SpeechRecognitionManager(
    private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldRestart = false

    private var onPartialResult: ((String) -> Unit)? = null
    private var onFinalResult: ((String) -> Unit)? = null
    private var onError: ((Int) -> Unit)? = null

    // 전체 세션에 걸쳐 누적된 최종 텍스트 세그먼트들
    private val finalSegments = mutableListOf<String>()

    fun setListeners(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Int) -> Unit = {}
    ) {
        this.onPartialResult = onPartialResult
        this.onFinalResult = onFinalResult
        this.onError = onError
    }

    fun startListening() {
        finalSegments.clear()
        shouldRestart = true
        createAndStartRecognizer()
    }

    fun stopListening(): String {
        shouldRestart = false
        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) { }
        destroyRecognizer()
        return finalSegments.joinToString(" ").trim()
    }

    fun release() {
        shouldRestart = false
        isListening = false
        destroyRecognizer()
    }

    private fun createAndStartRecognizer() {
        destroyRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError?.invoke(SpeechRecognizer.ERROR_CLIENT)
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 침묵 감지 시간을 길게 설정
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L)
        }

        isListening = true
        speechRecognizer?.startListening(intent)
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return

            // 누적된 세그먼트 + 현재 partial을 합쳐서 콜백
            val accumulated = buildAccumulatedText(partial)
            onPartialResult?.invoke(accumulated)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            if (!text.isNullOrBlank()) {
                finalSegments.add(text)
            }

            val accumulated = finalSegments.joinToString(" ").trim()
            onFinalResult?.invoke(accumulated)

            // 아직 녹음 중이면 자동 재시작
            if (shouldRestart) {
                createAndStartRecognizer()
            }
        }

        override fun onError(error: Int) {
            // 자동 재시작 가능한 에러: 침묵 타임아웃, 음성 없음 등
            val restartableErrors = setOf(
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            )

            if (shouldRestart && error in restartableErrors) {
                createAndStartRecognizer()
            } else if (shouldRestart) {
                onError?.invoke(error)
                // 네트워크 등 일시적 에러도 재시도
                createAndStartRecognizer()
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun buildAccumulatedText(currentPartial: String): String {
        return if (finalSegments.isEmpty()) {
            currentPartial
        } else {
            "${finalSegments.joinToString(" ")} $currentPartial".trim()
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        speechRecognizer = null
    }
}
