package com.survivalai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.survivalai.ai.GroqClient
import com.survivalai.overlay.OverlayService
import com.survivalai.security.ApiKeyStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_SurvivalAI)
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
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("أهلاً بك أيها الناجي! شغّل Minecraft ثم افتح المساعد العائم للحصول على إرشادات أثناء اللعب.") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("جاهز للمغامرة") }

    val quickPrompts = listOf(
        "ما أفضل خطوة أبدأ بها الآن في عالم Survival؟",
        "أعطني خطة سريعة لجمع الحديد وصنع الدرع.",
        "كيف أستعد لدخول Nether بأمان؟",
        "اشرح لي بناء مزرعة طعام بسيطة في Bedrock."
    )

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF49E36F),
            secondary = Color(0xFF35C8F5),
            background = Color(0xFF071526),
            surface = Color(0xFF10243A)
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF071526), Color(0xFF123A50), Color(0xFF071526)))
            )
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("MINECRAFT", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8BEA9A), fontWeight = FontWeight.Black)
                    Text("SURVIVAL AI", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("رفيقك الذكي في عالم البقاء", color = Color(0xFFB7D4E8))
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC16334B)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ابدأ المغامرة", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("شغّل اللعبة مباشرة، ثم افتح النافذة العائمة فوقها.", color = Color(0xFFC4D8E8))
                        Button(
                            onClick = {
                                val launch = context.packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
                                if (launch != null) {
                                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launch)
                                } else status = "لم يتم العثور على Minecraft. ثبّت Minecraft Bedrock أولاً."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18B94D))
                        ) { Text("العب الآن  🎮", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = {
                                if (Settings.canDrawOverlays(context)) {
                                    context.startForegroundService(Intent(context, OverlayService::class.java))
                                    status = "النافذة العائمة قيد التشغيل"
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                    status = "امنح إذن الظهور فوق التطبيقات ثم عد إلى هنا"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تشغيل المساعد العائم") }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10243A)), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("اتصال Groq AI", style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                            status = "تم حفظ المفتاح على الجهاز"
                        }, modifier = Modifier.fillMaxWidth()) { Text("حفظ المفتاح") }
                        Text("للاختبار الشخصي. لا ترفع المفتاح إلى GitHub أو تشاركه مع أحد.", color = Color(0xFFB7D4E8), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10243A)), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("اسأل المساعد", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        quickPrompts.forEach { question ->
                            OutlinedButton(
                                onClick = {
                                    if (apiKey.isBlank()) { status = "أدخل مفتاح Groq أولاً"; return@OutlinedButton }
                                    busy = true
                                    scope.launch {
                                        val result = GroqClient.ask(apiKey.trim(), model.trim(), question)
                                        answer = result.getOrElse { "تعذر الاتصال: ${it.message ?: "خطأ غير معروف"}" }
                                        busy = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(question) }
                        }
                        OutlinedTextField(
                            value = prompt, onValueChange = { prompt = it },
                            label = { Text("سؤال اختياري") }, modifier = Modifier.fillMaxWidth(), minLines = 2
                        )
                        Button(
                            enabled = !busy && apiKey.isNotBlank() && prompt.isNotBlank(),
                            onClick = {
                                busy = true
                                scope.launch {
                                    val result = GroqClient.ask(apiKey.trim(), model.trim(), prompt)
                                    answer = result.getOrElse { "تعذر الاتصال: ${it.message ?: "خطأ غير معروف"}" }
                                    busy = false
                                }
                            }, modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "يفكر..." else "إرسال السؤال") }
                        Text(answer, color = Color.White)
                    }
                }
                Text("الحالة: $status", color = Color(0xFF9AC3D8))
                Text("الإصدار 0.2.0 Alpha • Bedrock companion", color = Color(0xFF6F91A8), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
