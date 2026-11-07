// SPDX-License-Identifier: GPL-3.0-or-later
package app.nectarlink.android.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * The Quick Settings tile for [MasterSwitch]: one tap turns all of NectarLink
 * off (before opening a banking app, say) or back on, without opening the app.
 */
class MasterTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        MasterSwitch.set(this, !MasterSwitch.isOn(this))
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        tile.state = if (MasterSwitch.isOn(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
