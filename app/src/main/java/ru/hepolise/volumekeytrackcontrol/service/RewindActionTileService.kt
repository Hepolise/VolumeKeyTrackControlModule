package ru.hepolise.volumekeytrackcontrol.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import io.github.libxposed.service.XposedService
import ru.hepolise.volumekeytrackcontrol.App
import ru.hepolise.volumekeytrackcontrol.R
import ru.hepolise.volumekeytrackcontrol.util.RewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.REWIND_ACTION_TYPE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getRewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSettingsSharedPreferences

class RewindActionTileService : TileService(), App.ServiceStateListener {

    private var xposedService: XposedService? = null

    override fun onStartListening() {
        super.onStartListening()
        App.addServiceStateListener(this, true)
        updateTile()
    }

    override fun onStopListening() {
        App.removeServiceStateListener(this)
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        toggleActionType()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        xposedService = service
    }

    private fun toggleActionType() {
        val prefs = xposedService?.getSettingsSharedPreferences()
        val currentType = prefs.getRewindActionType()

        val newType = when (currentType) {
            RewindActionType.TRACK_CHANGE -> RewindActionType.REWIND
            RewindActionType.REWIND -> RewindActionType.TRACK_CHANGE
        }

        prefs?.edit {
            putString(REWIND_ACTION_TYPE, newType.name)
        }

        updateTile()
    }

    private fun updateTile() {
        val prefs = xposedService?.getSettingsSharedPreferences()
        val currentType = prefs.getRewindActionType()

        val tile = qsTile ?: return

        when (currentType) {
            RewindActionType.TRACK_CHANGE -> {
                tile.label = getString(R.string.track_change)
                tile.contentDescription = getString(R.string.track_change)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_skip_next_48dp)
                tile.state = Tile.STATE_ACTIVE
            }

            RewindActionType.REWIND -> {
                tile.label = getString(R.string.rewind)
                tile.contentDescription = getString(R.string.rewind)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_fast_forward_48dp)
                tile.state = Tile.STATE_ACTIVE
            }
        }

        tile.updateTile()
    }
}