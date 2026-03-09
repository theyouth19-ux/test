package com.voicediary.app.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onSignInResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        viewModel.checkSignInStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Google Drive 백업") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 상태 아이콘
            Icon(
                imageVector = if (uiState.isSignedIn) Icons.Filled.Backup else Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = if (uiState.isSignedIn) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 계정 상태 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiState.isSignedIn) "연결됨" else "연결 안됨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.accountEmail != null) {
                        Text(
                            text = uiState.accountEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 로그인/로그아웃 버튼
            if (uiState.isSignedIn) {
                OutlinedButton(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Google 계정 연결 해제")
                }
            } else {
                Button(
                    onClick = { signInLauncher.launch(viewModel.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Google 계정 연결")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 백업 버튼
            Button(
                onClick = { viewModel.startBackup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.isSignedIn && !uiState.isBackingUp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("백업 중...")
                } else {
                    Icon(Icons.Filled.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("지금 백업하기", style = MaterialTheme.typography.titleMedium)
                }
            }

            // 설명
            Text(
                text = "모든 일기/메모 데이터와 오디오 파일을\nGoogle Drive에 백업합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // 결과 메시지
            AnimatedVisibility(visible = uiState.lastResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isError)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isError) Icons.Filled.Error
                            else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (uiState.isError)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.lastResult ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isError)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
