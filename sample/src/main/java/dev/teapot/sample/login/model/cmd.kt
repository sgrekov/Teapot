package dev.teapot.sample.login.model

import dev.teapot.cmd.Cmd
import dev.teapot.cmd.ViewCmd

object GetSavedUserCmd : Cmd()
data class SaveUserCredentialsCmd(val login: String, val pass: String) : Cmd()
data class LoginCmd(val login: String, val pass: String) : Cmd()
object GoToMainCmd : ViewCmd()