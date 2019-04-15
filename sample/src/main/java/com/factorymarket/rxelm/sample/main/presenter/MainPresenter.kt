package com.factorymarket.rxelm.sample.main.presenter


import com.factorymarket.rxelm.cmd.CancelByClassCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.RenderableComponent
import com.factorymarket.rxelm.contract.Update
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.IApiService
import com.factorymarket.rxelm.sample.main.model.CancelMsg
import com.factorymarket.rxelm.sample.main.model.LoadReposCmd
import com.factorymarket.rxelm.sample.main.model.MainState
import com.factorymarket.rxelm.sample.main.model.RefreshMsg
import com.factorymarket.rxelm.sample.main.model.ReposLoadedMsg
import com.factorymarket.rxelm.sample.main.view.IMainView
import com.factorymarket.rxelm.sample.navigation.Navigator
import io.reactivex.Single
import org.eclipse.egit.github.core.Repository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainPresenter @Inject constructor(
    val view: IMainView,
    programBuilder: ProgramBuilder,
    private val service: IApiService,
    private val navigator: Navigator
) : RenderableComponent<MainState> {

    private val program: Program<MainState> = programBuilder.build(this)

    fun init(initialState: MainState?) {
        program.run(initialState ?: MainState(userName = service.getUserName()))
    }

    override fun update(msg: Msg, state: MainState): Update<MainState> {
        return when (msg) {
            is Init -> Update.update(state.copy(isLoading = true), LoadReposCmd(state.userName))
            is ReposLoadedMsg -> Update.state(state.copy(isLoading = false, reposList = msg.reposList))
            is CancelMsg -> Update.update(state.copy(isLoading = false), CancelByClassCmd(cmdClass = LoadReposCmd::class))
            is RefreshMsg -> Update.update(state.copy(isLoading = true, reposList = listOf()), LoadReposCmd(state.userName))
            else -> Update.idle()
        }
    }

    override fun render(state: MainState) {
        state.apply {
            view.setTitle(state.userName + "'s starred repos")

            if (isLoading) {
                view.showErrorText(false)
                if (reposList.isEmpty()) {
                    view.showProgress()
                }
            } else {
                view.hideProgress()
                if (reposList.isEmpty()) {
                    view.setErrorText("User has no starred repos")
                    view.showErrorText(true)
                }
            }
            view.setRepos(reposList)
        }
    }

    fun render() {
        program.render()
    }

    override fun call(cmd: Cmd): Single<Msg> {
        return when (cmd) {
            is LoadReposCmd -> service.getStarredRepos(cmd.userName).delay(2,TimeUnit.SECONDS)
                .map { repos -> ReposLoadedMsg(repos) }
            else -> Single.just(Idle)
        }
    }

    fun destroy() {
        program.stop()
    }

    fun refresh() {
        program.accept(RefreshMsg)
    }

    fun cancel() {
        program.accept(CancelMsg)
    }

    fun onRepoItemClick(repository: Repository) {
        navigator.goToRepo(repository)
    }

}
