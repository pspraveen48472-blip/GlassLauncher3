package com.glass.launcher

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.util.Random

object Backdrop {
    var sharp: Bitmap? = null
    var blur: Bitmap? = null
    var w = 0
    var h = 0

    fun build(ctx: Context, w: Int, h: Int) {
        this.w = w; this.h = h
        val bw = minOf(w, 1280)
        val bh = maxOf(1, bw * h / w)
        val bmp = custom(ctx, bw, bh) ?: procedural(ctx, bw, bh)
        sharp = bmp
        var b = bmp
        while (b.width / 2 >= 48) b = Bitmap.createScaledBitmap(b, b.width / 2, maxOf(1, b.height / 2), true)
        blur = b
    }

    private fun procedural(ctx: Context, w: Int, h: Int): Bitmap {
        val t = Prefs.theme(ctx)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(t.base)
        val pos = listOf(floatArrayOf(.15f, .2f, .55f), floatArrayOf(.9f, .15f, .5f),
            floatArrayOf(.65f, 1f, .6f), floatArrayOf(.05f, 1f, .45f))
        pos.forEachIndexed { i, q ->
            p.shader = RadialGradient(q[0] * w, q[1] * h, q[2] * w, t.blobs[i], t.blobs[i] and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        }
        val rnd = Random(7)
        for (i in 0 until 9) {
            val col = t.blobs[i % 4]
            val r = w * (0.03f + rnd.nextFloat() * 0.07f)
            val x = w * rnd.nextFloat(); val y = h * rnd.nextFloat()
            p.shader = RadialGradient(x, y, r, intArrayOf(col or 0xFF000000.toInt(), col and 0x00FFFFFF),
                floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, p)
        }
        return bmp
    }

    private fun custom(ctx: Context, w: Int, h: Int): Bitmap? {
        if (Prefs.int(ctx, "theme", 0) != Prefs.CUSTOM) return null
        val u = Prefs.str(ctx, "wpuri") ?: return null
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(Uri.parse(u))?.use { BitmapFactory.decodeStream(it, null, o) }
            var s = 1
            while (o.outWidth / (s * 2) >= w) s *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = s }
            val src = ctx.contentResolver.openInputStream(Uri.parse(u))?.use { BitmapFactory.decodeStream(it, null, o2) }
                ?: return null
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val sc = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
            val m = Matrix()
            m.postScale(sc, sc)
            m.postTranslate((w - src.width * sc) / 2f, (h - src.height * sc) / 2f)
            Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
            out
        } catch (e: Exception) { null }
    }
}
