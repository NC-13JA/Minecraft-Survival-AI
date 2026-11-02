package com.survivalai.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GroqClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun ask(apiKey: String, model: String, prompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(apiKey.startsWith("gsk_")) { "تحقق من مفتاح Groq." }
                require(model.isNotBlank()) { "اسم النموذج مطلوب." }
                val payload = JSONObject().apply {
                    put("model", model)
                    put("messages", org.json.JSONArray().put(JSONObject().apply {
                        put("role", "system")
                        put("content", "أنت Minecraft Survival AI، مساعد عربي خبير في Minecraft Bedrock. قدم خطوات عملية قصيرة، ولا تخلط معلومات Java مع Bedrock. إذا لم تعرف إصدار اللعبة فاذكر أن المعلومة قد تختلف.")
                    }).put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }))
                    put("temperature", 0.6)
                    put("max_completion_tokens", 700)
                }
                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                        error(message?.takeIf { it.isNotBlank() } ?: "Groq HTTP ${response.code}")
                    }
                    val content = JSONObject(body).getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").optString("content")
                    content.ifBlank { "لم يصل رد نصي من النموذج." }
                }
            }
        }
}
