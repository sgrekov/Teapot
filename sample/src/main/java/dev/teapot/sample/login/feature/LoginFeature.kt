package dev.teapot.sample.login.feature

import dev.teapot.msg.ErrorMsg
import dev.teapot.msg.Init
import dev.teapot.msg.Msg
import dev.teapot.program.Program
import dev.teapot.msg.Idle
import dev.teapot.program.ProgramBuilder
import dev.teapot.sample.navigation.Navigator
import dev.teapot.sample.data.RepoService
import dev.teapot.sample.data.IAppPrefs
import dev.teapot.sample.login.model.GetSavedUserCmd
import dev.teapot.sample.login.model.GoToMainCmd
import dev.teapot.sample.login.model.IsSaveCredentialsMsg
import dev.teapot.sample.login.model.LoginClickMsg
import dev.teapot.sample.login.model.LoginCmd
import dev.teapot.sample.login.model.LoginInputMsg
import dev.teapot.sample.login.model.LoginResponseMsg
import dev.teapot.sample.login.model.LoginState
import dev.teapot.sample.login.model.PassInputMsg
import dev.teapot.sample.login.model.SaveUserCredentialsCmd
import dev.teapot.sample.login.model.UserCredentialsLoadedMsg
import dev.teapot.sample.login.model.UserCredentialsSavedMsg
import dev.teapot.sample.login.view.LoginView
import dev.teapot.cmd.Cmd
import dev.teapot.contract.CoroutineFeature
import dev.teapot.contract.Renderable
import dev.teapot.contract.Update
import org.eclipse.egit.github.core.client.RequestException
import timber.log.Timber
import javax.inject.Inject

class LoginFeature @Inject constructor(
        private val loginView: LoginView,
        programBuilder: ProgramBuilder,
        private val appPrefs: IAppPrefs,
        private val apiService: RepoService,
        private val navigator: Navigator
) : CoroutineFeature<LoginState>, Renderable<LoginState> {

    private val program: Program<LoginState> = programBuilder.build(this)

    fun init() {
        program.run(initialState = LoginState())
    }

    override fun update(msg: Msg, state: LoginState): Update<LoginState> {
        return when (msg) {
            is Init -> Update.update(state.copy(isLoading = true), GetSavedUserCmd)
            is UserCredentialsLoadedMsg ->
                Update.update(state.copy(login = msg.login, pass = msg.pass), LoginCmd(msg.login, msg.pass))
            is LoginResponseMsg -> Update.effect(
                    if (state.saveUser) {
                        SaveUserCredentialsCmd(state.login, state.pass)
                    } else {
                        GoToMainCmd
                    }
            )
            is UserCredentialsSavedMsg -> Update.effect(GoToMainCmd)
            is IsSaveCredentialsMsg -> Update.state(state.copy(saveUser = msg.checked))
            is LoginInputMsg ->
                Update.state(
                        if (!validateLogin(msg.login)) {
                            state.copy(login = msg.login, btnEnabled = false)
                        } else state.copy(login = msg.login, loginError = null, btnEnabled = validatePass(state.pass))
                )
            is PassInputMsg ->
                Update.state(
                        if (!validatePass(msg.pass)) {
                            state.copy(pass = msg.pass, btnEnabled = false)
                        } else state.copy(pass = msg.pass, btnEnabled = validateLogin(state.login))
                )
            is LoginClickMsg -> {
                if (checkLogin(state.login)) {
                    Update.state(state.copy(loginError = "Login is not valid"))
                }
                if (checkPass(state.pass)) {
                    Update.state(state.copy(passError = "Password is not valid"))
                }
                Update.update(state.copy(isLoading = true, error = null), LoginCmd(state.login, state.pass))
            }
            is ErrorMsg -> {
                Timber.e(msg.err)
                val newState = when (msg.cmd) {
                    is GetSavedUserCmd -> state.copy(isLoading = false)
                    is LoginCmd -> {
                        if (msg.err is RequestException) {
                            state.copy(isLoading = false, error = (msg.err as RequestException).error.message)
                        }
                        state.copy(isLoading = false, error = "Error while login")
                    }
                    else -> state
                }
                return Update.state(newState)
            }
            else -> Update.idle()
        }
    }

    override fun render(state: LoginState) {
        state.apply {
            loginView.setProgress(isLoading)
            loginView.setEnableLoginBtn(btnEnabled)
            loginView.setError(error)
            loginView.showLoginError(loginError)
            loginView.showPasswordError(passError)
        }
    }

    fun render() {
        program.render()
    }

    override suspend fun call(cmd: Cmd): Msg {
        return when (cmd) {
            is GetSavedUserCmd -> {
                val (login, pass) = appPrefs.getUserSavedCredentials2()
                UserCredentialsLoadedMsg(login, pass)
            }
            is SaveUserCredentialsCmd -> {
                appPrefs.saveUserSavedCredentials2(cmd.login, cmd.pass)
                UserCredentialsSavedMsg()
            }
            is LoginCmd -> {
                val logged = apiService.login2(cmd.login, cmd.pass)
                LoginResponseMsg(logged)
            }
            is GoToMainCmd -> {
                navigator.goToMainScreen()
                Idle
            }
            else -> Idle
        }
    }

    fun loginBtnClick() {
        program.accept(LoginClickMsg())
    }

    fun onSaveCredentialsCheck(checked: Boolean) {
        program.accept(IsSaveCredentialsMsg(checked))
    }

    private fun validatePass(pass: CharSequence): Boolean {
        return pass.length > 4
    }

    private fun validateLogin(login: CharSequence): Boolean {
        return login.length > 3
    }

    private fun checkPass(pass: CharSequence): Boolean {
        return (pass.startsWith("42") || pass == "qwerty")
    }

    private fun checkLogin(login: CharSequence): Boolean {
        return (login.startsWith("42") || login == "admin")
    }

    fun loginInput(login : CharSequence) {
        program.accept(LoginInputMsg(login.toString()))
    }

    fun passInput(pass : CharSequence) {
        program.accept(PassInputMsg(pass.toString()))
    }

    fun destroy() {
        program.stop()
    }
}
