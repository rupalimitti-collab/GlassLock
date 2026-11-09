package com.glassui.lock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.dynamicanimation.animation.DynamicAnimation

class LockActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var panel: FrameLayout
    private lateinit var audio: AudioManager
    private lateinit var cam: CameraManager
    private var open = false
    private var torch = false
    private var downY = 0f

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setShowWhenLocked(true)
        window.setDecorFitsSystemWindows(false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.insetsController?.hide(WindowInsets.Type.systemBars())
        audio = getSystemService(AudioManager::class.java)
        cam = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        root = FrameLayout(this)
        root.addView(WallpaperView(this))

        val clock = TextClock(this).apply {
            format12Hr = "h\nmm"; format24Hr = "H\nmm"
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 130f)
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            includeFontPadding = false; setLineSpacing(0f, 0.8f); scaleY = 1.45f
        }
        clock.post {
            clock.paint.shader = LinearGradient(0f, 0f, 0f, clock.height.toFloat(), 0xFFFFFFFF.toInt(), 0xCCF2D9A0.toInt(), Shader.TileMode.CLAMP)
            clock.invalidate()
        }
        root.addView(clock, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(120f).toInt() })

        val date = TextClock(this).apply {
            format12Hr = "EEE MMM d"; format24Hr = "EEE MMM d"
            setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }
        root.addView(date, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(70f).toInt() })

        val hint = tv("Swipe up to unlock  •  swipe down from top for controls", 12f)
        root.addView(hint, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(44f).toInt() })

        val flash = circle("🔦", 50f) { setTorch(it) }
        root.addView(flash, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START).apply { leftMargin = dp(30f).toInt(); bottomMargin = dp(60f).toInt() })

        val bar = View(this).apply { background = glass(this@LockActivity, dp(3f), 230) }
        root.addView(bar, FrameLayout.LayoutParams(dp(134f).toInt(), dp(5f).toInt(), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(14f).toInt() })

        buildPanel()
        root.addView(panel, FrameLayout.LayoutParams(-1, -1))
        root.setOnTouchListener(swipe)
        setContentView(root)
    }

    private val swipe = View.OnTouchListener { _, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> downY = e.rawY
            MotionEvent.ACTION_UP -> {
                val dy = e.rawY - downY; val t = dp(90f)
                if (!open && dy > t && downY < root.height * 0.35f) toggle(true)
                else if (!open && dy < -t) finish()
                else if (open && dy < -t * 0.7f) toggle(false)
            }
        }
        true
    }

    private fun toggle(show: Boolean) {
        open = show
        spring(panel, DynamicAnimation.TRANSLATION_Y, if (show) 0f else -root.height.toFloat() - 50f, 0.75f, 180f)
    }

    private fun buildPanel() {
        panel = FrameLayout(this).apply { isClickable = true; translationY = -3000f; setOnTouchListener(swipe) }
        val wall = WallpaperView(this)
        if (Build.VERSION.SDK_INT >= 31) wall.setRenderEffect(RenderEffect.createBlurEffect(70f, 70f, Shader.TileMode.CLAMP))
        panel.addView(wall)
        panel.addView(View(this).apply { setBackgroundColor(Color.argb(150, 20, 24, 36)) })

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f).toInt(), dp(70f).toInt(), dp(20f).toInt(), dp(20f).toInt())
        }
        // connectivity card
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = glass(this@LockActivity, dp(28f), 50)
            setPadding(dp(6f).toInt(), dp(6f).toInt(), dp(6f).toInt(), dp(6f).toInt())
        }
        fun row(a: View, b: View) = LinearLayout(this).apply { addView(a); addView(b) }
        grid.addView(row(circle("✈️", 56f) { go(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }, circle("📱", 56f) { go(Settings.ACTION_WIRELESS_SETTINGS) }))
        grid.addView(row(circle("📶", 56f) { go(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) }, circle("🔷", 56f) { go(Settings.ACTION_BLUETOOTH_SETTINGS) }))

        val br = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) * 100 / 255
        val maxV = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val vol = audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / maxV
        val top = LinearLayout(this).apply { gravity = Gravity.TOP }
        top.addView(grid, LinearLayout.LayoutParams(0, -2, 2.2f))
        top.addView(sliderCol("☀", br) {
            val lp = window.attributes; lp.screenBrightness = (it / 100f).coerceAtLeast(0.02f); window.attributes = lp
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(sliderCol("🔊", vol) { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it * maxV / 100, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(top)

        // media card
        val media = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = glass(this@LockActivity, dp(28f), 50); setPadding(0, dp(14f).toInt(), 0, dp(14f).toInt())
        }
        media.addView(tv("Now Playing", 14f))
        val ctl = LinearLayout(this).apply { gravity = Gravity.CENTER }
        for ((sym, code) in listOf("⏮" to KeyEvent.KEYCODE_MEDIA_PREVIOUS, "⏯" to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "⏭" to KeyEvent.KEYCODE_MEDIA_NEXT)) {
            ctl.addView(tv(sym, 30f).apply {
                setPadding(dp(22f).toInt(), dp(6f).toInt(), dp(22f).toInt(), dp(6f).toInt())
                setOnClickListener { bounce(this); key(code) }
            })
        }
        media.addView(ctl)
        col.addView(media, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16f).toInt() })

        // round tiles
        val tiles = LinearLayout(this).apply { gravity = Gravity.CENTER }
        tiles.addView(circle("🔦", 64f) { setTorch(it) })
        tiles.addView(circle("🔒", 64f) {})
        tiles.addView(circle("🌙", 64f) {})
        tiles.addView(circle("🔔", 64f) {})
        col.addView(tiles, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16f).toInt() })
        panel.addView(col)
    }

    private fun sliderCol(icon: String, init: Int, onChange: (Int) -> Unit): LinearLayout {
        val label = tv("$icon $init%", 12f)
        val s = GlassSlider(this, true).apply { value = init }
        val set = { v: Int -> s.value = v; label.text = "$icon ${s.value}%"; onChange(s.value) }
        s.onChange = { v -> label.text = "$icon $v%"; onChange(v) }
        val plus = tv("＋", 22f).apply { setOnClickListener { bounce(this); set(s.value + 5) } }
        val minus = tv("－", 22f).apply { setOnClickListener { bounce(this); set(s.value - 5) } }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            addView(plus); addView(s, LinearLayout.LayoutParams(dp(64f).toInt(), dp(170f).toInt())); addView(minus); addView(label)
        }
    }

    private fun tv(t: String, sp: Float) = TextView(this).apply {
        text = t; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(Color.WHITE); gravity = Gravity.CENTER
    }

    private fun circle(label: String, sizeDp: Float, action: (Boolean) -> Unit): TextView {
        val s = dp(sizeDp).toInt(); val m = dp(6f).toInt()
        return tv(label, 22f).apply {
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(m, m, m, m) }
            background = glass(this@LockActivity, s / 2f, 60)
            var on = false
            setOnClickListener {
                on = !on; bounce(this)
                background = if (on) GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) } else glass(this@LockActivity, s / 2f, 60)
                setTextColor(if (on) Color.BLACK else Color.WHITE)
                action(on)
            }
        }
    }

    private fun go(action: String) {
        try { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
    }

    private fun key(code: Int) {
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun setTorch(on: Boolean) {
        try { cam.setTorchMode(cam.cameraIdList[0], on); torch = on } catch (_: Exception) {}
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (open) toggle(false) }
}
