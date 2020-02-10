package dev.teapot.sample.data

interface IAppPrefs {

    suspend fun getUserSavedCredentials2() : Pair<String, String>
    suspend fun saveUserSavedCredentials2(login : String, pass : String) : Boolean
}