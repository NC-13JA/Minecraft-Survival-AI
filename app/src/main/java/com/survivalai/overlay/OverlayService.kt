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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var panel: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        createNotification()
        if (!SettingsCompat.canDraw(this)) { stopSelf(); return }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panel = buildPanel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        layoutParams = WindowManager.LayoutParams(
            dp(232), dp(470), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(10); y = dp(90) }
        panel?.let { windowManager?.addView(it, layoutParams) }
    }

    private fun buildPanel(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 27, 43)); cornerRadius = dp(18).toFloat()
                setStroke(dp(2), Color.rgb(52, 190, 125))
            }
            elevation = dp(12).toFloat()
        }
        val header = TextView(this).apply {
            text = "⚔  SURVIVAL AI   ⋮"
            textSize = 14f; setTextColor(Color.WHITE)
            setPadding(dp(7), dp(5), dp(7), dp(8))
        }
        root.addView(header); attachDrag(header)
        root.addView(TextView(this).apply {
            text = "مساعد Minecraft Bedrock • أدوات سريعة"
            textSize = 10f; setTextColor(Color.rgb(164, 199, 216))
            setPadding(dp(7), 0, dp(7), dp(7))
        })
        val scroll = ScrollView(this)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        actions.addView(section("اللعب"))
        actions.addView(makeButton("فتح Minecraft  🎮", Color.rgb(29, 130, 77)) { launchMinecraft() })
        actions.addView(makeButton("فتح المساعد الذكي  ✨", Color.rgb(38, 116, 145)) {
            openAssistant("حلل وضعي الحالي في Minecraft Bedrock، واسألني عن المعلومات الناقصة قبل تقديم نصيحة.")
        })
        actions.addView(section("مساعدات البقاء"))
        actions.addView(makeButton("الخطوة التالية المناسبة", Color.rgb(31, 105, 132)) {
            openAssistant("اقترح لي خطوة واحدة تالية مناسبة في وضع Survival، مع خطوات تنفيذ واضحة.")
        })
        actions.addView(makeButton("جمع الموارد والتعدين ⛏", Color.rgb(94, 91, 48)) {
            openAssistant("ضع لي خطة آمنة لجمع الخشب والحجر والحديد والفحم، وحدد الأدوات والأولويات.")
        })
        actions.addView(makeButton("الطعام والزراعة 🍞", Color.rgb(104, 82, 39)) {
            openAssistant("ساعدني في تأمين الطعام في Minecraft Bedrock، واقترح أسرع طريقة مناسبة لمرحلتي.")
        })
        actions.addView(makeButton("القتال والحماية 🛡", Color.rgb(119, 62, 55)) {
            openAssistant("جهز لي خطة للقتال والحماية حسب مواردي، واذكر ما يجب صنعه قبل المواجهة.")
        })
        actions.addView(makeButton("القرى والمعالم 🧭", Color.rgb(72, 83, 122)) {
            openAssistant("ساعدني في العثور على قرية أو معلم قريب. لا تخترع إحداثيات؛ اطلب Seed وإحداثياتي أو بيانات موثوقة عند الحاجة.")
        })
        actions.addView(makeButton("الاستعداد للـ Nether 🔥", Color.rgb(128, 67, 42)) {
            openAssistant("أعطني قائمة تجهيز ودخول آمن إلى Nether في Bedrock، وفق مواردي الحالية.")
        })
        actions.addView(section("أدوات الشاشة"))
        actions.addView(makeButton("تصوير الشاشة  📷", Color.rgb(35, 105, 150)) { requestCapture() })
        actions.addView(makeButton("إخفاء اللوحة  −", Color.rgb(65, 76, 91)) {
            panel?.let { runCatching { windowManager?.removeView(it) } }
            panel = null
            stopSelf()
        })
        scroll.addView(actions)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return root
    }

    private fun section(label: String) = TextView(this).apply {
        text = label; textSize = 11f; setTextColor(Color.rgb(111, 225, 160))
        setPadding(dp(7), dp(9), dp(7), dp(2))
    }

    private fun launchMinecraft() {
        val launch = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(launch) }.onFailure { toast("تعذر فتح Minecraft") }
        } else toast("Minecraft Bedrock غير مثبتة")
    }

    private fun openAssistant(question: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) { toast("تعذر فتح SurvivalAI"); return }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        launch.putExtra("com.survivalai.EXTRA_QUICK_PROMPT", question)
        runCatching { startActivity(launch) }.onFailure { toast("تعذر فتح المساعد") }
    }

    private fun requestCapture() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) { toast("تعذر فتح SurvivalAI"); return }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        launch.putExtra("com.survivalai.EXTRA_CAPTURE_SCREEN", true)
        runCatching { startActivity(launch) }.onFailure { toast("تعذر فتح شاشة التصوير") }
    }

    private fun makeButton(label: String, color: Int, action: () -> Unit) =
        Button(this).apply {
            text = label; textSize = 11f; isAllCaps = false
            setTextColor(Color.WHITE)
            setPadding(dp(4), 0, dp(4), 0)
            minHeight = dp(42)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(color))
            setOnClickListener { action() }
        }

    private fun toast(message: String) =
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun attachDrag(handle: View) {
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        handle.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX=params.x; startY=params.y; touchX=event.rawX; touchY=event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x=(startX-(event.rawX-touchX).toInt()).coerceAtLeast(0)
                    params.y=(startY+(event.rawY-touchY).toInt()).coerceAtLeast(0)
                    runCatching { windowManager?.updateViewLayout(panel, params) }; true
                }
                MotionEvent.ACTION_UP -> true
                else -> true
            }
        }
    }

    private fun createNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "survival_ai_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            manager.createNotificationChannel(NotificationChannel(channelId, "SurvivalAI Overlay", NotificationManager.IMPORTANCE_LOW))
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, channelId).setContentTitle("SurvivalAI يعمل").setContentText("أدوات المساعد العائم جاهزة").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        else @Suppress("DEPRECATION") Notification.Builder(this).setContentTitle("SurvivalAI يعمل").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        startForeground(41, notification)
    }

    override fun onDestroy() {
        panel?.let { runCatching { windowManager?.removeView(it) } }
        panel = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

private object SettingsCompat {
    fun canDraw(service: Service): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.provider.Settings.canDrawOverlays(service) else true
}
