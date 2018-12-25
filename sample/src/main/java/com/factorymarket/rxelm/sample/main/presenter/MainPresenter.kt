package com.factorymarket.rxelm.sample.main.presenter


import com.factorymarket.rxelm.cmd.CancelCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.contract.RenderableComponent
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.GitHubService
import com.factorymarket.rxelm.sample.data.IApiService
import com.factorymarket.rxelm.sample.main.model.CancelMsg
import com.factorymarket.rxelm.sample.main.model.LoadReposCmd
import com.factorymarket.rxelm.sample.main.model.MainState
import com.factorymarket.rxelm.sample.main.model.ReposLoadedMsg
import com.factorymarket.rxelm.sample.main.view.IMainView
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainPresenter @Inject constructor(
    val view: IMainView,
    programBuilder: ProgramBuilder,
    private val service: IApiService
) : RenderableComponent<MainState> {

    private val program: Program<MainState> = programBuilder.build(this)

    fun init(initialState: MainState?) {
        program.run(initialState ?: MainState(userName = service.getUserName()))
    }

    override fun update(msg: Msg, state: MainState): Pair<MainState, Cmd> {
        return when (msg) {
            is Init -> state.copy(isLoading = true) to LoadReposCmd(state.userName)
            is ReposLoadedMsg -> state.copy(isLoading = false, reposList = msg.reposList) to None
            is CancelMsg -> state to CancelCmd(LoadReposCmd(state.userName))
            else -> state to None
        }
    }

    override fun render(state: MainState) {
        state.apply {
            view.setTitle(state.userName + "'s starred repos")

            if (isLoading) {
                if (reposList.isEmpty()) {
                    view.showProgress()
                }
            } else {
                view.hideProgress()
                if (reposList.isEmpty()) {
                    view.setErrorText("User has no starred repos")
                    view.showErrorText()
                }
            }
            view.setRepos(reposList)
        }
    }

    override fun call(cmd: Cmd): Single<Msg> {
        return when (cmd) {
            is LoadReposCmd -> service.getStarredRepos(cmd.userName).delay(
                10,
                TimeUnit.SECONDS
            ).map { repos -> ReposLoadedMsg(repos) }
            else -> Single.just(Idle)
        }
    }

    fun destroy() {
        program.stop()
    }

    fun send() {
//        program.accept(SendMsg)
    }

    fun cancel() {
        program.accept(CancelMsg)
    }

}
