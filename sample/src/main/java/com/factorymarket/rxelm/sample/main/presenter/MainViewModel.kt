package com.factorymarket.rxelm.sample.main.presenter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.contract.RenderableComponent
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.IApiService
import com.factorymarket.rxelm.sample.main.model.LoadReposCmd
import com.factorymarket.rxelm.sample.main.model.MainState
import com.factorymarket.rxelm.sample.main.model.ReposLoadedMsg
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

class MainViewModel @Inject constructor(
    programBuilder: ProgramBuilder,
    private val service: IApiService
) : ViewModel(), RenderableComponent<MainState> {

    private val program: Program<MainState> = programBuilder.build(this)

    var stateLiveData : MutableLiveData<MainState> = MutableLiveData()

    init {
        Timber.tag("MainState").d("MainViewModel create")
    }

    fun init(initialState: MainState?) {
        Timber.tag("MainState").d("init")
        program.run(initialState ?: MainState(userName = service.getUserName()))
    }

    override fun update(msg: Msg, state: MainState): Pair<MainState, Cmd> {
        return when (msg) {
            is Init -> state.copy(isLoading = true) to LoadReposCmd(state.userName)
            is ReposLoadedMsg -> state.copy(isLoading = false, reposList = msg.reposList) to None
            is ErrorMsg -> {
                Timber.e(msg.err)
                state to None
            }
            else -> state to None
        }
    }

    override fun render(state: MainState) {
        stateLiveData.value = state
    }

    override fun call(cmd: Cmd): Single<Msg> {
        return when (cmd) {
            is LoadReposCmd -> service.getStarredRepos(cmd.userName).map { repos ->
                ReposLoadedMsg(
                    repos
                )
            }
            else -> Single.just(Idle)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.tag("MainState").d("onCleared")
        program.stop()
    }

}