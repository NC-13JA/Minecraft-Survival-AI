package com.survivalai.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object GroqClient {
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build()
    private fun systemPrompt() = "أنت Minecraft Survival AI، مساعد عربي خبير في Minecraft Bedrock. قدم خطوات عملية قصيرة، ولا تخلط معلومات Java مع Bedrock. حلل فقط ما يظهر بالصورة، وميّز بين المؤكد والمحتمل، ولا تخترع إحداثيات أو عناصر غير واضحة."
    suspend fun ask(apiKey:String,model:String,prompt:String):Result<String> = withContext(Dispatchers.IO) { runCatching {
        require(apiKey.startsWith("gsk_")){"تحقق من مفتاح Groq."};require(model.isNotBlank()){"اسم النموذج مطلوب."}
        val payload=JSONObject().apply{put("model",model);put("messages",org.json.JSONArray().put(JSONObject().apply{put("role","system");put("content",systemPrompt())}).put(JSONObject().apply{put("role","user");put("content",prompt)}));put("temperature",0.6);put("max_completion_tokens",900)}
        execute(apiKey,payload)
    }}
    suspend fun analyzeScreenshot(apiKey:String,imageFile:File,context:String):Result<String> = withContext(Dispatchers.IO) { runCatching {
        require(apiKey.startsWith("gsk_")){"تحقق من مفتاح Groq."};require(imageFile.exists() && imageFile.length()>0){"ملف لقطة الشاشة غير موجود."}
        require(imageFile.length()<15L*1024*1024){"حجم الصورة كبير للتحليل؛ أعد الالتقاط."}
        val encoded=Base64.encodeToString(imageFile.readBytes(),Base64.NO_WRAP)
        val content=org.json.JSONArray()
            .put(JSONObject().put("type","text").put("text","حلل لقطة Minecraft Bedrock التالية. استخرج ما يمكن قراءته بوضوح: الإحداثيات إن ظهرت، القلوب، الطعام، الشريط السفلي، الأدوات، الأخطار، الموارد، والبيئة. اذكر درجة الثقة، ثم أعطني خطوة واحدة تالية. لا تخمّن القيم المخفية. سياق العالم:\n$context"))
            .put(JSONObject().put("type","image_url").put("image_url",JSONObject().put("url","data:image/jpeg;base64,$encoded")))
        val payload=JSONObject().put("model","qwen/qwen3.8-27b").put("messages",org.json.JSONArray().put(JSONObject().put("role","system").put("content",systemPrompt())).put(JSONObject().put("role","user").put("content",content))).put("temperature",0.3).put("max_completion_tokens",1200)
        execute(apiKey,payload)
    }}
    private fun execute(apiKey:String,payload:JSONObject):String {
        val request=Request.Builder().url("https://api.groq.com/openai/v1/chat/completions").header("Authorization","Bearer $apiKey").header("Content-Type","application/json").post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        return http.newCall(request).execute().use{response->val body=response.body?.string().orEmpty();if(!response.isSuccessful){val message=runCatching{JSONObject(body).optJSONObject("error")?.optString("message")}.getOrNull();error(message?.takeIf{it.isNotBlank()}?:"Groq HTTP ${response.code}")};JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content").ifBlank{"لم يصل رد نصي من النموذج."}}
    }
}