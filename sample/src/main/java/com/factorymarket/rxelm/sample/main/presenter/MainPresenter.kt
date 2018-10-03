package com.factorymarket.rxelm.sample.main.presenter


import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.contract.RenderableComponent
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.GitHubService
import com.factorymarket.rxelm.sample.main.model.LoadReposCmd
import com.factorymarket.rxelm.sample.main.model.MainState
import com.factorymarket.rxelm.sample.main.model.ReposLoadedMsg
import com.factorymarket.rxelm.sample.main.view.IMainView
import io.reactivex.Single

class MainPresenter(
    val view: IMainView,
    programBuilder: ProgramBuilder,
    val service: GitHubService
) : RenderableComponent<MainState> {

    private val program : Program<MainState> = programBuilder.build(this)

    fun init(initialState: MainState?) {
        program.run(initialState ?: MainState(userName = service.getUserName()))
    }

    override fun update(msg: Msg, state: MainState): Pair<MainState, Cmd> {
        return when (msg) {
            is Init -> state.copy(isLoading = true) to LoadReposCmd(state.userName)
            is ReposLoadedMsg -> state.copy(isLoading = false, reposList = msg.reposList) to None
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
            is LoadReposCmd -> service.getStarredRepos(cmd.userName).map { repos -> ReposLoadedMsg(repos) }
            else -> Single.just(Idle)
        }
    }

    fun destroy() {
        program.stop()
    }

    fun getState(): MainState {
        return program.state
    }

    fun render() {
        program.render()
    }

}
