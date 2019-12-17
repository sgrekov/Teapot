package dev.teapot.sample.main.di

import androidx.fragment.app.FragmentActivity
import dev.teapot.sample.login.di.LoginComponent
import dev.teapot.sample.login.di.LoginModule
import dev.teapot.sample.navigation.AndroidNavigator
import dev.teapot.sample.navigation.Navigator
import dev.teapot.sample.repo.di.RepoComponent
import dev.teapot.sample.repo.di.RepoModule
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {

    fun plusLoginComponent(module: LoginModule): LoginComponent
    fun plusMainComponent(module: MainModule): MainComponent
    fun plusRepoComponent(module: RepoModule): RepoComponent

}

@Module
class ActivityModule(private val activity: FragmentActivity) {

    @Provides
    fun navigator(): Navigator = AndroidNavigator(activity)

}