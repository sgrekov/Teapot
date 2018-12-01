package com.factorymarket.rxelm.sample

import android.app.Application
import com.factorymarket.rxelm.sample.data.GitHubService
import com.factorymarket.rxelm.sample.di.AppComponent
import com.factorymarket.rxelm.sample.di.DaggerAppComponent
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class SampleApp : Application() {

    lateinit var appComponent : AppComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent
            .builder()
            .application(this)
            .build()

        Timber.plant(Timber.DebugTree())
    }
}
