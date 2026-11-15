package com.glass.launcher

import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View

class GlassDrawable(private val radius: Float, private val stroke: Float, private val k: Float = 1f, private val onlyFocused: Boolean = false) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val gloss = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wash = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
    private val back = Paint(Paint.FILTER_BITMAP_FLAG)
    private val r = RectF()
    private val clip = Path()
    private val dst = Rect()
    private val loc = IntArray(2)
    private var focused = false

    init {
        val cm = ColorMatrix()
        cm.setSaturation(1.6f)
        cm.postConcat(ColorMatrix(floatArrayOf(1.08f, 0f, 0f, 0f, 14f, 0f, 1.08f, 0f, 0f, 14f, 0f, 0f, 1.08f, 0f, 14f, 0f, 0f, 0f, 1f, 0f)))
        back.colorFilter = ColorMatrixColorFilter(cm)
    }

    private fun w(a: Int) = Color.argb(minOf(255, (a * k).toInt()), 255, 255, 255)

    override fun onBoundsChange(b: Rect) {
        r.set(b); r.inset(3f, 3f)
        clip.reset(); clip.addRoundRect(r, radius, radius, Path.Direction.CW)
        fill.shader = LinearGradient(0f, r.top, 0f, r.bottom,
            intArrayOf(w(60), w(14), w(36)), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        rim.shader = LinearGradient(r.left, r.top, r.right, r.bottom,
            intArrayOf(w(240), w(24), w(24), w(160)), floatArrayOf(0f, .35f, .65f, 1f), Shader.TileMode.CLAMP)
        gloss.shader = LinearGradient(0f, r.top, 0f, r.top + r.height() * .5f, w(80), w(0), Shader.TileMode.CLAMP)
    }

    override fun draw(c: Canvas) {
        if (onlyFocused && !focused) return
        val bl = Backdrop.blur
        val v = callback as? View
        if (bl != null && v != null && Backdrop.w > 0) {
            v.getLocationInWindow(loc)
            c.save()
            c.clipPath(clip)
            c.translate(-loc[0].toFloat(), -loc[1].toFloat())
            c.scale(1.07f, 1.07f, loc[0] + r.centerX(), loc[1] + r.centerY())
            dst.set(0, 0, Backdrop.w, Backdrop.h)
            c.drawBitmap(bl, null, dst, back)
            c.restore()
        }
        c.drawRoundRect(r, radius, radius, fill)
        if (focused) c.drawRoundRect(r, radius, radius, wash)
        c.save(); c.clipPath(clip)
        c.drawRect(r.left, r.top, r.right, r.top + r.height() * .5f, gloss)
        c.restore()
        rim.strokeWidth = if (focused) stroke else stroke / 2.5f
        c.drawRoundRect(r, radius, radius, rim)
    }

    override fun isStateful() = true
    override fun onStateChange(s: IntArray): Boolean {
        val f = s.contains(android.R.attr.state_focused)
        if (f == focused) return false
        focused = f; invalidateSelf(); return true
    }

    override fun setAlpha(a: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
