package com.lovable.extradimness

import android.content.Context

object DimPrefs {
    private const val FILE = "extra_dim_prefs"
    private const val KEY_LEVEL = "dim_level"   // 0..90 (percentage of black overlay alpha)
    private const val KEY_ON = "dim_on"

    fun setLevel(ctx: Context, level: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_LEVEL, level.coerceIn(0, 90)).apply()
    }

    fun getLevel(ctx: Context): Int =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_LEVEL, 40)

    fun setOn(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ON, on).apply()
    }

    fun isOn(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ON, false)
}
