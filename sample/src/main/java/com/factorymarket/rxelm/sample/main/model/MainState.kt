package com.factorymarket.rxelm.sample.main.model

import com.factorymarket.rxelm.contract.State
import org.eclipse.egit.github.core.Repository

data class MainState(
    val isLoading: Boolean = true,
    val userName: String,
    val reposList: List<Repository> = listOf()
) : State()