package dev.teapot.sample

import android.app.Application
import dev.teapot.sample.di.AppComponent
import dev.teapot.sample.di.DaggerAppComponent
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber

class SampleApp : Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        RxJavaPlugins.setErrorHandler { throwable ->
            if (throwable is UndeliverableException) {
                Timber.e(throwable.cause)
            }
        }

        appComponent = DaggerAppComponent
            .builder()
            .application(this)
            .build()

        Timber.plant(Timber.DebugTree())
    }
}
