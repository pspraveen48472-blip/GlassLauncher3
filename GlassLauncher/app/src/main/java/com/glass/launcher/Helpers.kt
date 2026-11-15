package com.glass.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build

fun loadApps(ctx: Context): List<ResolveInfo> {
    val pm = ctx.packageManager
    fun q(cat: String) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
    return q(Intent.CATEGORY_LEANBACK_LAUNCHER).ifEmpty { q(Intent.CATEGORY_LAUNCHER) }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .sortedBy { it.loadLabel(pm).toString().lowercase() }
}

fun loadInputs(ctx: Context): List<Pair<String, Intent>> {
    val out = mutableListOf<Pair<String, Intent>>()
    try {
        val tm = ctx.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        for (i in tm.tvInputList) {
            if (!i.isPassthroughInput) continue
            val label = if (Build.VERSION.SDK_INT >= 24)
                (i.loadCustomLabel(ctx)?.toString() ?: i.loadLabel(ctx).toString())
            else "Input ${out.size + 1}"
            out.add(label to Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(i.id))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    } catch (e: Exception) { }
    return out
}
