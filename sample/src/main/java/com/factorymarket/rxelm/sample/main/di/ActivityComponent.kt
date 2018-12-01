package com.factorymarket.rxelm.sample.main.di

import androidx.fragment.app.FragmentActivity
import com.factorymarket.rxelm.sample.login.di.LoginComponent
import com.factorymarket.rxelm.sample.login.di.LoginModule
import com.factorymarket.rxelm.sample.navigation.AndroidNavigator
import com.factorymarket.rxelm.sample.navigation.Navigator
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {

    fun plusLoginComponent(module: LoginModule): LoginComponent
    fun plusMainComponent(module: MainModule): MainComponent

}

@Module
class ActivityModule(private val activity: FragmentActivity) {

    @Provides
    fun navigator(): Navigator = AndroidNavigator(activity)

}