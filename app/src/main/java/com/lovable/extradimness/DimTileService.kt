package com.lovable.extradimness

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * Quick Settings tile shown in the pull-down notification panel.
 * Tap to toggle the dim overlay.
 */
@RequiresApi(Build.VERSION_CODES.N)
class DimTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val on = DimPrefs.isOn(this)
        val intent = Intent(this, DimOverlayService::class.java).apply {
            action = if (on) DimOverlayService.ACTION_STOP else DimOverlayService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        DimPrefs.setOn(this, !on)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = DimPrefs.isOn(this)
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Extra Dim"
        tile.contentDescription = "Toggle extra screen dimness"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_dim)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (on) "${DimPrefs.getLevel(this)}%" else "Off"
        }
        tile.updateTile()
    }
}
