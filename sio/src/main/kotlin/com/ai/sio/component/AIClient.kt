package com.ai.sio.component

import com.ai.sio.dto.AIMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class AIClient(
    @Value("\${api.key}") private val apiKey: String,
    @Value("\${api.base-url}") private val baseUrl: String,
    @Value("\${api.model}") private val model:String = "gemini-2.0-flash"
) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()

    fun generateContent(messages: List<AIMessage>): Mono<String> {

        val contents = convertToGeminiFormat(messages)

        val requestBody = mapOf("contents" to contents)

        return webClient.post()
            .uri("/models/$model:generateContent?key=$apiKey")
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { response ->
                response.bodyToMono(String::class.java).flatMap { errorBody ->
                    Mono.error(RuntimeException("Gemini API 4xx error: $errorBody"))
                }
            }
            .onStatus(HttpStatusCode::is5xxServerError) {
                Mono.error(RuntimeException("Gemini API server error"))
            }
            .bodyToMono(Map::class.java)
            .map { response -> extractGeminiContent(response) }
            .doOnError { error ->
                println("Gemini API error: ${error.message}")
            }
            .timeout(Duration.ofSeconds(30))
    }

    private fun convertToGeminiFormat(messages: List<AIMessage>): List<Map<String, Any>> {
        return messages.map { message ->
            mapOf(
                "role" to when(message.role) {
                    "user" -> "user"
                    "assistant" -> "model"  // Gemini는 "model"을 사용
                    "system" -> "user"      // Gemini는 system role이 없어서 user로 변환
                    else -> "user"
                },
                "parts" to listOf(mapOf("text" to message.content))
            )
        }
    }

    private fun extractGeminiContent(response: Map<*, *>): String {
        try {
            @Suppress("UNCHECKED_CAST")
            val candidates = response["candidates"] as? List<Map<String, Any>>
                ?: return "응답을 받을 수 없습니다"

            val firstCandidate = candidates.firstOrNull()
                ?: return "응답을 받을 수 없습니다"

            val content = firstCandidate["content"] as? Map<String, Any>
                ?: return "응답을 받을 수 없습니다"

            val parts = content["parts"] as? List<Map<String, Any>>
                ?: return "응답을 받을 수 없습니다"

            val text = parts.firstOrNull()?.get("text") as? String
                ?: return "응답을 받을 수 없습니다"

            return text
        } catch (e: Exception) {
            println("Gemini 응답 파싱 오류: ${e.message}")
            return "응답 처리 중 오류가 발생했습니다"
        }
    }

}