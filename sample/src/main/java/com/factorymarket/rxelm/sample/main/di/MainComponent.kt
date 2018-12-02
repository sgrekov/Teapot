package com.factorymarket.rxelm.sample.main.di

import com.factorymarket.rxelm.sample.main.view.MainFragment
import dagger.Subcomponent


@Subcomponent()
interface MainComponent {

    fun inject(mainFragment: MainFragment)

}