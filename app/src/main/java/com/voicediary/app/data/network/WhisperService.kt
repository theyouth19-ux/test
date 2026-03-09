package com.voicediary.app.data.network

import com.voicediary.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    /**
     * 오디오 파일을 Whisper API에 전송하여 STT 결과를 반환한다.
     * 실패 시 null 반환.
     */
    suspend fun transcribe(audioFilePath: String): WhisperResult? {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) return null

        val file = File(audioFilePath)
        if (!file.exists()) return null

        return try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", "ko")
                .addFormDataPart("response_format", "json")
                .build()

            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            // OkHttp synchronous call wrapped in suspend via Dispatchers.IO in caller
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            response.close()

            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val text = json.optString("text", "")
                if (text.isNotBlank()) {
                    WhisperResult(text = text)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

data class WhisperResult(
    val text: String
)
