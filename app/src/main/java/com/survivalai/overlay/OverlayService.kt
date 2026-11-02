package com.survivalai.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.survivalai.ai.GroqClient
import com.survivalai.security.ApiKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var panel: LinearLayout? = null
    private var answerView: TextView? = null
    private var contentScroll: ScrollView? = null
    private var expandButton: Button? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotification()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panel = buildPanel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(110)
        }
        panel?.let { windowManager?.addView(it, layoutParams) }
    }

    private fun buildPanel(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.rgb(13, 29, 48))
                cornerRadius = dp(18).toFloat()
                setStroke(dp(2), Color.rgb(48, 183, 220))
            }
            elevation = dp(10).toFloat()
        }

        val header = TextView(this).apply {
            text = "☰   SurvivalAI     ● جاهز"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(12))
        }
        root.addView(header)
        attachDrag(header)

        root.addView(actionButton("فتح Minecraft 🎮") {
            val launch = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(launch) }
                    .onFailure { showAnswer("تعذر فتح Minecraft: ${it.message}") }
            } else showAnswer("لم يعثر Android على Minecraft Bedrock. تحقق من تثبيت اللعبة.")
        })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val questionInput = android.widget.EditText(this).apply {
            hint = "اكتب ما تريد..."
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            minLines = 2
            maxLines = 4
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.rgb(28, 49, 69))
                cornerRadius = dp(12).toFloat()
            }
        }
        actions.addView(questionInput, LinearLayout.LayoutParams(dp(292), LinearLayout.LayoutParams.WRAP_CONTENT))
        actions.addView(actionButton("إرسال سؤالي") {
            val question = questionInput.text?.toString()?.trim().orEmpty()
            if (question.isBlank()) showAnswer("اكتب سؤالك أولاً.")
            else askGroq("سؤال اللاعب عن Minecraft Bedrock Survival: $question")
        })
        actions.addView(actionButton("أعطني الخطوة التالية") {
            askGroq("اعتماداً على سياق المحادثة وبيانات العالم المتاحة، ما الخطوة التالية الأنسب للاعب في Minecraft Bedrock Survival؟ لا تفترض معلومات لم تظهر لك.")
        })
        actions.addView(actionButton("تحليل صورة اللعبة") {
            showAnswer("تحليل الصور قيد التجهيز. في هذه النسخة لا يتم التقاط الشاشة بعد؛ لن أدّعي قراءة الإحداثيات أو Seed دون صورة فعلية.")
        })
        answerView = TextView(this).apply {
            text = "اكتب ما تريد أو اطلب الخطوة التالية."
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.rgb(28, 49, 69))
                cornerRadius = dp(14).toFloat()
            }
        }
        actions.addView(answerView)
        root.addView(actions)
        root.addView(actionButton("إغلاق النافذة") { stopSelf() })
        return root
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30, 105, 145)))
            setOnClickListener { action() }
        }

    private fun attachDrag(handle: View) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        handle.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (startX - (event.rawX - touchX).toInt()).coerceAtLeast(0)
                    params.y = (startY + (event.rawY - touchY).toInt()).coerceAtLeast(0)
                    runCatching { windowManager?.updateViewLayout(panel, params) }
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> true
            }
        }
    }

    private fun askGroq(prompt: String) {
        val key = ApiKeyStore.read(this)
        if (key.isNullOrBlank()) {
            showAnswer("افتح التطبيق وأكمل إعداد مفتاح Groq أولاً.")
            return
        }
        showAnswer("جارٍ إرسال طلبك إلى Groq...")
        serviceScope.launch {
            val result = GroqClient.ask(key, "openai/gpt-oss-120b", prompt)
            showAnswer(result.getOrElse { "تعذر الحصول على إجابة: ${it.message ?: "تحقق من الإنترنت والمفتاح واسم النموذج."}" })
        }
    }

    private fun showAnswer(text: String) {
        answerView?.text = text
    }

    private fun createNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "survival_ai_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "SurvivalAI Overlay", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, channelId).setContentTitle("SurvivalAI يعمل").setContentText("المساعد العائم نشط").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        else @Suppress("DEPRECATION") Notification.Builder(this).setContentTitle("SurvivalAI يعمل").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        startForeground(41, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        panel?.let { runCatching { windowManager?.removeView(it) } }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
