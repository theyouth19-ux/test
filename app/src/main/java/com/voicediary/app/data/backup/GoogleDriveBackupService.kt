package com.voicediary.app.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val APP_FOLDER_NAME = "VoiceDiary_Backup"
    }

    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignInIntent(): Intent = getSignInClient().signInIntent

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(): Boolean = getSignedInAccount() != null

    /**
     * Google Drive에 백업 업로드.
     * 1) 앱 전용 폴더를 찾거나 생성
     * 2) JSON 파일 업로드
     * 3) 오디오 파일들 업로드
     */
    suspend fun uploadBackup(backupResult: BackupResult): UploadResult {
        val account = getSignedInAccount() ?: return UploadResult.NotSignedIn

        return withContext(Dispatchers.IO) {
            try {
                val driveService = buildDriveService(account)
                val folderId = getOrCreateFolder(driveService)

                // JSON 파일 업로드
                uploadFile(driveService, folderId, backupResult.jsonFile, "application/json")

                // 오디오 파일들 업로드
                for (audioFile in backupResult.audioFiles) {
                    uploadFile(driveService, folderId, audioFile, "audio/mp4")
                }

                UploadResult.Success(
                    fileCount = 1 + backupResult.audioFiles.size,
                    entryCount = backupResult.entryCount
                )
            } catch (e: Exception) {
                UploadResult.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun buildDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("VoiceDiary")
            .build()
    }

    private fun getOrCreateFolder(driveService: Drive): String {
        // 기존 폴더 검색
        val result = driveService.files().list()
            .setQ("name = '$APP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .setSpaces("drive")
            .execute()

        if (result.files.isNotEmpty()) {
            return result.files[0].id
        }

        // 폴더 생성
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = APP_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        val folder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()
        return folder.id
    }

    private fun uploadFile(
        driveService: Drive,
        folderId: String,
        file: File,
        mimeType: String
    ) {
        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = file.name
            parents = listOf(folderId)
        }
        val mediaContent = FileContent(mimeType, file)
        driveService.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
    }
}

sealed class UploadResult {
    data class Success(val fileCount: Int, val entryCount: Int) : UploadResult()
    data object NotSignedIn : UploadResult()
    data class Error(val message: String) : UploadResult()
}
