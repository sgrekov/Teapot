package com.factorymarket.rxelm.sample.login.di

import com.factorymarket.rxelm.sample.login.view.LoginFragment
import dagger.Subcomponent

@Subcomponent()
interface LoginComponent {

    fun inject(loginFragment: LoginFragment)

}
