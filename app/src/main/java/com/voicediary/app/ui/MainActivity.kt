package com.voicediary.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.voicediary.app.ui.navigation.VoiceDiaryNavHost
import com.voicediary.app.ui.theme.VoiceDiaryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 위젯에서 전달된 녹음 타입
        val widgetRecordType = intent?.getStringExtra("record_type")

        setContent {
            VoiceDiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceDiaryNavHost(initialRecordType = widgetRecordType)
                }
            }
        }
    }
}
