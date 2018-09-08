package com.factorymarket.rxelm.sample.navigation

import androidx.fragment.app.FragmentActivity
import com.factorymarket.rxelm.sample.MAIN_TAG
import com.factorymarket.rxelm.sample.R
import com.factorymarket.rxelm.sample.main.view.MainFragment
import com.factorymarket.rxelm.sample.navigation.Navigator


class AndroidNavigator(private val activity: FragmentActivity) : Navigator {

    override fun goToMainScreen() {
        activity
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment, MainFragment(), MAIN_TAG)
            .commitNow()
    }

}