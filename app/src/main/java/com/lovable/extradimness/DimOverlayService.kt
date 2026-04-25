package com.lovable.extradimness

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.core.app.NotificationCompat


/**
 * Foreground service that owns:
 *  - a full-screen black overlay (the "extra dim" layer)
 *  - an optional small floating slider to control its alpha
 */
class DimOverlayService : Service() {

    private lateinit var wm: WindowManager
    private var dimView: View? = null


    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SET_LEVEL = "ACTION_SET_LEVEL"

        const val EXTRA_LEVEL = "level"
        private const val CHANNEL_ID = "extra_dim_channel"
        private const val NOTIF_ID = 42
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, createNotification(DimPrefs.getLevel(this)))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentLevel = DimPrefs.getLevel(this)
        when (intent?.action) {
            ACTION_START -> {
                addDimOverlay()
                DimPrefs.setOn(this, true)
                updateNotification(currentLevel)
                sendBroadcast(Intent("STATUS_CHANGED"))
            }
            ACTION_STOP -> {
                removeDimOverlay()

                DimPrefs.setOn(this, false)
                sendBroadcast(Intent("STATUS_CHANGED"))
                stopForeground(true)
                stopSelf()
            }
            ACTION_SET_LEVEL -> {
                val level = intent.getIntExtra(EXTRA_LEVEL, currentLevel)
                updateLevel(level)
            }
        }
        return START_STICKY
    }

    private fun updateLevel(level: Int) {
        DimPrefs.setLevel(this, level)
        applyLevel(level)
        updateNotification(level)
        
        // Broadcast change back to MainActivity if it's open
        sendBroadcast(Intent("STATUS_CHANGED"))
    }

    /* ---------- Dim overlay ---------- */

    private fun addDimOverlay() {
        if (dimView != null) return
        val v = View(this).apply { setBackgroundColor(Color.BLACK) }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setFitInsetsTypes(0)
                setFitInsetsSides(0)
            }
        }
        v.alpha = DimPrefs.getLevel(this) / 100f
        wm.addView(v, params)
        dimView = v
    }

    private fun applyLevel(level: Int) {
        dimView?.alpha = level / 100f
    }

    private fun removeDimOverlay() {
        dimView?.let { runCatching { wm.removeView(it) } }
        dimView = null
    }



    /* ---------- Foreground notification ---------- */

    private fun updateNotification(level: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, createNotification(level))
    }

    private fun createNotification(level: Int): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Extra Dimness",
                        NotificationManager.IMPORTANCE_LOW).apply {
                        setShowBadge(false)
                        enableVibration(false)
                        setSound(null, null)
                    }
                )
            }
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_dim)
            .setContentTitle("Extra Dimness is active")
            .setContentText("Tap to open app and adjust brightness ($level%)")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun getActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, DimOverlayService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onDestroy() {
        removeDimOverlay()

        super.onDestroy()
    }
}
