package com.glassui.lock

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation

fun Context.dp(v: Float) = v * resources.displayMetrics.density

fun glass(ctx: Context, radius: Float, alpha: Int = 70) = GradientDrawable().apply {
    cornerRadius = radius
    setColor(Color.argb(alpha, 255, 255, 255))
    setStroke(ctx.dp(1.2f).toInt(), Color.argb(110, 255, 255, 255))
}

fun spring(v: View, prop: DynamicAnimation.ViewProperty, to: Float, damping: Float = 0.4f, stiff: Float = 350f) {
    SpringAnimation(v, prop, to).apply { spring.dampingRatio = damping; spring.stiffness = stiff; start() }
}

fun bounce(v: View) {
    v.scaleX = 0.8f; v.scaleY = 0.8f
    spring(v, DynamicAnimation.SCALE_X, 1f); spring(v, DynamicAnimation.SCALE_Y, 1f)
}

/** Silky flowing-curve wallpaper like the video. */
class WallpaperView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.shader = LinearGradient(0f, 0f, w, h,
            intArrayOf(0xFFE9C46A.toInt(), 0xFFB8B3AA.toInt(), 0xFF5B5A8F.toInt()), null, Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = null
        val path = Path()
        for (i in 0..3) {
            path.reset()
            path.moveTo(0f, h * (0.35f + 0.12f * i))
            path.cubicTo(w * 0.4f, h * (0.2f + 0.12f * i), w * 0.6f, h * (0.75f + 0.05f * i), w, h * (0.45f + 0.12f * i))
            path.lineTo(w, h); path.lineTo(0f, h); path.close()
            p.style = Paint.Style.FILL; p.color = Color.argb(60, 255, 255, 255); cv.drawPath(path, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = Color.argb(150, 255, 255, 255); cv.drawPath(path, p)
        }
    }
}

/** Liquid-glass slider: stretches while touched, springs back on release. */
class GlassSlider(c: Context, private val vertical: Boolean) : View(c) {
    var onChange: (Int) -> Unit = {}
    var value = 50
        set(v) { field = v.coerceIn(0, 100); invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = minOf(w, h) / 2f
        val rect = RectF(0f, 0f, w, h)
        val f = value / 100f
        paint.style = Paint.Style.FILL; paint.color = Color.argb(70, 255, 255, 255)
        cv.drawRoundRect(rect, r, r, paint)
        cv.save()
        clip.reset(); clip.addRoundRect(rect, r, r, Path.Direction.CW); cv.clipPath(clip)
        paint.color = Color.argb(235, 255, 255, 255)
        if (vertical) cv.drawRect(0f, h * (1 - f), w, h, paint) else cv.drawRect(0f, 0f, w * f, h, paint)
        cv.restore()
        paint.style = Paint.Style.STROKE; paint.strokeWidth = context.dp(1.2f); paint.color = Color.argb(130, 255, 255, 255)
        cv.drawRoundRect(rect, r, r, paint)
        if (!vertical) {
            val cx = (w * f).coerceIn(r, w - r)
            paint.style = Paint.Style.FILL; paint.color = Color.WHITE
            cv.drawCircle(cx, h / 2, r * 0.85f, paint)
            paint.style = Paint.Style.STROKE; paint.color = Color.argb(90, 80, 80, 100)
            cv.drawCircle(cx, h / 2, r * 0.85f, paint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { parent.requestDisallowInterceptTouchEvent(true); squish(true); update(e) }
            MotionEvent.ACTION_MOVE -> update(e)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> squish(false)
        }
        return true
    }

    private fun update(e: MotionEvent) {
        val f = if (vertical) 1f - e.y / height else e.x / width
        value = (f * 100).toInt(); onChange(value)
    }

    private fun squish(down: Boolean) {
        val sx = if (down) (if (vertical) 1.1f else 1.03f) else 1f
        val sy = if (down) (if (vertical) 1.03f else 1.25f) else 1f
        spring(this, DynamicAnimation.SCALE_X, sx, 0.45f, 300f)
        spring(this, DynamicAnimation.SCALE_Y, sy, 0.45f, 300f)
    }
}
