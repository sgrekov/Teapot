package com.factorymarket.rxelm.sample

import android.app.Application
import com.factorymarket.rxelm.sample.data.GitHubService
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class SampleApp : Application() {

    lateinit var service: GitHubService

    override fun onCreate() {
        super.onCreate()
        service = GitHubService(Schedulers.io())
        Timber.plant(Timber.DebugTree())
    }
}
