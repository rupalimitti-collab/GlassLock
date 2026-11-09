package com.glassui.lock

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = FrameLayout(this)
        root.addView(WallpaperView(this))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(28f).toInt(), dp(40f).toInt(), dp(28f).toInt(), dp(40f).toInt())
        }
        col.addView(text("Glass Lock", 34f))
        col.addView(text("Liquid-glass lock screen + control panel", 14f))
        col.addView(text("Drag the liquid slider:", 13f).apply { setPadding(0, dp(30f).toInt(), 0, dp(10f).toInt()) })
        col.addView(GlassSlider(this, false).apply { value = 40 }, LinearLayout.LayoutParams(-1, dp(44f).toInt()))
        col.addView(button("1 · Allow display over other apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        col.addView(button("2 · Start Glass Lock service") {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            startForegroundService(Intent(this, LockService::class.java))
        })
        col.addView(button("Stop service") { stopService(Intent(this, LockService::class.java)) })
        col.addView(button("Preview lock screen") { startActivity(Intent(this, LockActivity::class.java)) })
        col.addView(text("Tip: Settings > Battery > Glass Lock > Unrestricted, so One UI doesn't stop the service.", 11f)
            .apply { setPadding(0, dp(24f).toInt(), 0, 0) })
        root.addView(col)
        setContentView(root)
    }

    private fun text(t: String, sp: Float) = TextView(this).apply {
        text = t; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(Color.WHITE); gravity = Gravity.CENTER
    }

    private fun button(t: String, onTap: () -> Unit) = text(t, 15f).apply {
        background = glass(this@MainActivity, dp(26f), 70)
        setPadding(dp(16f).toInt(), dp(14f).toInt(), dp(16f).toInt(), dp(14f).toInt())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14f).toInt() }
        setOnClickListener { bounce(this); onTap() }
    }
}
