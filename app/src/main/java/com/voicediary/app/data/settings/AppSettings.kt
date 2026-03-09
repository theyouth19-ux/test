package com.voicediary.app.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SttEngine(val label: String) {
    DEVICE("기기 음성인식 (Android)"),
    WHISPER("OpenAI Whisper API")
}

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _sttEngine = MutableStateFlow(loadSttEngine())
    val sttEngine: StateFlow<SttEngine> = _sttEngine.asStateFlow()

    private fun loadSttEngine(): SttEngine {
        val name = prefs.getString("stt_engine", SttEngine.DEVICE.name) ?: SttEngine.DEVICE.name
        return try { SttEngine.valueOf(name) } catch (_: Exception) { SttEngine.DEVICE }
    }

    fun setSttEngine(engine: SttEngine) {
        prefs.edit().putString("stt_engine", engine.name).apply()
        _sttEngine.value = engine
    }
}
