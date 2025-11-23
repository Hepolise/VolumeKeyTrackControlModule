package ru.hepolise.volumekeytrackcontrol.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.RewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.REWIND_ACTION_TYPE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getRewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSettingsSharedPreferences
import ru.hepolise.volumekeytrackcontrolmodule.R

class RewindActionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        toggleActionType()
    }

    private fun toggleActionType() {
        val prefs = getSettingsSharedPreferences()
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
        val prefs = getSettingsSharedPreferences()
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