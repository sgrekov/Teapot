package com.factorymarket.rxelm.sample.login.model

import com.factorymarket.rxelm.contract.State

data class LoginState(
    val login: String = "",
    val loginError: String? = null,
    val pass: String = "",
    val passError: String? = null,
    val saveUser: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val btnEnabled: Boolean = false
) : State()