package com.factorymarket.rxelm.sample.main.view

import org.eclipse.egit.github.core.Repository


interface IMainView {
    fun setTitle(title: String)

    fun showProgress()

    fun hideProgress()

    fun setErrorText(errorText: String)

    fun showErrorText(show : Boolean)

    fun setRepos(reposList: List<Repository>)
}
