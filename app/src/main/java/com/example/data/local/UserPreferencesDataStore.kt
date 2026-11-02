package com.example.data.local

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.dataStore by preferencesDataStore(name = "jy_music_user_prefs")

class UserPreferencesDataStore(private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_IGNORE_SHORT = booleanPreferencesKey("ignore_short_tracks")
    private val KEY_PAUSE_DISCONNECT = booleanPreferencesKey("pause_on_disconnect")
    private val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")
    private val KEY_AVATAR_PATH = stringPreferencesKey("profile_avatar_path")

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[KEY_THEME] ?: "DARK",
            ignoreShortTracks = prefs[KEY_IGNORE_SHORT] ?: true,
            pauseOnDisconnect = prefs[KEY_PAUSE_DISCONNECT] ?: true,
            profileName = prefs[KEY_PROFILE_NAME] ?: "JY Listener",
            profileAvatarUri = prefs[KEY_AVATAR_PATH]
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun setIgnoreShortTracks(ignore: Boolean) {
        context.dataStore.edit { it[KEY_IGNORE_SHORT] = ignore }
    }

    suspend fun setPauseOnDisconnect(pause: Boolean) {
        context.dataStore.edit { it[KEY_PAUSE_DISCONNECT] = pause }
    }

    suspend fun setProfileName(name: String) {
        context.dataStore.edit { it[KEY_PROFILE_NAME] = name }
    }

    suspend fun saveProfileAvatar(uri: Uri?): String? = withContext(Dispatchers.IO) {
        if (uri == null) {
            context.dataStore.edit { it.remove(KEY_AVATAR_PATH) }
            return@withContext null
        }

        try {
            val destinationFile = File(context.filesDir, "jy_profile_avatar.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            val savedPath = destinationFile.absolutePath
            context.dataStore.edit { it[KEY_AVATAR_PATH] = savedPath }
            savedPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
