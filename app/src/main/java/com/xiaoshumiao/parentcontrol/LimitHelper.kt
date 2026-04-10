package com.xiaoshumiao.parentcontrol

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.util.Log

object LimitHelper {
    private const val TAG = "LimitHelper"

    private val STREAMS = intArrayOf(
        AudioManager.STREAM_VOICE_CALL,
        AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_NOTIFICATION,
    )

    fun clampAllVolumes(context: Context, maxVolumePct: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val pct = maxVolumePct.coerceIn(1, 100)
        for (stream in STREAMS) {
            try {
                val max = am.getStreamMaxVolume(stream)
                val cap = (max * (pct / 100f)).toInt().coerceIn(0, max)
                val cur = am.getStreamVolume(stream)
                if (cur > cap) {
                    am.setStreamVolume(stream, cap, 0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "clamp volume stream=$stream", e)
            }
        }
    }

    fun clampBrightness(context: Context, maxBrightnessPct: Int) {
        if (!Settings.System.canWrite(context)) return
        val pct = maxBrightnessPct.coerceIn(1, 100)
        val cap = (255 * (pct / 100f)).toInt().coerceIn(1, 255)
        try {
            val cur = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            if (cur > cap) {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    cap
                )
            }
        } catch (e: Settings.SettingNotFoundException) {
            Log.w(TAG, "brightness not found", e)
        } catch (e: Exception) {
            Log.w(TAG, "clamp brightness", e)
        }
    }
}
