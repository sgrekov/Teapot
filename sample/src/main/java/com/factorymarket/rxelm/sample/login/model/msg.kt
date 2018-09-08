package com.factorymarket.rxelm.sample.login.model

import com.factorymarket.rxelm.msg.Msg

data class UserCredentialsLoadedMsg(val login: String, val pass: String) : Msg()
class UserCredentialsSavedMsg : Msg()
data class LoginInputMsg(val login: String) : Msg()
data class PassInputMsg(val pass: String) : Msg()
data class IsSaveCredentialsMsg(val checked: Boolean) : Msg()
data class LoginResponseMsg(val logged: Boolean) : Msg()
class LoginClickMsg : Msg()