package com.voicediary.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    entryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 삭제 완료 시 목록으로 이동
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("기록 삭제") },
            text = { Text("이 기록을 삭제하시겠습니까?\n삭제된 기록은 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry()
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("상세 보기") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val entry = uiState.entry
        if (entry == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("기록을 찾을 수 없습니다", style = MaterialTheme.typography.titleMedium)
            }
            return@Scaffold
        }

        val isReviewed = entry.reviewStatus == "reviewed"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 상단: type + 날짜 + 뱃지 ---
            EntryHeader(entry = entry)

            // --- 오디오 플레이어 ---
            AudioPlayerCard(
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                onTogglePlayback = viewModel::togglePlayback,
                onSeek = viewModel::seekTo
            )

            // --- 교정된 텍스트 (편집 가능) ---
            CorrectedTextSection(
                text = uiState.editedCorrectedText,
                onTextChange = viewModel::onCorrectedTextChange,
                readOnly = isReviewed
            )

            // --- 원본 텍스트 (접기/펼치기) ---
            RawTextSection(
                rawText = entry.rawTranscript,
                isExpanded = uiState.isRawTextExpanded,
                onToggle = viewModel::toggleRawTextExpanded
            )

            // --- 액션 버튼들 ---
            ActionButtons(
                isReviewed = isReviewed,
                isSaved = uiState.isSaved,
                hasTextChanged = uiState.editedCorrectedText != entry.correctedText,
                onSave = viewModel::saveCorrectedText,
                onToggleReview = viewModel::toggleReviewStatus,
                onDelete = { showDeleteDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EntryHeader(entry: com.voicediary.app.data.local.VoiceEntry) {
    val timeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    val typeIcon = if (entry.type == "diary") "\uD83D\uDCD4" else "\uD83D\uDCDD"
    val typeLabel = if (entry.type == "diary") "일기" else "메모"
    val isReviewed = entry.reviewStatus == "reviewed"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = typeIcon, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeFormat.format(Date(entry.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // reviewStatus 뱃지
        val badgeBg = if (isReviewed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        val badgeText = if (isReviewed) Color(0xFF2E7D32) else Color(0xFFE65100)
        val badgeLabel = if (isReviewed) "검토완료" else "검토전"

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeBg
        ) {
            Text(
                text = badgeLabel,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = badgeText
            )
        }
    }
}

@Composable
private fun AudioPlayerCard(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "음성 녹음",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 재생/일시정지 버튼
                IconButton(onClick = onTogglePlayback) {
                    if (isPlaying) {
                        // Pause icon using text
                        Text(
                            text = "\u23F8",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "재생",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 프로그레스바
                val progress = if (durationMs > 0) {
                    currentPositionMs.toFloat() / durationMs.toFloat()
                } else 0f

                Slider(
                    value = progress,
                    onValueChange = { fraction ->
                        onSeek((fraction * durationMs).toLong())
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 현재시간 / 전체시간
                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CorrectedTextSection(
    text: String,
    onTextChange: (String) -> Unit,
    readOnly: Boolean
) {
    Column {
        Text(
            text = "교정된 텍스트",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            minLines = 4,
            maxLines = 10,
            shape = RoundedCornerShape(12.dp),
            label = if (readOnly) {{ Text("검토 완료 — 편집 불가") }} else null
        )
    }
}

@Composable
private fun RawTextSection(
    rawText: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "원본 텍스트",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "접기" else "펼치기"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = rawText.ifBlank { "(원본 텍스트 없음)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isReviewed: Boolean,
    isSaved: Boolean,
    hasTextChanged: Boolean,
    onSave: () -> Unit,
    onToggleReview: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 수정 저장 버튼 (검토 완료 시 숨김)
        if (!isReviewed) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasTextChanged && !isSaved,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSaved) "저장 완료" else "수정 저장")
            }
        }

        // 검토 완료 / 검토 취소 버튼
        if (isReviewed) {
            OutlinedButton(
                onClick = onToggleReview,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("검토 취소")
            }
        } else {
            Button(
                onClick = onToggleReview,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                )
            ) {
                Text("검토 완료")
            }
        }

        // 삭제 버튼
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("삭제")
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
