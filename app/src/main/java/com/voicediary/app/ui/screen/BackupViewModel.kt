package com.voicediary.app.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.backup.BackupManager
import com.voicediary.app.data.backup.GoogleDriveBackupService
import com.voicediary.app.data.backup.UploadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isSignedIn: Boolean = false,
    val accountEmail: String? = null,
    val isBackingUp: Boolean = false,
    val lastResult: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val driveService: GoogleDriveBackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        checkSignInStatus()
    }

    fun checkSignInStatus() {
        val account = driveService.getSignedInAccount()
        _uiState.value = _uiState.value.copy(
            isSignedIn = account != null,
            accountEmail = account?.email
        )
    }

    fun getSignInIntent() = driveService.getSignInIntent()

    fun onSignInResult(success: Boolean) {
        checkSignInStatus()
        if (!success) {
            _uiState.value = _uiState.value.copy(
                lastResult = "Google 로그인에 실패했습니다.",
                isError = true
            )
        }
    }

    fun signOut() {
        driveService.getSignInClient().signOut().addOnCompleteListener {
            _uiState.value = BackupUiState()
        }
    }

    fun startBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBackingUp = true,
                lastResult = null,
                isError = false
            )

            try {
                val backupResult = backupManager.createBackup()
                val uploadResult = driveService.uploadBackup(backupResult)

                when (uploadResult) {
                    is UploadResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isBackingUp = false,
                            lastResult = "${uploadResult.entryCount}개 항목, ${uploadResult.fileCount}개 파일 백업 완료!",
                            isError = false
                        )
                    }
                    is UploadResult.NotSignedIn -> {
                        _uiState.value = _uiState.value.copy(
                            isBackingUp = false,
                            lastResult = "Google 로그인이 필요합니다.",
                            isError = true
                        )
                    }
                    is UploadResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isBackingUp = false,
                            lastResult = "백업 실패: ${uploadResult.message}",
                            isError = true
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    lastResult = "백업 중 오류: ${e.message}",
                    isError = true
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(lastResult = null, isError = false)
    }
}
