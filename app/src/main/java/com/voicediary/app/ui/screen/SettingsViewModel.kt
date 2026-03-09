package com.voicediary.app.ui.screen

import androidx.lifecycle.ViewModel
import com.voicediary.app.data.settings.AppSettings
import com.voicediary.app.data.settings.SttEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings
) : ViewModel() {

    val sttEngine: StateFlow<SttEngine> = appSettings.sttEngine

    fun setSttEngine(engine: SttEngine) {
        appSettings.setSttEngine(engine)
    }
}
