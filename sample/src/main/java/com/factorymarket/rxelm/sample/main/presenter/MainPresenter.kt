package com.factorymarket.rxelm.sample.main.presenter


import com.factorymarket.rxelm.cmd.CancelByClassCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.CoroutineComponent
import com.factorymarket.rxelm.contract.Renderable
import com.factorymarket.rxelm.contract.Update
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.IApiService
import com.factorymarket.rxelm.sample.main.model.*
import com.factorymarket.rxelm.sample.main.view.IMainView
import com.factorymarket.rxelm.sample.navigation.Navigator
import org.eclipse.egit.github.core.Repository
import timber.log.Timber
import javax.inject.Inject

class MainPresenter @Inject constructor(
        val view: IMainView,
        programBuilder: ProgramBuilder,
        private val service: IApiService,
        private val navigator: Navigator
) : CoroutineComponent<MainState>, Renderable<MainState> {

    private val program: Program<MainState> = programBuilder.build(this)

    fun init(initialState: MainState?) {
        program.run(initialState ?: MainState(userName = service.getUserName()))
    }

    override fun update(msg: Msg, state: MainState): Update<MainState> {
        return when (msg) {
            is Init -> Update.update(state.copy(isLoading = true), LoadReposCmd(state.userName))
            is ReposLoadedMsg -> Update.state(state.copy(isLoading = false, reposList = msg.reposList))
            is CancelMsg -> Update.update(state.copy(isLoading = false), CancelByClassCmd(cmdClass = LoadReposCmd::class))
            is RefreshMsg -> Update.update(state.copy(isLoading = true, isCanceled = false, reposList = listOf()), LoadReposCmd(state.userName))
            is ErrorMsg -> {
                Timber.e(msg.err)
                when (msg.cmd) {
                    is LoadReposCmd -> Update.state(state.copy(isCanceled = true))
                    else -> Update.idle()
                }
            }
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
                    view.setErrorText(if (state.isCanceled) "Request is canceled" else "User has no starred repos")
                    view.showErrorText(true)
                }
            }
            view.setRepos(reposList)
        }
    }

    fun render() {
        program.render()
    }

    override suspend fun callCoroutine(cmd: Cmd): Msg {
        return when (cmd) {
            is LoadReposCmd -> ReposLoadedMsg(service.getStarredRepos2(cmd.userName))
            else -> Idle
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
