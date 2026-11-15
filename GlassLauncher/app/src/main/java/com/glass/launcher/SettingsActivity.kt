package com.glass.launcher

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class SettingsActivity : Activity() {
    private lateinit var box: LinearLayout
    private lateinit var bg: LiquidBackground
    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = FrameLayout(this)
        bg = LiquidBackground(this, null)
        root.addView(bg, FrameLayout.LayoutParams(-1, -1))
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(120f), dp(48f), dp(120f), dp(48f))
        }
        val scroll = ScrollView(this).apply { addView(box) }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        build()
    }

    private fun cycle(key: String, def: Int, n: Int) = Prefs.putInt(this, key, (Prefs.int(this, key, def) + 1) % n)
    private fun toggle(key: String) = Prefs.putBool(this, key, !Prefs.bool(this, key, true))
    private fun onOff(key: String) = if (Prefs.bool(this, key, true)) "Show" else "Hide"

    private fun build() {
        box.removeAllViews()
        bg.refresh()
        box.addView(TextView(this).apply {
            text = "Customize"; setTextColor(Color.WHITE); textSize = 40f
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            setPadding(0, 0, 0, dp(24f))
        })
        row("Wallpaper", wpName()) { cycleWp() }
        row("Choose your own image", "") { pickImage() }
        row("Glass strength", Prefs.GLASS[Prefs.int(this, "glass", 1)]) { cycle("glass", 1, 3) }
        row("Card size", Prefs.SIZES[Prefs.int(this, "size", 1)]) { cycle("size", 1, 3) }
        row("HDMI inputs", onOff("inputs")) { toggle("inputs") }
        row("Clock and date", onOff("clock")) { toggle("clock") }
        row("Top bar", onOff("topbar")) { toggle("topbar") }
        row("Hidden apps", "${Prefs.hidden(this).size} hidden") { hideDialog() }
        row("Reset to default", "") { Prefs.reset(this) }
    }

    private fun row(title: String, value: String, action: () -> Unit) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            isFocusable = true; isClickable = true
            setPadding(dp(32f), dp(18f), dp(32f), dp(18f))
            background = GlassDrawable(dp(28f).toFloat(), dp(2.5f).toFloat(), Prefs.glassK(this@SettingsActivity))
            setOnFocusChangeListener { v, f ->
                val s = if (f) 1.03f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            }
        }
        r.addView(TextView(this).apply { text = title; setTextColor(Color.WHITE); textSize = 20f },
            LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(TextView(this).apply { text = value; setTextColor(0xCCFFFFFF.toInt()); textSize = 20f })
        r.setOnClickListener {
            val idx = box.indexOfChild(r)
            action(); build()
            box.getChildAt(idx)?.requestFocus()
        }
        box.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14f) })
    }


    private fun wpName(): String =
        if (Prefs.int(this, "theme", 0) == Prefs.CUSTOM) "Your image" else Prefs.theme(this).name

    private fun cycleWp() {
        val n = Prefs.THEMES.size + if (Prefs.str(this, "wpuri") != null) 1 else 0
        Prefs.putInt(this, "theme", (Prefs.int(this, "theme", 0) + 1) % n)
    }

    private fun pickImage() {
        try {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(i, 7)
        } catch (e: Exception) {
            Toast.makeText(this, "No file picker on this TV. Install a file manager app and try again.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("")
    override fun onActivityResult(req: Int, res: Int, d: Intent?) {
        super.onActivityResult(req, res, d)
        val u = d?.data
        if (req == 7 && res == RESULT_OK && u != null) {
            try { contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            Prefs.putStr(this, "wpuri", u.toString())
            Prefs.putInt(this, "theme", Prefs.CUSTOM)
            build()
        }
    }

    private fun hideDialog() {
        val apps = loadApps(this)
        val names = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        val hidden = Prefs.hidden(this).toMutableSet()
        val checked = BooleanArray(apps.size) { hidden.contains(apps[it].activityInfo.packageName) }
        AlertDialog.Builder(this).setTitle("Hide apps")
            .setMultiChoiceItems(names, checked) { _, i, c ->
                val p = apps[i].activityInfo.packageName
                if (c) hidden.add(p) else hidden.remove(p)
            }
            .setPositiveButton("Done") { _, _ -> Prefs.setHidden(this, hidden) }
            .setOnDismissListener { build() }
            .show()
    }
}
