package com.example.basicsplorer
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

object BookmarkManager {
    private const val PREFS_NAME = "prefs"
    private const val KEY_URI = "last_uri"

    fun saveFolderUri(context: Context, uri: Uri) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun loadFolderUri(context: Context): Uri? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_URI, null)
        return uriString?.let { Uri.parse(it) }
    }
}