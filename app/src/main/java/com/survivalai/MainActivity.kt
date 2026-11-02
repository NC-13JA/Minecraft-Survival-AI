package com.survivalai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val captureIntent = Intent(context, com.survivalai.capture.ScreenCaptureService::class.java).apply {
                putExtra(com.survivalai.capture.ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(com.survivalai.capture.ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) context.startForegroundService(captureIntent)
            else context.startService(captureIntent)
            status = "تم منح إذن الشاشة؛ يجري التقاط صورة واحدة."
        } else {
            status = "أُلغي إذن التقاط الشاشة."
        }
    }
    var hasKey by remember { mutableStateOf(!ApiKeyStore.read(context).isNullOrBlank()) }
    var setupKey by remember { mutableStateOf("") }
    var changeKey by remember { mutableStateOf("") }
    var showKeySettings by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("openai/gpt-oss-120b") }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("أهلاً بك أيها الناجي! اختر مساعدة سريعة أو اكتب سؤالك.") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("جاهز للمغامرة") }
    val worldPrefs = remember { context.getSharedPreferences("survival_world", android.content.Context.MODE_PRIVATE) }
    var worldSeed by remember { mutableStateOf(worldPrefs.getString("seed", "") ?: "") }
    var worldX by remember { mutableStateOf(worldPrefs.getString("x", "") ?: "") }
    var worldY by remember { mutableStateOf(worldPrefs.getString("y", "") ?: "") }
    var worldZ by remember { mutableStateOf(worldPrefs.getString("z", "") ?: "") }
    var worldNotes by remember { mutableStateOf(worldPrefs.getString("notes", "") ?: "") }
    var goalText by remember { mutableStateOf(worldPrefs.getString("goal", "إنشاء مأوى آمن وجمع الطعام") ?: "") }
    var reminderInterval by remember { mutableStateOf(worldPrefs.getInt("interval", 30)) }
    var dataSaver by remember { mutableStateOf(worldPrefs.getBoolean("data_saver", true)) }
    var explorationRadius by remember { mutableIntStateOf(worldPrefs.getInt("exploration_radius", 500).coerceIn(500, 2000)) }

    fun worldContext(): String = """
        بيانات عالم اللاعب:
        Seed: ${worldSeed.ifBlank { "غير معروف" }}
        الإحداثيات المدخلة: X=${worldX.ifBlank { "غير معروفة" }}, Y=${worldY.ifBlank { "غير معروفة" }}, Z=${worldZ.ifBlank { "غير معروفة" }}
        الهدف الحالي: ${goalText.ifBlank { "لم يحدد" }}
        ملاحظات العالم والمواقع: ${worldNotes.ifBlank { "لا توجد ملاحظات" }}
        نطاق الاستكشاف حول اللاعب: ${explorationRadius} بلوكة في جميع الاتجاهات.
        هذا النطاق إعداد مبدئي للتحليل التدريجي، وليس دليلاً على أن الخريطة حُللت فعلياً.
        فترة المراقبة المطلوبة: ${reminderInterval} ثانية.
        وضع توفير البيانات: ${if (dataSaver) "مفعّل" else "متوقف"}.
        لا تفترض أن Seed أو الإحداثيات صحيحة إن لم يقدمها اللاعب، ولا تخترع أي موقع.
    """.trimIndent()

    fun saveWorldProfile() {
        worldPrefs.edit()
            .putString("seed", worldSeed.trim())
            .putString("x", worldX.trim()).putString("y", worldY.trim()).putString("z", worldZ.trim())
            .putString("notes", worldNotes.trim()).putString("goal", goalText.trim())
            .putInt("interval", reminderInterval).putBoolean("data_saver", dataSaver)
            .putInt("exploration_radius", explorationRadius).apply()
        status = "حُفظت بيانات العالم والأهداف على الهاتف"
    }

    val quickPrompts = listOf(
        "ما أفضل خطوة أبدأ بها الآن في عالم Survival؟",
        "أعطني خطة سريعة لجمع الحديد وصنع الدرع.",
        "كيف أستعد لدخول Nether بأمان؟",
        "اشرح لي بناء مزرعة طعام بسيطة في Bedrock."
    )

    fun launchMinecraft() {
        val launch = context.packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(launch) }
                .onFailure { status = "تعذر فتح Minecraft: ${it.message}" }
        } else {
            status = "Minecraft Bedrock غير مثبتة أو لا تملك أيقونة تشغيل متاحة."
        }
    }

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
            if (!hasKey) {
                Column(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("SURVIVAL AI", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("إعداد المساعد لأول مرة", color = Color(0xFF8BEA9A), style = MaterialTheme.typography.titleLarge)
                    Text("أدخل مفتاح Groq API مرة واحدة. سيُحفظ مشفّراً على هاتفك ولن تحتاج إلى إدخاله عند كل تشغيل.", color = Color(0xFFB7D4E8))
                    OutlinedTextField(
                        value = setupKey, onValueChange = { setupKey = it.trim() },
                        label = { Text("مفتاح Groq API") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = setupKey.startsWith("gsk_"),
                        onClick = {
                            ApiKeyStore.save(context, setupKey)
                            setupKey = ""
                            hasKey = true
                            status = "تم حفظ المفتاح المشفر على الجهاز"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("حفظ ومتابعة") }
                    Text("المفتاح يبقى محلياً ولا يُرسل إلى GitHub. لا تشاركه مع أي شخص.", color = Color(0xFF9AB8CC), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("MINECRAFT", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8BEA9A), fontWeight = FontWeight.Black)
                        Text("SURVIVAL AI", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("رفيقك الذكي في عالم البقاء", color = Color(0xFFB7D4E8))
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC16334B)), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("ابدأ المغامرة", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text("أدخل Seed عالمك واحفظه قبل فتح Minecraft.", color = Color(0xFFC4D8E8))
                            OutlinedTextField(
                                value = worldSeed,
                                onValueChange = { worldSeed = it },
                                label = { Text("World Seed") },
                                placeholder = { Text("أدخل Seed العالم هنا") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedButton(onClick = {
                                saveWorldProfile()
                                val seedToCopy = worldSeed.trim()
                                if (seedToCopy.isNotEmpty()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Minecraft World Seed", seedToCopy))
                                    status = "حُفظ Seed ونُسخ. الصقه في Chunkbase."
                                } else {
                                    status = "لم تدخل Seed؛ فُتحت Chunkbase لإدخاله هناك."
                                }
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chunkbase.com/apps/seed-map")))
                                }.onFailure { status = "تعذر فتح صفحة Chunkbase: ${it.message}" }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("تحليل Seed على Chunkbase")
                            }
                            Button(onClick = {
                                saveWorldProfile()
                                launchMinecraft()
                            }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18B94D))) {
                                Text("حفظ Seed وفتح Minecraft 🎮", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val manager = context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                    captureLauncher.launch(manager.createScreenCaptureIntent())
                                }, modifier = Modifier.fillMaxWidth()
                            ) { Text("التقاط صورة من شاشة Minecraft") }
                            OutlinedButton(
                                onClick = {
                                    if (Settings.canDrawOverlays(context)) {
                                        context.startForegroundService(Intent(context, OverlayService::class.java))
                                        status = "تم طلب تشغيل النافذة العائمة"
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                        status = "امنح إذن الظهور فوق التطبيقات ثم ارجع للتطبيق"
                                    }
                                }, modifier = Modifier.fillMaxWidth()
                            ) { Text("تشغيل المساعد العائم") }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10243A)), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("إعدادات الذكاء الاصطناعي", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("المفتاح محفوظ ومشفّر على هذا الجهاز.", color = Color(0xFF9ED9AE))
                            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("اسم النموذج") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { showKeySettings = !showKeySettings; changeKey = "" }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (showKeySettings) "إخفاء تغيير المفتاح" else "تغيير مفتاح Groq")
                            }
                            if (showKeySettings) {
                                OutlinedTextField(value = changeKey, onValueChange = { changeKey = it.trim() },
                                    label = { Text("المفتاح الجديد") }, visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                Button(enabled = changeKey.startsWith("gsk_"), onClick = {
                                    ApiKeyStore.save(context, changeKey)
                                    changeKey = ""
                                    showKeySettings = false
                                    status = "تم تحديث المفتاح المشفر"
                                }, modifier = Modifier.fillMaxWidth()) { Text("حفظ المفتاح الجديد") }
                            }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC16334B)), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("ملف عالمك وذاكرة النجاة", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("تُحفظ هذه البيانات محلياً وتُرفق تلقائياً بأسئلتك.", color = Color(0xFFB7D4E8))
                            Text("Seed محفوظ محلياً ويُرفق تلقائياً بطلبات المساعد.", color = Color(0xFFB7D4E8), style = MaterialTheme.typography.bodySmall)
                            Text("نطاق الاستكشاف حول موقع اللاعب: ${explorationRadius} بلوكة", color = Color.White)
                            Slider(
                                value = explorationRadius.toFloat(),
                                onValueChange = { explorationRadius = (it.toInt() / 250) * 250 },
                                valueRange = 500f..2000f,
                                steps = 5
                            )
                            Text("يمكن تغيير النطاق من 500 إلى 2000 بلوكة، والإعداد الافتراضي 500.", color = Color(0xFFB7D4E8), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = worldX, onValueChange = { worldX = it }, label = { Text("X") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = worldY, onValueChange = { worldY = it }, label = { Text("Y") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = worldZ, onValueChange = { worldZ = it }, label = { Text("Z") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            OutlinedTextField(value = goalText, onValueChange = { goalText = it }, label = { Text("هدفك الحالي") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(value = worldNotes, onValueChange = { worldNotes = it }, label = { Text("ملاحظات ومواقع مهمة (المنزل، قرية، منجم...)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Text("فترة المراقبة: $reminderInterval ثانية", color = Color.White)
                            Slider(value = reminderInterval.toFloat(), onValueChange = { reminderInterval = it.toInt() }, valueRange = 15f..120f, steps = 6)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = dataSaver, onCheckedChange = { dataSaver = it })
                                Text("توفير البيانات (تقليل التحليلات غير الضرورية)", color = Color.White)
                            }
                            Button(onClick = { saveWorldProfile() }, modifier = Modifier.fillMaxWidth()) { Text("حفظ ملف العالم") }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10243A)), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("اسأل المساعد", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            OutlinedButton(enabled = !busy, onClick = {
                                busy = true
                                scope.launch {
                                    val nextPrompt = worldContext() + "\n\n" +
                                        "حلل سياق اللاعب الحالي واقترح خطوة واحدة تالية واضحة وقابلة للتنفيذ في Minecraft Bedrock Survival. اذكر سبب اختيارها وما يحتاجه اللاعب. إذا كانت معلومات حالة العالم ناقصة فاطلب صورة شاشة أو معلومة محددة ولا تخمن."
                                    val result = GroqClient.ask(ApiKeyStore.read(context).orEmpty(), model.trim(), nextPrompt)
                                    answer = result.getOrElse { "تعذر الاتصال: ${it.message ?: "خطأ غير معروف"}" }
                                    busy = false
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("أعطني الخطوة التالية") }
                            quickPrompts.forEach { question ->
                                OutlinedButton(enabled = !busy, onClick = {
                                    busy = true
                                    scope.launch {
                                        val result = GroqClient.ask(ApiKeyStore.read(context).orEmpty(), model.trim(), worldContext() + "\n\n" + question)
                                        answer = result.getOrElse { "تعذر الاتصال: ${it.message ?: "خطأ غير معروف"}" }
                                        busy = false
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text(question) }
                            }
                            OutlinedTextField(value = prompt, onValueChange = { prompt = it },
                                label = { Text("سؤال اختياري") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Button(enabled = !busy && prompt.isNotBlank(), onClick = {
                                busy = true
                                val question = prompt
                                val seedMatch = Regex("(?i)(?:world\\\\s*seed|seed|بذرة العالم|سيد)\\\\s*[:=：]?\\\\s*(-?\\\\d{1,20})").find(question)
                                if (seedMatch != null) {
                                    worldSeed = seedMatch.groupValues[1]
                                    worldPrefs.edit().putString("seed", worldSeed).apply()
                                }
                                scope.launch {
                                    val seedInstruction = if (seedMatch != null)
                                        "\nتم التعرف على Seed من رسالة اللاعب وحفظه محلياً: ${worldSeed}. استخدمه عند تحليل العالم، ولا تعتبره إحداثيات."
                                    else ""
                                    val result = GroqClient.ask(ApiKeyStore.read(context).orEmpty(), model.trim(), worldContext() + seedInstruction + "\n\n" + question)
                                    answer = result.getOrElse { "تعذر الاتصال: ${it.message ?: "خطأ غير معروف"}" }
                                    busy = false
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "يفكر..." else "إرسال السؤال") }
                            Text(answer, color = Color.White)
                        }
                    }
                    Text("الحالة: $status", color = Color(0xFF9AC3D8))
                    Text("الإصدار 0.4.0 Alpha • Bedrock companion", color = Color(0xFF6F91A8), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
