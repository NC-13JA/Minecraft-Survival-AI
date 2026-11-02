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
import com.survivalai.security.ApiKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var panel: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(12); y = dp(110) }
        panel?.let { windowManager?.addView(it, layoutParams) }
    }

    private fun buildPanel(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(8), dp(9), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(13, 29, 48)); cornerRadius = dp(16).toFloat()
                setStroke(dp(2), Color.rgb(48, 183, 220))
            }
            elevation = dp(10).toFloat()
        }
        val header = TextView(this).apply {
            text = "☰  SurvivalAI"
            textSize = 13f; setTextColor(Color.WHITE)
            setPadding(dp(8), dp(4), dp(8), dp(6))
        }
        root.addView(header); attachDrag(header)
        root.addView(Button(this).apply {
            text = "فتح Minecraft 🎮"; textSize = 12f; isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30, 105, 145)))
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { startActivity(launch) }
                } else {
                    android.widget.Toast.makeText(this@OverlayService, "Minecraft Bedrock غير مثبتة", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        })
        return root
    }

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
            Notification.Builder(this, channelId).setContentTitle("SurvivalAI يعمل").setContentText("زر فتح Minecraft العائم نشط").setSmallIcon(android.R.drawable.ic_dialog_info).build()
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

private object SettingsCompat {
    fun canDraw(service: Service): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.provider.Settings.canDrawOverlays(service) else true
}
