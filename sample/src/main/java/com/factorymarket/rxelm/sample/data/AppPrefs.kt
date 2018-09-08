package com.factorymarket.rxelm.sample.data

import android.content.SharedPreferences
import io.reactivex.Single

class AppPrefs(val preferences: SharedPreferences) : IAppPrefs {

    companion object {
        const val KEY_LOGIN = "key_login"
        const val KEY_PASS = "key_pass"
    }

    override fun getUserSavedCredentials(): Single<Pair<String, String>> {
        return Single.fromCallable {
            val login = preferences.getString(KEY_LOGIN, null)
            val pass = preferences.getString(KEY_PASS, null)
            if (login == null || pass == null) {
                throw NoSuchElementException()
            }
            return@fromCallable Pair(login, pass)
        }
    }

    override fun saveUserSavedCredentials(login: String, pass: String): Single<Boolean> {
        return Single.fromCallable {
            val editor = preferences.edit()
            editor.putString(KEY_LOGIN, login)
            editor.putString(KEY_PASS, pass)
            editor.apply()
            return@fromCallable true
        }
    }
}