package com.factorymarket.rxelm.sample.main.presenter


import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.sample.data.GitHubService
import com.factorymarket.rxelm.sample.main.model.MainState
import com.factorymarket.rxelm.sample.main.view.IMainView
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import org.eclipse.egit.github.core.Repository

class MainPresenter(
    val view: IMainView,
    val program: Program<MainState>,
    val service: GitHubService
) : Component<MainState> {

    data class LoadReposCmd(val userName: String) : Cmd()

    data class ReposLoadedMsg(val reposList: List<Repository>) : Msg()

    lateinit var disposable: Disposable

    fun init(initialState: MainState?) {
        disposable =
                program.init(initialState ?: MainState(userName = service.getUserName()), this)

        initialState ?: program.accept(Init) //if no saved state, then run init Msg
    }

    override fun update(msg: Msg, state: MainState): Pair<MainState, Cmd> {
        return when (msg) {
            is Init -> state to LoadReposCmd(state.userName)
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
        disposable.dispose()
    }

    fun getState(): MainState {
        return program.state
    }

    fun render() {
        program.render()
    }

}
