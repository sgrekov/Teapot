package com.factorymarket.rxelm.sample.login.model

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.ViewCmd

object GetSavedUserCmd : Cmd()
data class SaveUserCredentialsCmd(val login: String, val pass: String) : Cmd()
data class LoginCmd(val login: String, val pass: String) : Cmd()
object GoToMainCmd : ViewCmd()