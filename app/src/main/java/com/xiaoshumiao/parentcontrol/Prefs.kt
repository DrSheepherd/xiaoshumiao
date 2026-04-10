package com.xiaoshumiao.parentcontrol

import android.content.Context

object Prefs {
    private const val NAME = "xiaoshumiao_parent"
    private const val KEY_LOCKED = "locked"
    private const val KEY_MAX_VOL_PCT = "max_vol_pct"
    private const val KEY_MAX_BRI_PCT = "max_bri_pct"

    fun isLocked(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_LOCKED, false)

    fun setLocked(context: Context, locked: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOCKED, locked).apply()
    }

    fun getMaxVolumePct(context: Context): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(KEY_MAX_VOL_PCT, 60)
            .coerceIn(1, 100)

    fun getMaxBrightnessPct(context: Context): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(KEY_MAX_BRI_PCT, 60)
            .coerceIn(1, 100)

    fun setLimits(context: Context, volumePct: Int, brightnessPct: Int) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_MAX_VOL_PCT, volumePct.coerceIn(1, 100))
            .putInt(KEY_MAX_BRI_PCT, brightnessPct.coerceIn(1, 100))
            .apply()
    }
}
