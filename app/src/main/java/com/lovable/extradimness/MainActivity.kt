package com.lovable.extradimness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.lovable.extradimness.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            syncUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupButtons()
        syncUI()
    }

    override fun onResume() {
        super.onResume()
        syncUI()
        registerReceiver(statusReceiver, IntentFilter("STATUS_CHANGED"))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
    }

    private fun setupButtons() {
        b.dimSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    b.levelLabel.text = "$p%"
                    sendAction(DimOverlayService.ACTION_SET_LEVEL, p)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        b.btnEnable.setOnClickListener {
            if (!hasOverlayPermission()) { requestOverlayPermission(); return@setOnClickListener }
            sendAction(DimOverlayService.ACTION_START)
            syncUI()
        }
        b.btnDisable.setOnClickListener { 
            sendAction(DimOverlayService.ACTION_STOP)
            syncUI()
        }
    }

    private fun syncUI() {
        val level = DimPrefs.getLevel(this)
        val isOn = DimPrefs.isOn(this)

        b.dimSeekBar.progress = level
        b.levelLabel.text = "$level%"
        
        if (isOn) {
            b.statusText.text = "Active"
            b.statusText.setTextColor(getColor(R.color.accent_purple))
            b.statusDot.setBackgroundResource(R.drawable.circle_active)
        } else {
            b.statusText.text = "Inactive"
            b.statusText.setTextColor(getColor(R.color.text_primary))
            b.statusDot.setBackgroundResource(R.drawable.circle_inactive)
        }
    }

    private fun hasOverlayPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
        )
    }

    private fun sendAction(action: String, level: Int? = null) {
        val intent = Intent(this, DimOverlayService::class.java).apply {
            this.action = action
            level?.let { putExtra(DimOverlayService.EXTRA_LEVEL, it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }
}
