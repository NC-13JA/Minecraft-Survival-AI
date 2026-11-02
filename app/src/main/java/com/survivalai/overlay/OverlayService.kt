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
            dp(220), WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(10); y = dp(110) }
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
            setPadding(dp(7), dp(5), dp(7), dp(9))
        }
        root.addView(header); attachDrag(header)
        root.addView(TextView(this).apply {
            text = "مساعد Minecraft Bedrock"
            textSize = 11f; setTextColor(Color.rgb(164, 199, 216))
            setPadding(dp(7), 0, dp(7), dp(7))
        })
        root.addView(makeButton("فتح Minecraft  🎮", Color.rgb(29, 130, 77)) {
            val launch = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(launch) }.onFailure { toast("تعذر فتح Minecraft") }
            } else toast("Minecraft Bedrock غير مثبتة")
        })
        root.addView(makeButton("تصوير الشاشة  📷", Color.rgb(35, 105, 150)) {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launch.putExtra("com.survivalai.EXTRA_CAPTURE_SCREEN", true)
                runCatching { startActivity(launch) }.onFailure { toast("تعذر فتح شاشة التصوير") }
            } else toast("تعذر فتح SurvivalAI")
        })
        root.addView(makeButton("إخفاء اللوحة  −", Color.rgb(65, 76, 91)) {
            panel?.let { runCatching { windowManager?.removeView(it) } }
            panel = null
            stopSelf()
        })
        return root
    }

    private fun makeButton(label: String, color: Int, action: () -> Unit) =
        Button(this).apply {
            text = label; textSize = 12f; isAllCaps = false
            setTextColor(Color.WHITE)
            setPadding(dp(4), 0, dp(4), 0)
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
            Notification.Builder(this, channelId).setContentTitle("SurvivalAI يعمل").setContentText("لوحة المساعد العائمة جاهزة").setSmallIcon(android.R.drawable.ic_dialog_info).build()
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
