package com.factorymarket.rxelm.sample.main.view

import org.eclipse.egit.github.core.Repository


interface IMainView {
    fun setTitle(title: String)

    fun showProgress(show : Boolean)

    fun setErrorText(errorText: String)

    fun showErrorText()

    fun setRepos(reposList: List<Repository>)
}
