package com.survivalai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.survivalai.overlay.OverlayService
import com.survivalai.security.ApiKeyStore
import com.survivalai.ai.GroqClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SurvivalApp() }
    }
}

@Composable
private fun SurvivalApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf(ApiKeyStore.read(context).orEmpty()) }
    var model by remember { mutableStateOf("openai/gpt-oss-120b") }
    var prompt by remember { mutableStateOf("أنا ألعب Minecraft Bedrock في وضع Survival. أعطني الخطوة التالية المناسبة للمبتدئ.") }
    var answer by remember { mutableStateOf("مرحباً! أنا مساعدك لتختيم Minecraft Bedrock.") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("جاهز") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Minecraft Survival AI", style = MaterialTheme.typography.headlineSmall)
                Text("مساعد عربي للعبة Minecraft Bedrock", style = MaterialTheme.typography.bodyMedium)
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إعداد Groq", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = apiKey, onValueChange = { apiKey = it },
                            label = { Text("مفتاح Groq API") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = model, onValueChange = { model = it },
                            label = { Text("اسم النموذج") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            ApiKeyStore.save(context, apiKey.trim())
                            status = "تم حفظ المفتاح على هذا الجهاز"
                        }, modifier = Modifier.fillMaxWidth()) { Text("حفظ المفتاح") }
                        Text("للاختبار الشخصي فقط: الاتصال المباشر من التطبيق لا يجعل المفتاح محمياً بالكامل.")
                    }
                }
                OutlinedTextField(
                    value = prompt, onValueChange = { prompt = it },
                    label = { Text("اسأل مساعد Minecraft") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3
                )
                Button(
                    enabled = !busy && apiKey.isNotBlank(),
                    onClick = {
                        busy = true
                        status = "جارٍ الاتصال بـ Groq..."
                        scope.launch {
                            val result = GroqClient.ask(apiKey.trim(), model.trim(), prompt)
                            answer = result.getOrElse { "حدث خطأ: ${it.message ?: "تعذر الاتصال"}" }
                            status = if (result.isSuccess) "تم الرد" else "فشل الطلب"
                            busy = false
                        }
                    }, modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "جارٍ التفكير..." else "اسأل المساعد") }
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("رد المساعد", style = MaterialTheme.typography.titleMedium)
                        Text(answer)
                    }
                }
                Text("حالة التطبيق: $status")
                Button(onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        context.startForegroundService(Intent(context, OverlayService::class.java))
                        status = "تم طلب تشغيل النافذة العائمة"
                    } else {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")))
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("تفعيل / تشغيل النافذة العائمة") }
                Text("قراءة الشاشة والإحداثيات وخرائط Seed ستضاف في المراحل التالية.")
            }
        }
    }
}
