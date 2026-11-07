// SPDX-License-Identifier: GPL-3.0-or-later
package app.nectarlink.android.service

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import app.nectarlink.android.NectarlinkApplication
import app.nectarlink.android.banking.BankingPause
import app.nectarlink.android.mic.MicService
import app.nectarlink.android.mirror.InputService
import app.nectarlink.android.mirror.MirrorService
import app.nectarlink.android.notifications.NotificationListener
import app.nectarlink.android.recorder.RecorderService
import app.nectarlink.android.webcam.WebcamService
import java.util.concurrent.Executors

/**
 * One switch for all of NectarLink: on, it works as always; off, it does
 * nothing at all until it's turned on again, so it costs no battery and
 * stays out of the way of apps that refuse to run beside it (banking and
 * payment apps such as InstaPay).
 *
 * Off means:
 *  - the connection to PCs is closed and the connection service is stopped;
 *  - the Elevated helper is stopped, and nothing starts it again;
 *  - notification mirroring is unbound;
 *  - Assist (the accessibility service) is turned off;
 *  - when NectarLink may change them (WRITE_SECURE_SETTINGS, which Elevated
 *    grants once), Developer options and USB / Wireless debugging are turned
 *    off as well, through [BankingPause], which remembers what was on and
 *    brings it back exactly when the switch is turned on.
 *
 * The state lives in the same preferences as the other app switches. Every
 * entry point that would start something in the background (the boot
 * receiver, the Companion Device service, Elevated, the connection service,
 * the core itself) asks [isOn] first.
 */
object MasterSwitch {
    private const val TAG = "MasterSwitch"
    private const val PREFS = "appearance"
    internal const val KEY = "master_enabled"

    /** Turning on and off run one after another, in the order they were asked. */
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "nectarlink-master-switch").apply { isDaemon = true }
    }

    /** Whether NectarLink is allowed to run. True until the user turns it off. */
    fun isOn(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    /**
     * Turns NectarLink on or off. The choice is saved at once, so everything
     * that starts in the meantime already sees it; the rest runs off the main thread.
     */
    fun set(context: Context, on: Boolean) {
        val app = context.applicationContext
        val saved = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (saved.getBoolean(KEY, true) == on) return
        saved.edit(commit = true) { putBoolean(KEY, on) }
        worker.execute {
            runCatching { if (on) turnOn(app) else turnOff(app) }
                .onFailure { Log.w(TAG, "couldn't turn ${if (on) "on" else "off"} completely", it) }
        }
    }

    private fun turnOff(context: Context) {
        val app = context.applicationContext as NectarlinkApplication
        Log.i(TAG, "turning NectarLink off")

        // Whatever a PC is using the phone for right now.
        runCatching { MirrorService.stopCurrent() }
        runCatching { if (RecorderService.session.value.active) RecorderService.stop(context) }
        runCatching { MicService.stopFromUser(context) }
        runCatching { WebcamService.stop(context) }

        // The connection, the Elevated helper (the core stops it) and the foreground service.
        ConnectionService.stop(context)
        app.core.stop()

        // Notification mirroring: Android keeps the listener bound until told otherwise.
        runCatching { NotificationListener.release() }

        // Developer options, debugging and Assist: the things banking apps refuse to run beside.
        // Needs WRITE_SECURE_SETTINGS; without it, Assist can still turn itself off.
        BankingPause.pause(context, notify = false)
        if (!BankingPause.isPaused(context)) InputService.turnOff()
    }

    private fun turnOn(context: Context) {
        val app = context.applicationContext as NectarlinkApplication
        Log.i(TAG, "turning NectarLink on")

        // Puts back exactly what turning off took away (nothing, if it took nothing).
        BankingPause.resume(context)
        runCatching { NotificationListener.reacquire(context) }

        // Starts the core, which starts Elevated and, when a PC is paired, the connection service.
        app.core.start()
        ConnectionService.start(context)
    }
}
