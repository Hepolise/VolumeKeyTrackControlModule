package ru.hepolise.volumekeytrackcontrolmodule.extension

import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent

object AudioManagerExtension {

    fun AudioManager.sendMediaButtonEvent(code: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val keyIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null)
        var keyEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, code, 0)
        keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        dispatchMediaButtonEvent(keyEvent)
        keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP)
        keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        dispatchMediaButtonEvent(keyEvent)
    }

    private fun AudioManager.dispatchMediaButtonEvent(keyEvent: KeyEvent) {
        try {
            this.dispatchMediaKeyEvent(keyEvent)
        } catch (t: Throwable) {
            t.localizedMessage?.let { Log.e("xposed", it) }
            t.printStackTrace()
        }
    }
}