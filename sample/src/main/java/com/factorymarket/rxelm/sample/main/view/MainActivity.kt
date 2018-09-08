package com.factorymarket.rxelm.sample.main.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.factorymarket.rxelm.sample.LOGIN_TAG
import com.factorymarket.rxelm.sample.R
import com.factorymarket.rxelm.sample.login.view.LoginFragment

class MainActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_main)

        if (supportFragmentManager.findFragmentByTag(LOGIN_TAG) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, LoginFragment(), LOGIN_TAG)
                .commit()
        }
    }
}
