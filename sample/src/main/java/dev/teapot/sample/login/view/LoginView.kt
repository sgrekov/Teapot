package dev.teapot.sample.login.view

interface LoginView {

    fun setProgress(show : Boolean)

    fun showPasswordError(errorText: String?)

    fun showLoginError(errorText: String?)

    fun setError(error: String?)

    fun hideKeyboard()

    fun setEnableLoginBtn(enabled : Boolean)

}
