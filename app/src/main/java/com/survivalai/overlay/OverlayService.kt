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
import android.widget.SeekBar
import android.widget.TextView

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var panel: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var minimized = false
    private var settingsOpen = false
    private var pinned = false
    private val prefs by lazy { getSharedPreferences("overlay_settings", MODE_PRIVATE) }
    private var panelWidth = 232
    private var panelHeight = 470
    private var panelAlpha = 95

    override fun onCreate() {
        super.onCreate()
        createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) { stopSelf(); return }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panelWidth = prefs.getInt("width", 232).coerceIn(190, 330)
        panelHeight = prefs.getInt("height", 470).coerceIn(260, 700)
        panelAlpha = prefs.getInt("alpha", 95).coerceIn(45, 100)
        pinned = prefs.getBoolean("pinned", false)
        panel = buildPanel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        layoutParams = WindowManager.LayoutParams(dp(panelWidth), dp(panelHeight), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = dp(10); y = dp(90) }
        panel?.let { windowManager?.addView(it, layoutParams) }
    }

    private fun buildPanel(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(8), dp(9), dp(9))
            background = GradientDrawable().apply { setColor(Color.rgb(10,27,43)); cornerRadius=dp(18).toFloat(); setStroke(dp(2),Color.rgb(52,190,125)) }
            elevation=dp(12).toFloat(); alpha=panelAlpha/100f
        }
        if (minimized) {
            val icon = TextView(this).apply {
                text = "⚔"
                textSize = 27f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                contentDescription = "إعادة فتح نافذة Survival AI"
                // Keep the icon tap available to its click listener.
                // attachDrag consumes ACTION_UP and suppresses performClick.
                setOnClickListener {
                    minimized = false
                    rebuildPanel()
                }
            }
            root.addView(icon, LinearLayout.LayoutParams(-1, -1))
            return root
        }
        val header = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply { text="⚔ SURVIVAL AI"; textSize=13f; setTextColor(Color.WHITE); setPadding(dp(5),dp(5),dp(4),dp(5)) }
        header.addView(title, LinearLayout.LayoutParams(0,dp(40),1f)); attachDrag(title)
        header.addView(smallControl("−") { minimized=true; rebuildPanel() })
        header.addView(smallControl("＋") { minimized=false; panelWidth=330; panelHeight=650; persistSize(); rebuildPanel() })
        header.addView(smallControl(if(pinned) "📌" else "📍") { pinned=!pinned; prefs.edit().putBoolean("pinned",pinned).apply(); rebuildPanel() })
        header.addView(smallControl("⚙") { settingsOpen=!settingsOpen; rebuildPanel() })
        root.addView(header)
        root.addView(TextView(this).apply { text=if(pinned) "مثبّت • السحب معطّل" else "مساعد Minecraft Bedrock • اسحب العنوان للتحريك"; textSize=9f; setTextColor(Color.rgb(164,199,216)); setPadding(dp(6),0,dp(6),dp(5)) })
        if (settingsOpen) addSettings(root)
        val scroll=ScrollView(this)
        val actions=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        actions.addView(section("اللعب"))
        actions.addView(makeButton("فتح Minecraft  🎮",0xFF1D824D.toInt()){launchMinecraft()})
        actions.addView(makeButton("فتح المساعد الذكي  ✨",0xFF267491.toInt()){openAssistant("حلل وضعي الحالي في Minecraft Bedrock، واسألني عن المعلومات الناقصة قبل تقديم نصيحة.")})
        actions.addView(section("مساعدات البقاء"))
        actions.addView(makeButton("الخطوة التالية المناسبة",0xFF1F6984.toInt()){openAssistant("اقترح لي خطوة واحدة تالية مناسبة في وضع Survival، مع خطوات تنفيذ واضحة.")})
        actions.addView(makeButton("جمع الموارد والتعدين ⛏",0xFF5E5B30.toInt()){openAssistant("ضع لي خطة آمنة لجمع الخشب والحجر والحديد والفحم، وحدد الأدوات والأولويات.")})
        actions.addView(makeButton("الطعام والزراعة 🍞",0xFF685227.toInt()){openAssistant("ساعدني في تأمين الطعام في Minecraft Bedrock، واقترح أسرع طريقة مناسبة لمرحلتي.")})
        actions.addView(makeButton("القتال والحماية 🛡",0xFF773E37.toInt()){openAssistant("جهز لي خطة للقتال والحماية حسب مواردي، واذكر ما يجب صنعه قبل المواجهة.")})
        actions.addView(makeButton("القرى والمعالم 🧭",0xFF48537A.toInt()){openAssistant("ساعدني في العثور على قرية أو معلم قريب. لا تخترع إحداثيات؛ اطلب Seed وإحداثياتي أو بيانات موثوقة عند الحاجة.")})
        actions.addView(makeButton("الاستعداد للـ Nether 🔥",0xFF80432A.toInt()){openAssistant("أعطني قائمة تجهيز ودخول آمن إلى Nether في Bedrock، وفق مواردي الحالية.")})
        actions.addView(section("أدوات الشاشة"))
        actions.addView(makeButton("📷 التقاط الشاشة وتحليلها بالذكاء الاصطناعي",0xFF236996.toInt()){requestCapture()})
        actions.addView(makeButton("إخفاء اللوحة",0xFF414C5B.toInt()){if(!pinned) stopSelf() else toast("ألغِ التثبيت أولاً لإخفاء اللوحة")})
        scroll.addView(actions); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        return root
    }

    private fun addSettings(root: LinearLayout) {
        root.addView(section("إعدادات النافذة"))
        root.addView(settingRow("العرض: ${panelWidth}dp",190,330,panelWidth-190){panelWidth=190+it; persistSize(); applyWindowSize()})
        root.addView(settingRow("الارتفاع: ${panelHeight}dp",260,700,panelHeight-260){panelHeight=260+it; persistSize(); applyWindowSize()})
        root.addView(settingRow("الشفافية: $panelAlpha%",45,100,panelAlpha-45){panelAlpha=45+it; prefs.edit().putInt("alpha",panelAlpha).apply(); panel?.alpha=panelAlpha/100f})
    }
    private fun settingRow(label:String,min:Int,max:Int,initial:Int,onChange:(Int)->Unit): LinearLayout {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val text=TextView(this).apply { this.text=label; textSize=10f; setTextColor(Color.WHITE) }
        box.addView(text)
        box.addView(SeekBar(this).apply { this.max=max-min; progress=initial; setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar:SeekBar?,progress:Int,fromUser:Boolean){if(fromUser){onChange(progress); text.text=when(label.substringBefore(":")){"العرض"->"العرض: ${190+progress}dp";"الارتفاع"->"الارتفاع: ${260+progress}dp";else->"الشفافية: ${45+progress}%"} }}
            override fun onStartTrackingTouch(seekBar:SeekBar?){}; override fun onStopTrackingTouch(seekBar:SeekBar?){}
        })})
        return box
    }
    private fun smallControl(label:String,action:()->Unit)=TextView(this).apply { text=label; textSize=17f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(dp(5),dp(7),dp(5),dp(7)); setOnClickListener{action()} }
    private fun rebuildPanel(){ val old=panel; val params=layoutParams; if(old!=null) runCatching{windowManager?.removeView(old)}; panel=buildPanel(); if(params!=null){params.width=dp(if(minimized)58 else panelWidth);params.height=dp(if(minimized)58 else panelHeight);runCatching{windowManager?.addView(panel,params)}} }
    private fun applyWindowSize(){layoutParams?.let{it.width=dp(panelWidth);it.height=dp(panelHeight);runCatching{windowManager?.updateViewLayout(panel,it)}}}
    private fun persistSize(){prefs.edit().putInt("width",panelWidth).putInt("height",panelHeight).apply()}
    private fun section(label:String)=TextView(this).apply{text=label;textSize=11f;setTextColor(Color.rgb(111,225,160));setPadding(dp(7),dp(9),dp(7),dp(2))}
    private fun launchMinecraft(){val i=packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe");if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);runCatching{startActivity(i)}.onFailure{toast("تعذر فتح Minecraft")}}else toast("Minecraft Bedrock غير مثبتة")}
    private fun openAssistant(question:String){val i=packageManager.getLaunchIntentForPackage(packageName)?:return toast("تعذر فتح SurvivalAI");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP);i.putExtra("com.survivalai.EXTRA_QUICK_PROMPT",question);runCatching{startActivity(i)}}
    private fun requestCapture(){val i=packageManager.getLaunchIntentForPackage(packageName)?:return toast("تعذر فتح SurvivalAI");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP);i.putExtra("com.survivalai.EXTRA_CAPTURE_SCREEN",true);runCatching{startActivity(i)}}
    private fun makeButton(label:String,color:Int,action:()->Unit)=Button(this).apply{text=label;textSize=10f;isAllCaps=false;setTextColor(Color.WHITE);setPadding(dp(3),0,dp(3),0);minHeight=dp(40);setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));setOnClickListener{action()}}
    private fun toast(message:String)=android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_SHORT).show()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun attachDrag(handle:View){var sx=0;var sy=0;var tx=0f;var ty=0f;handle.setOnTouchListener{_,e->val p=layoutParams?:return@setOnTouchListener false
        when(e.action){MotionEvent.ACTION_DOWN->{sx=p.x;sy=p.y;tx=e.rawX;ty=e.rawY;true}
        MotionEvent.ACTION_MOVE->{if(!pinned){p.x=(sx-(e.rawX-tx).toInt()).coerceAtLeast(0);p.y=(sy+(e.rawY-ty).toInt()).coerceAtLeast(0);runCatching{windowManager?.updateViewLayout(panel,p)}};true}
        MotionEvent.ACTION_UP->true;else->true}}}
    private fun createNotification(){val m=getSystemService(NotificationManager::class.java);val id="survival_ai_overlay";if(Build.VERSION.SDK_INT>=26)m.createNotificationChannel(NotificationChannel(id,"SurvivalAI Overlay",NotificationManager.IMPORTANCE_LOW));val n=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,id).setContentTitle("SurvivalAI يعمل").setContentText("أدوات المساعد العائم جاهزة").setSmallIcon(android.R.drawable.ic_dialog_info).build()else @Suppress("DEPRECATION") Notification.Builder(this).setContentTitle("SurvivalAI يعمل").setSmallIcon(android.R.drawable.ic_dialog_info).build();startForeground(41,n)}
    override fun onDestroy(){panel?.let{runCatching{windowManager?.removeView(it)}};panel=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}