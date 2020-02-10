package dev.teapot.sample.data

import android.content.SharedPreferences

class AppPrefs(val preferences: SharedPreferences) : IAppPrefs {

    companion object {
        const val KEY_LOGIN = "key_login"
        const val KEY_PASS = "key_pass"
    }

    override suspend fun getUserSavedCredentials2(): Pair<String, String> {
        val login = preferences.getString(KEY_LOGIN, null)
        val pass = preferences.getString(KEY_PASS, null)
        if (login == null || pass == null) {
            throw NoSuchElementException()
        }
        return Pair(login, pass)
    }

    override suspend fun saveUserSavedCredentials2(login: String, pass: String): Boolean {
        val editor = preferences.edit()
        editor.putString(KEY_LOGIN, login)
        editor.putString(KEY_PASS, pass)
        editor.apply()
        return true
    }
}