package dev.teapot.sample.login

import dev.teapot.msg.Init
import dev.teapot.program.ProgramBuilder
import dev.teapot.sample.data.RepoService
import dev.teapot.sample.data.IAppPrefs
import dev.teapot.sample.login.model.GetSavedUserCmd
import dev.teapot.sample.login.model.GoToMainCmd
import dev.teapot.sample.login.model.LoginCmd
import dev.teapot.sample.login.model.LoginResponseMsg
import dev.teapot.sample.login.model.LoginState
import dev.teapot.sample.login.model.SaveUserCredentialsCmd
import dev.teapot.sample.login.model.UserCredentialsLoadedMsg
import dev.teapot.sample.login.feature.LoginFeature
import dev.teapot.sample.login.view.LoginView
import dev.teapot.sample.navigation.Navigator
import dev.teapot.test.TeapotSpec
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class LoginFeatureTest {

    lateinit var feature: LoginFeature
    lateinit var view: LoginView
    lateinit var loginService: RepoService
    lateinit var prefs: IAppPrefs
    lateinit var programBuilder: ProgramBuilder
    lateinit var spec : TeapotSpec<LoginState>

    @Before
    fun setUp() {
        view = mock(LoginView::class.java)
        loginService = mock(RepoService::class.java)
        prefs = mock(IAppPrefs::class.java)
        programBuilder = ProgramBuilder().outputDispatcher(Dispatchers.Main)
        feature = LoginFeature(view, programBuilder, prefs, loginService, mock(Navigator::class.java))
        spec = TeapotSpec(feature).withState(LoginState())
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