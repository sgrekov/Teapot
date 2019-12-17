package dev.teapot.sample.main.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.teapot.sample.LOGIN_TAG
import dev.teapot.sample.MAIN_TAG
import dev.teapot.sample.R
import dev.teapot.sample.SampleApp
import dev.teapot.sample.login.view.LoginFragment
import dev.teapot.sample.main.di.ActivityComponent
import dev.teapot.sample.main.di.ActivityModule

class MainActivity : AppCompatActivity() {

    lateinit var activityComponent: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        activityComponent = (application as SampleApp)
            .appComponent
            .plusActivityComponent(ActivityModule(this))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_main)

        if (supportFragmentManager.findFragmentByTag(MAIN_TAG) != null) {
            return
        }

        if (supportFragmentManager.findFragmentByTag(LOGIN_TAG) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, LoginFragment(), LOGIN_TAG)
                .commit()
        }
    }
}
