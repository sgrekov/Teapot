package com.factorymarket.rxelm.sample.login.presenter

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.contract.Renderable
import com.factorymarket.rxelm.contract.Update
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.navigation.Navigator
import com.factorymarket.rxelm.sample.data.IApiService
import com.factorymarket.rxelm.sample.data.IAppPrefs
import com.factorymarket.rxelm.sample.inView
import com.factorymarket.rxelm.sample.login.model.GetSavedUserCmd
import com.factorymarket.rxelm.sample.login.model.GoToMainCmd
import com.factorymarket.rxelm.sample.login.model.IsSaveCredentialsMsg
import com.factorymarket.rxelm.sample.login.model.LoginClickMsg
import com.factorymarket.rxelm.sample.login.model.LoginCmd
import com.factorymarket.rxelm.sample.login.model.LoginInputMsg
import com.factorymarket.rxelm.sample.login.model.LoginResponseMsg
import com.factorymarket.rxelm.sample.login.model.LoginState
import com.factorymarket.rxelm.sample.login.model.PassInputMsg
import com.factorymarket.rxelm.sample.login.model.SaveUserCredentialsCmd
import com.factorymarket.rxelm.sample.login.model.UserCredentialsLoadedMsg
import com.factorymarket.rxelm.sample.login.model.UserCredentialsSavedMsg
import com.factorymarket.rxelm.sample.login.view.ILoginView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import org.eclipse.egit.github.core.client.RequestException
import javax.inject.Inject

class LoginPresenter @Inject constructor(
    private val loginView: ILoginView,
    programBuilder: ProgramBuilder,
    private val appPrefs: IAppPrefs,
    private val apiService: IApiService,
    private val navigator: Navigator
) : Component<LoginState>, Renderable<LoginState> {

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

    override fun call(cmd: Cmd): Single<Msg> {
        return when (cmd) {
            is GetSavedUserCmd -> appPrefs.getUserSavedCredentials()
                .map { (login, pass) -> UserCredentialsLoadedMsg(login, pass) }
            is SaveUserCredentialsCmd -> appPrefs.saveUserSavedCredentials(cmd.login, cmd.pass)
                .map { _ -> UserCredentialsSavedMsg() }
            is LoginCmd -> apiService.login(cmd.login, cmd.pass)
                .map { logged -> LoginResponseMsg(logged) }
            is GoToMainCmd -> {
                inView {
                    navigator.goToMainScreen()
                }
            }
            else -> Single.just(Idle)
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

    fun addLoginInput(logintextViewText: Observable<CharSequence>): Disposable {
        return logintextViewText.skip(1).subscribe { login ->
            program.accept(LoginInputMsg(login.toString()))
        }
    }

    fun addPasswordInput(passValueObservable: Observable<CharSequence>): Disposable {
        return passValueObservable.skip(1).subscribe { pass ->
            program.accept(PassInputMsg(pass.toString()))
        }
    }

    fun destroy() {
        program.stop()
    }
}
