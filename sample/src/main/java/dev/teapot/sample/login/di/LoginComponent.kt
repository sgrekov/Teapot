package dev.teapot.sample.login.di

import dev.teapot.sample.login.view.LoginView
import dev.teapot.sample.login.view.LoginFragment
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [LoginModule::class])
interface LoginComponent {

    fun inject(loginFragment: LoginFragment)

}

@Module
class LoginModule(private val loginFragment: LoginFragment) {

    @Provides
    fun loginView(): LoginView = loginFragment

}