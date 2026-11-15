package com.glass.launcher

import android.content.Context

class Theme(val name: String, val base: Int, val blobs: List<Int>)

object Prefs {
    val THEMES = listOf(
        Theme("Aurora", 0xFF0A0E22.toInt(), listOf(0xCC7B5CFF.toInt(), 0xCC00B7FF.toInt(), 0xCCFF4F9A.toInt(), 0xAA25D0A0.toInt())),
        Theme("Sunset", 0xFF1A0B1E.toInt(), listOf(0xCCFF7A45.toInt(), 0xCCFF3D6E.toInt(), 0xCC8A4DFF.toInt(), 0xAAFFC145.toInt())),
        Theme("Ocean", 0xFF04112A.toInt(), listOf(0xCC0091FF.toInt(), 0xCC00D4D4.toInt(), 0xCC3D5AFE.toInt(), 0xAA00E5A0.toInt())),
        Theme("Forest", 0xFF06150F.toInt(), listOf(0xCC2ECC71.toInt(), 0xCC00A896.toInt(), 0xCC9BE15D.toInt(), 0xAA1B8A5A.toInt())),
        Theme("Midnight", 0xFF05060F.toInt(), listOf(0xCC5B6CFF.toInt(), 0xCC8E8EFF.toInt(), 0xCC3A3A8C.toInt(), 0xAA6A5ACD.toInt()))
    )
    val GLASS = listOf("Low", "Medium", "High")
    val GLASS_K = listOf(0.7f, 1f, 1.4f)
    val SIZES = listOf("Small", "Medium", "Large")
    val CARD_W = listOf(124f, 150f, 184f)
    val ICON = listOf(72f, 88f, 112f)

    private fun sp(c: Context) = c.getSharedPreferences("glass", Context.MODE_PRIVATE)
    fun int(c: Context, k: String, d: Int) = sp(c).getInt(k, d)
    fun putInt(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()
    fun bool(c: Context, k: String, d: Boolean) = sp(c).getBoolean(k, d)
    fun putBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
    fun hidden(c: Context): Set<String> = sp(c).getStringSet("hidden", emptySet())!!.toSet()
    fun setHidden(c: Context, s: Set<String>) = sp(c).edit().putStringSet("hidden", s).apply()
    fun reset(c: Context) = sp(c).edit().clear().apply()
    fun theme(c: Context) = THEMES[int(c, "theme", 0) % THEMES.size]
    fun glassK(c: Context) = GLASS_K[int(c, "glass", 1)]
    val CUSTOM get() = THEMES.size
    fun str(c: Context, k: String): String? = sp(c).getString(k, null)
    fun putStr(c: Context, k: String, v: String?) = sp(c).edit().putString(k, v).apply()
    fun favs(c: Context): Set<String> = sp(c).getStringSet("favs", emptySet())!!.toSet()
    fun setFavs(c: Context, s: Set<String>) = sp(c).edit().putStringSet("favs", s).apply()
}
