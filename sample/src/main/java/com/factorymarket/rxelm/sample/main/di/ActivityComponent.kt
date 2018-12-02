package com.factorymarket.rxelm.sample.main.di

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.factorymarket.rxelm.sample.di.SampleViewModelFactory
import com.factorymarket.rxelm.sample.di.ViewModelKey
import com.factorymarket.rxelm.sample.login.di.LoginComponent
import com.factorymarket.rxelm.sample.login.presenter.LoginViewModel
import com.factorymarket.rxelm.sample.main.presenter.MainViewModel
import com.factorymarket.rxelm.sample.navigation.AndroidNavigator
import com.factorymarket.rxelm.sample.navigation.Navigator
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap

@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {

    fun plusLoginComponent(): LoginComponent
    fun plusMainComponent(): MainComponent

}

@Module
class ActivityModule(private val activity: FragmentActivity) {

    @Provides
    fun navigator(): Navigator = AndroidNavigator(activity)

    @Provides
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    fun bindLoginViewModel(loginViewModel: LoginViewModel): ViewModel {
        return loginViewModel
    }

    @Provides
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    fun bindMainViewModel(mainViewModel: MainViewModel): ViewModel {
        return mainViewModel
    }

    @Provides
    fun bindViewModelFactory(factory: SampleViewModelFactory): ViewModelProvider.Factory {
        return factory
    }

}