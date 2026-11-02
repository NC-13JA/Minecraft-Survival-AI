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
            x = 16
            y = 120
        }
        panel?.let { windowManager?.addView(it, layoutParams) }
    }

    private fun buildPanel(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = GradientDrawable().apply {
                setColor(Color.rgb(13, 29, 48))
                cornerRadius = 28f
                setStroke(2, Color.rgb(48, 183, 220))
            }
            elevation = 16f
        }

        val header = TextView(this).apply {
            text = "☰   SurvivalAI     ● متصل"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 12)
        }
        root.addView(header)
        attachDrag(header)

        val gameButton = actionButton("العودة إلى Minecraft  🎮") {
            val launch = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
            } else showAnswer("لم يتم العثور على Minecraft Bedrock على هذا الجهاز.")
        }
        root.addView(gameButton)

        val scroll = ScrollView(this)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        actions.addView(actionButton("أعطني الخطوة التالية") { askGroq("أنا ألعب Minecraft Bedrock في وضع Survival. أعطني الخطوة التالية المنطقية، مع مراعاة أنني لم أرسل صورة للشاشة.") })
        actions.addView(actionButton("خطة جمع الموارد") { askGroq("ضع لي خطة عملية لجمع الخشب والطعام والحجر والحديد في Minecraft Bedrock Survival.") })
        actions.addView(actionButton("مزرعة مناسبة للمبتدئ") { askGroq("اشرح مزرعة طعام بسيطة في Minecraft Bedrock، بالمواد والأبعاد وخطوات البناء.") })
        actions.addView(actionButton("الاستعداد للتختيم") { askGroq("ضع خطة تدريجية لتجهيز نفسي لهزيمة Ender Dragon في Minecraft Bedrock.") })
        actions.addView(actionButton("تحليل صورة من اللعبة") {
            showAnswer("ميزة تحليل لقطة الشاشة تحتاج إلى إضافة إذن التقاط الشاشة وربطه بنموذج رؤية. هذه النسخة لا تلتقط الشاشة تلقائياً بعد.")
        })

        answerView = TextView(this).apply {
            text = "اختر مساعدة سريعة. ستظهر إجابة Groq هنا."
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(12, 14, 12, 14)
            background = GradientDrawable().apply {
                setColor(Color.rgb(28, 49, 69))
                cornerRadius = 18f
            }
        }
        actions.addView(answerView)
        scroll.addView(actions)
        root.addView(scroll, LinearLayout.LayoutParams(dp(300), dp(440)))

        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        footer.addView(actionButton("تصغير") { root.findViewWithTag<View>("panel_scroll")?.visibility = View.GONE })
        footer.addView(actionButton("إغلاق") { stopSelf() })
        root.addView(footer)
        scroll.tag = "panel_scroll"
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
                    params.x = startX - (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    windowManager?.updateViewLayout(panel, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun askGroq(prompt: String) {
        val key = ApiKeyStore.read(this)
        if (key.isNullOrBlank()) {
            showAnswer("افتح التطبيق وأدخل مفتاح Groq API ثم احفظه.")
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
