package com.factorymarket.rxelm.sample.login

import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.IApiService
import com.factorymarket.rxelm.sample.data.IAppPrefs
import com.factorymarket.rxelm.sample.login.model.GetSavedUserCmd
import com.factorymarket.rxelm.sample.login.model.GoToMainCmd
import com.factorymarket.rxelm.sample.login.model.LoginCmd
import com.factorymarket.rxelm.sample.login.model.LoginResponseMsg
import com.factorymarket.rxelm.sample.login.model.LoginState
import com.factorymarket.rxelm.sample.login.model.SaveUserCredentialsCmd
import com.factorymarket.rxelm.sample.login.model.UserCredentialsLoadedMsg
import com.factorymarket.rxelm.sample.login.presenter.LoginPresenter
import com.factorymarket.rxelm.sample.login.view.ILoginView
import com.factorymarket.rxelm.sample.navigation.Navigator
import com.factorymarket.rxelm.test.RxElmSpec
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class LoginPresenterTest {

    lateinit var presenter: LoginPresenter
    lateinit var view: ILoginView
    lateinit var loginService: IApiService
    lateinit var prefs: IAppPrefs
    lateinit var program: Program<LoginState>
    lateinit var spec : RxElmSpec<LoginState>

    @Before
    fun setUp() {
        view = mock(ILoginView::class.java)
        loginService = mock(IApiService::class.java)
        prefs = mock(IAppPrefs::class.java)
        program = ProgramBuilder().outputScheduler(Schedulers.trampoline()).build()
        presenter = LoginPresenter(view, program, prefs, loginService, mock(Navigator::class.java))
        spec = RxElmSpec(presenter).withState(LoginState())
    }

    @Test
    fun initWithSavedLogin_HaveSavedCredentials_LoginOk() {
        //login screen init and look for saved credentials in preferences
        spec.whenMsg(Init)
            .diffState { it.copy(isLoading = true) }
            .andCmd(GetSavedUserCmd)

        //credentials loaded and start auth http call
        spec.whenMsg(UserCredentialsLoadedMsg("login", "password"))
            .diffState {
                it.copy(login = "login", pass = "password")
            }.andCmd(LoginCmd("login","password"))

        //auth OK, checkbox is on, then save to prefs
        spec.copy()
            .withState(spec.state().copy(saveUser = true))
            .whenMsg(LoginResponseMsg(true))
            .diffState { it } //state no changed
            .andCmd(SaveUserCredentialsCmd("login","password"))


        //auth OK, go to main screen
        spec.whenMsg(LoginResponseMsg(true))
            .diffState { it } //state no changed
            .andCmd(GoToMainCmd)

    }
}