package com.xiaoshumiao.parentcontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo

class LockEnforcementService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var volumeReceiver: BroadcastReceiver? = null
    private var brightnessObserver: ContentObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.isLocked(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
        registerListeners()
        applyLimitsNow()
        return START_STICKY
    }

    private fun applyLimitsNow() {
        val volPct = Prefs.getMaxVolumePct(this)
        val briPct = Prefs.getMaxBrightnessPct(this)
        LimitHelper.clampAllVolumes(this, volPct)
        LimitHelper.clampBrightness(this, briPct)
    }

    private fun registerListeners() {
        if (volumeReceiver == null) {
            volumeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    applyLimitsNow()
                }
            }
            val filter = IntentFilter(AudioManager.VOLUME_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(volumeReceiver, filter)
            }
        }
        if (brightnessObserver == null && Settings.System.canWrite(this)) {
            val uri: Uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)
            brightnessObserver = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    applyLimitsNow()
                }
            }
            contentResolver.registerContentObserver(uri, false, brightnessObserver!!)
        }
    }

    private fun unregisterListeners() {
        volumeReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
            volumeReceiver = null
        }
        brightnessObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {
            }
            brightnessObserver = null
        }
    }

    override fun onDestroy() {
        unregisterListeners()
        super.onDestroy()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_lock),
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notif_lock_title))
            .setContentText(getString(R.string.notif_lock_text))
            .setOngoing(true)
            .setContentIntent(launch)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "lock_enforcement"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val i = Intent(context, LockEnforcementService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockEnforcementService::class.java))
        }
    }
}
