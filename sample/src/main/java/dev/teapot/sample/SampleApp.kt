package dev.teapot.sample

import android.app.Application
import dev.teapot.sample.di.AppComponent
import dev.teapot.sample.di.DaggerAppComponent
import timber.log.Timber

class SampleApp : Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent
            .builder()
            .application(this)
            .build()

        Timber.plant(Timber.DebugTree())
    }
}
