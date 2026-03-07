package com.voicediary.app.data.network

import com.voicediary.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextCorrectionService @Inject constructor(
    private val openAiApi: OpenAiApi
) {
    companion object {
        private const val SYSTEM_PROMPT =
            "다음은 한국어 음성인식 결과입니다. " +
            "문맥상 잘못 인식된 부분을 자연스럽게 교정해주세요. " +
            "원래 의미를 최대한 보존하되, " +
            "맞춤법, 띄어쓰기, 어색한 표현만 수정하세요. " +
            "교정된 텍스트만 출력하세요."
    }

    /**
     * STT 텍스트를 AI로 교정.
     * 네트워크 오류 시 원본 텍스트를 그대로 반환.
     */
    suspend fun correctText(rawTranscript: String): CorrectionResult {
        if (rawTranscript.isBlank()) {
            return CorrectionResult(correctedText = rawTranscript, isOriginal = true)
        }

        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) {
            return CorrectionResult(correctedText = rawTranscript, isOriginal = true)
        }

        return try {
            val request = ChatCompletionRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    ChatMessage(role = "system", content = SYSTEM_PROMPT),
                    ChatMessage(role = "user", content = rawTranscript)
                ),
                temperature = 0.3
            )

            val response = openAiApi.chatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            val corrected = response.choices.firstOrNull()?.message?.content?.trim()

            if (!corrected.isNullOrBlank()) {
                CorrectionResult(correctedText = corrected, isOriginal = false)
            } else {
                CorrectionResult(correctedText = rawTranscript, isOriginal = true)
            }
        } catch (_: Exception) {
            // 네트워크 오류 등 → rawTranscript 그대로 사용
            CorrectionResult(correctedText = rawTranscript, isOriginal = true)
        }
    }
}

data class CorrectionResult(
    val correctedText: String,
    val isOriginal: Boolean
)
