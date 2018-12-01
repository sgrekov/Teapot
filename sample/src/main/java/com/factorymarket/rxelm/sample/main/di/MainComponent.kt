package com.factorymarket.rxelm.sample.main.di

import com.factorymarket.rxelm.sample.login.view.ILoginView
import com.factorymarket.rxelm.sample.login.view.LoginFragment
import com.factorymarket.rxelm.sample.main.view.IMainView
import com.factorymarket.rxelm.sample.main.view.MainFragment
import dagger.Module
import dagger.Provides
import dagger.Subcomponent


@Subcomponent(modules = [MainModule::class])
interface MainComponent {

    fun inject(mainFragment: MainFragment)

}

@Module
class MainModule(private val mainFragment: MainFragment) {

    @Provides
    fun mainView(): IMainView = mainFragment

}