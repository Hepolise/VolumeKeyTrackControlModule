package ru.hepolise.volumekeytrackcontrol.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.Constants
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.HOOK_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LAST_INIT_HOOK_TIME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LAUNCHED_COUNT
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLaunchedCount

class HookStatusProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        val prefs = context.getSharedPreferences(HOOK_PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            when (method) {
                Constants.SET_HOOKED -> {
                    putLong(LAST_INIT_HOOK_TIME, System.currentTimeMillis())
                }

                Constants.INCREMENT_LAUNCH_COUNT -> {
                    val current = prefs.getLaunchedCount()
                    putInt(LAUNCHED_COUNT, current + 1)
                }
            }
        }
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
