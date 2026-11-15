package com.glass.launcher

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LiquidBackground(ctx: Context, a: AttributeSet?) : View(ctx, a) {
    private val p = Paint(Paint.FILTER_BITMAP_FLAG)
    private val scrim = Paint().apply { color = Color.argb(50, 0, 0, 0) }
    private val dst = Rect()

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = refresh()

    fun refresh() {
        if (width == 0 || height == 0) return
        Backdrop.build(context, width, height)
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        dst.set(0, 0, width, height)
        val s = Backdrop.sharp
        if (s != null) c.drawBitmap(s, null, dst, p) else c.drawColor(0xFF0A0E22.toInt())
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrim)
    }
}
