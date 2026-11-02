package com.survivalai.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.graphics.Color

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var panel: TextView? = null

    override fun onCreate() {
        super.onCreate()
        createNotification()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panel = TextView(this).apply {
            text = "SurvivalAI\nالمساعد العائم جاهز"
            setTextColor(Color.WHITE)
            setBackgroundColor(0xDD263238.toInt())
            setPadding(24, 18, 24, 18)
            textSize = 14f
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 20; y = 140 }
        windowManager?.addView(panel, params)
    }

    private fun createNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "survival_ai_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "SurvivalAI Overlay", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, channelId).setContentTitle("SurvivalAI يعمل").setContentText("النافذة العائمة نشطة").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        else @Suppress("DEPRECATION") Notification.Builder(this).setContentTitle("SurvivalAI يعمل").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        startForeground(41, notification)
    }

    override fun onDestroy() {
        panel?.let { windowManager?.removeView(it) }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
