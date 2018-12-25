package com.factorymarket.rxelm.sample.main.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.factorymarket.rxelm.sample.LOGIN_TAG
import com.factorymarket.rxelm.sample.R
import com.factorymarket.rxelm.sample.SampleApp
import com.factorymarket.rxelm.sample.login.view.LoginFragment
import com.factorymarket.rxelm.sample.main.di.ActivityComponent
import com.factorymarket.rxelm.sample.main.di.ActivityModule

class MainActivity : AppCompatActivity() {

    lateinit var activityComponent: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        activityComponent = (application as SampleApp)
            .appComponent
            .plusActivityComponent(ActivityModule(this))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_main)

        if (supportFragmentManager.findFragmentByTag(LOGIN_TAG) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, LoginFragment(), LOGIN_TAG)
                .commit()
        }
    }
}
