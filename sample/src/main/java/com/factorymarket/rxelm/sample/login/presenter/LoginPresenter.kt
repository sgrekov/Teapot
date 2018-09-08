package com.factorymarket.rxelm.sample.login.presenter

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.msg.Idle
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
import timber.log.Timber

class LoginPresenter(
    private val loginView: ILoginView,
    private val program: Program<LoginState>,
    private val appPrefs: IAppPrefs,
    private val apiService: IApiService,
    private val navigator: Navigator
) : Component<LoginState> {

    var programDisposable: Disposable

    init {
        programDisposable = program.init(LoginState(), this)
    }

    fun init() {
        program.accept(Init)
    }


    override fun update(msg: Msg, state: LoginState): Pair<LoginState, Cmd> {
        return when (msg) {
            is Init -> state.copy(isLoading = true) to GetSavedUserCmd
            is UserCredentialsLoadedMsg ->
                state.copy(login = msg.login, pass = msg.pass) to LoginCmd(msg.login, msg.pass)
            is LoginResponseMsg -> {
                if (state.saveUser) {
                    state to SaveUserCredentialsCmd(state.login, state.pass)
                } else {
                    state to GoToMainCmd
                }
            }
            is UserCredentialsSavedMsg -> state to GoToMainCmd
            is IsSaveCredentialsMsg -> Pair(state.copy(saveUser = msg.checked), None)
            is LoginInputMsg -> {
                if (!validateLogin(msg.login))
                    state.copy(login = msg.login, btnEnabled = false) to None
                else
                    state.copy(login = msg.login, loginError = null, btnEnabled = validatePass(state.pass)) to None
            }
            is PassInputMsg -> {
                if (!validatePass(msg.pass))
                    state.copy(pass = msg.pass, btnEnabled = false) to None
                else
                    state.copy(pass = msg.pass, btnEnabled = validateLogin(state.login)) to None
            }
            is LoginClickMsg -> {
                if (checkLogin(state.login)) {
                    state.copy(loginError = "Login is not valid") to None
                }
                if (checkPass(state.pass)) {
                    state.copy(passError = "Password is not valid") to None
                }
                state.copy(isLoading = true, error = null) to LoginCmd(state.login, state.pass)
            }
            is ErrorMsg -> {
                Timber.e(msg.err)
                return when (msg.cmd) {
                    is GetSavedUserCmd -> state.copy(isLoading = false) to None
                    is LoginCmd -> {
                        if (msg.err is RequestException) {
                            state.copy(isLoading = false, error = (msg.err as RequestException).error.message) to None
                        }
                        state.copy(isLoading = false, error = "Error while login") to None
                    }
                    else -> state to None
                }
            }
            else -> state to None
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
        programDisposable.dispose()
    }
}
