package com.factorymarket.rxelm.sample.login.view

interface ILoginView {

    fun setProgress(show : Boolean)

    fun showPasswordError(errorText: String?)

    fun showLoginError(errorText: String?)

    fun setError(error: String?)

    fun hideKeyboard()

    fun setEnableLoginBtn(enabled : Boolean)

}
