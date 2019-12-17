package dev.teapot.sample.main.di

import dev.teapot.sample.main.view.MainView
import dev.teapot.sample.main.view.MainFragment
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
    fun mainView(): MainView = mainFragment

}