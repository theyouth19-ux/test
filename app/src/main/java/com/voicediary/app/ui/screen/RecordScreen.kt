package com.voicediary.app.ui.screen

import android.Manifest
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordScreen(
    type: String,
    onNavigateBack: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = if (type == "diary") "일기 녹음" else "메모 녹음"

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (!micPermissionState.status.isGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PermissionRequestContent(
                    shouldShowRationale = micPermissionState.status.shouldShowRationale,
                    onRequestPermission = { micPermissionState.launchPermissionRequest() }
                )
            }
        } else {
            RecordingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                isRecording = uiState.isRecording,
                elapsedSeconds = uiState.elapsedSeconds,
                hasRecording = uiState.filePath != null && !uiState.isRecording,
                partialTranscript = uiState.partialTranscript,
                onStartRecording = viewModel::startRecording,
                onStopRecording = viewModel::stopRecording,
                onSave = viewModel::saveEntry
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (shouldShowRationale) {
                "음성 녹음을 위해 마이크 권한이 필요합니다."
            } else {
                "녹음을 시작하려면 마이크 권한을 허용해주세요."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("권한 허용하기")
        }
    }
}

@Composable
private fun RecordingContent(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    elapsedSeconds: Long,
    hasRecording: Boolean,
    partialTranscript: String,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSave: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 녹음 상태 표시
        if (isRecording) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(blinkAlpha)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "녹음 중",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = if (hasRecording) "녹음 완료" else "준비됨",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 타이머
        Text(
            text = formatElapsedTime(elapsedSeconds),
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            color = if (isRecording) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 녹음 시작/정지 버튼
        if (isRecording) {
            Button(
                onClick = onStopRecording,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "녹음 정지",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        } else {
            Button(
                onClick = onStartRecording,
                modifier = Modifier.size(80.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "녹음 시작",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // 저장 버튼
        if (hasRecording) {
            Spacer(modifier = Modifier.height(24.dp))
            FilledTonalButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("저장하기")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 실시간 텍스트 표시 영역
        TranscriptPanel(
            transcript = partialTranscript,
            isRecording = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun TranscriptPanel(
    transcript: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // 텍스트가 변경될 때 자동 스크롤
    LaunchedEffect(transcript) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "음성 인식 텍스트",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (transcript.isBlank()) {
                    Text(
                        text = if (isRecording) "음성을 인식하고 있습니다..." else "녹음을 시작하면 여기에 텍스트가 표시됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = transcript,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    )
                }
            }
        }
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
