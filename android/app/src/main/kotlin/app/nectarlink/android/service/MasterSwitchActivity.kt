// SPDX-License-Identifier: GPL-3.0-or-later
package app.nectarlink.android.service

import android.app.Activity
import android.os.Bundle

/**
 * Turns NectarLink on or off from outside, with no screen of its own: for
 * routines (Samsung's Modes and Routines, Tasker, MacroDroid), launcher
 * shortcuts and `adb shell am start`. What it does is in the intent's action:
 *
 *  - [ACTION_ON]: [MasterSwitch] on;
 *  - [ACTION_OFF]: [MasterSwitch] off;
 *  - [ACTION_TOGGLE] (or no action, as a plain "open app" does): the other one.
 *
 * Any app can start it, and all it can do is flip this one switch, the same one
 * as the Quick Settings tile.
 */
class MasterSwitchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            ACTION_ON -> MasterSwitch.set(this, true)
            ACTION_OFF -> MasterSwitch.set(this, false)
            else -> MasterSwitch.set(this, !MasterSwitch.isOn(this))
        }
        finish()
    }

    companion object {
        const val ACTION_ON = "app.nectarlink.android.action.MASTER_ON"
        const val ACTION_OFF = "app.nectarlink.android.action.MASTER_OFF"
        const val ACTION_TOGGLE = "app.nectarlink.android.action.MASTER_TOGGLE"
    }
}
