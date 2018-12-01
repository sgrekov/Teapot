package com.factorymarket.rxelm.sample.login.di

import com.factorymarket.rxelm.sample.login.view.ILoginView
import com.factorymarket.rxelm.sample.login.view.LoginFragment
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
    fun loginView(): ILoginView = loginFragment

}