package com.glass.launcher

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Entry(val label: String, val icon: Drawable, val onClick: () -> Unit, val onLong: () -> Unit = {})

class MainActivity : Activity() {
    private lateinit var rv: RecyclerView
    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.main)
        rv = findViewById(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(r: RecyclerView, dx: Int, dy: Int) {
                for (i in 0 until r.childCount) r.getChildAt(i).invalidate()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun refreshAll() {
        val k = Prefs.glassK(this)
        findViewById<LiquidBackground>(R.id.bg).refresh()
        findViewById<View>(R.id.dock).background = GlassDrawable(dp(40f), dp(2.5f), k)
        buildBar(k)
        findViewById<View>(R.id.clockbox).visibility =
            if (Prefs.bool(this, "clock", true)) View.VISIBLE else View.INVISIBLE
        buildInputs(k)
        rv.adapter = Adapter(entries())
        rv.requestFocus()
    }

    @Deprecated("") override fun onBackPressed() {}

    override fun onKeyDown(code: Int, e: KeyEvent): Boolean {
        if (code == KeyEvent.KEYCODE_MENU) { openSettings(); return true }
        return super.onKeyDown(code, e)
    }

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    private fun entries(): List<Entry> {
        val pm = packageManager
        val hidden = Prefs.hidden(this)
        val favs = Prefs.favs(this)
        val list = loadApps(this).filter { it.activityInfo.packageName !in hidden }
            .sortedWith(compareBy<ResolveInfo>({ it.activityInfo.packageName !in favs }, { it.loadLabel(pm).toString().lowercase() }))
            .map { a ->
                val pkg = a.activityInfo.packageName
                val name = a.loadLabel(pm).toString()
                val fav = pkg in favs
                Entry((if (fav) "\u2605 " else "") + name, a.loadIcon(pm),
                    onClick = { pm.getLaunchIntentForPackage(pkg)?.let { startActivity(it) } },
                    onLong = { appMenu(pkg, name, fav) })
            }.toMutableList()
        val gear = resources.getDrawable(android.R.drawable.ic_menu_preferences).mutate()
        gear.setTint(Color.WHITE)
        list.add(Entry("Customize", gear, { openSettings() }))
        return list
    }

    private fun appMenu(pkg: String, name: String, fav: Boolean) {
        val items = arrayOf(if (fav) "Remove from favorites" else "Add to favorites", "App info", "Uninstall")
        AlertDialog.Builder(this).setTitle(name).setItems(items) { _, i ->
            when (i) {
                0 -> {
                    val s = Prefs.favs(this).toMutableSet()
                    if (fav) s.remove(pkg) else s.add(pkg)
                    Prefs.setFavs(this, s)
                    refreshAll()
                }
                1 -> try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
                } catch (e: Exception) { }
                2 -> try {
                    startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
                } catch (e: Exception) { }
            }
        }.show()
    }

    private fun wifiOn() = try {
        (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    } catch (e: Exception) { true }

    private fun btOn() = try { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true } catch (e: Exception) { false }

    private fun open(action: String) {
        try { startActivity(Intent(action)) }
        catch (e: Exception) {
            try { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            catch (e2: Exception) { Toast.makeText(this, "Can't open settings on this TV", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun buildBar(k: Float) {
        val bar = findViewById<LinearLayout>(R.id.bar)
        bar.removeAllViews()
        bar.visibility = if (Prefs.bool(this, "topbar", true)) View.VISIBLE else View.GONE
        val items: List<Triple<Int, Boolean, () -> Unit>> = listOf(
            Triple(R.drawable.ic_wifi, wifiOn()) { open(Settings.ACTION_WIFI_SETTINGS) },
            Triple(R.drawable.ic_bluetooth, btOn()) { open(Settings.ACTION_BLUETOOTH_SETTINGS) },
            Triple(R.drawable.ic_settings, true) { open(Settings.ACTION_SETTINGS) },
            Triple(R.drawable.ic_tune, true) { openSettings() }
        )
        for ((icon, on, act) in items) {
            val b = ImageView(this).apply {
                setImageResource(icon)
                if (!on) setColorFilter(Color.argb(115, 255, 255, 255), PorterDuff.Mode.SRC_IN)
                val pad = dp(15f).toInt(); setPadding(pad, pad, pad, pad)
                isFocusable = true; isClickable = true
                background = GlassDrawable(dp(28f), dp(2f), k)
                layoutParams = LinearLayout.LayoutParams(dp(56f).toInt(), dp(56f).toInt()).apply { marginStart = dp(14f).toInt() }
                setOnFocusChangeListener { v, f ->
                    val s = if (f) 1.12f else 1f
                    v.animate().scaleX(s).scaleY(s).setDuration(140).start()
                }
                setOnClickListener { act() }
            }
            bar.addView(b)
        }
    }

    private fun buildInputs(k: Float) {
        val box = findViewById<LinearLayout>(R.id.inputs)
        box.removeAllViews()
        if (!Prefs.bool(this, "inputs", true)) return
        for ((label, intent) in loadInputs(this)) {
            val chip = TextView(this).apply {
                text = label; setTextColor(Color.WHITE); textSize = 16f
                setPadding(dp(26f).toInt(), dp(10f).toInt(), dp(26f).toInt(), dp(10f).toInt())
                isFocusable = true; isClickable = true
                background = GlassDrawable(dp(24f), dp(2f), k)
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(14f).toInt() }
                setOnFocusChangeListener { v, f ->
                    val s = if (f) 1.1f else 1f
                    v.animate().scaleX(s).scaleY(s).setDuration(140).start()
                }
                setOnClickListener {
                    try { startActivity(intent) }
                    catch (e: Exception) { Toast.makeText(context, "Can't open $label on this TV", Toast.LENGTH_SHORT).show() }
                }
            }
            box.addView(chip)
        }
    }

    inner class Adapter(private val items: List<Entry>) : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
            val label: TextView = v.findViewById(R.id.label)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item, p, false)
            val s = Prefs.int(this@MainActivity, "size", 1)
            v.background = GlassDrawable(dp(28f), dp(2.5f), Prefs.glassK(this@MainActivity))
            v.layoutParams.width = dp(Prefs.CARD_W[s]).toInt()
            val ic = v.findViewById<ImageView>(R.id.icon)
            ic.layoutParams.width = dp(Prefs.ICON[s]).toInt()
            ic.layoutParams.height = dp(Prefs.ICON[s]).toInt()
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, i: Int) {
            val e = items[i]
            h.icon.setImageDrawable(e.icon)
            h.label.text = e.label
            h.itemView.setOnFocusChangeListener { v, f ->
                val s = if (f) 1.12f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(140).start()
            }
            h.itemView.setOnClickListener { e.onClick() }
            h.itemView.setOnLongClickListener { e.onLong(); true }
        }
    }
}
