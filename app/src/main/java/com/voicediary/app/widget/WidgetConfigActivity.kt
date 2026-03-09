package com.voicediary.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicediary.app.ui.theme.VoiceDiaryTheme

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 취소 시 결과
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            VoiceDiaryTheme {
                var selectedType by remember { mutableStateOf("diary") }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("위젯 설정") })
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "위젯을 탭하면 바로 녹음을 시작합니다.\n기록 유형을 선택하세요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 일기 선택
                            Card(
                                onClick = { selectedType = "diary" },
                                modifier = Modifier.weight(1f).height(120.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedType == "diary")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("일기", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // 메모 선택
                            Card(
                                onClick = { selectedType = "memo" },
                                modifier = Modifier.weight(1f).height(120.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedType == "memo")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.StickyNote2,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("메모", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { confirmWidget(selectedType) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("위젯 추가", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    private fun confirmWidget(type: String) {
        QuickRecordWidget.saveWidgetType(this, appWidgetId, type)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        QuickRecordWidget.updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
