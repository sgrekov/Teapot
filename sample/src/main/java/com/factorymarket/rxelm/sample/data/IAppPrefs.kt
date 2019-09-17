package com.factorymarket.rxelm.sample.data

import io.reactivex.Single

interface IAppPrefs {

    fun getUserSavedCredentials() : Single<Pair<String, String>>
    suspend fun getUserSavedCredentials2() : Pair<String, String>
    fun saveUserSavedCredentials(login : String, pass : String) : Single<Boolean>
    suspend fun saveUserSavedCredentials2(login : String, pass : String) : Boolean
}