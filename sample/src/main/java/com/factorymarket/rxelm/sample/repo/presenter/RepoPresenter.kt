package com.factorymarket.rxelm.sample.repo.presenter

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.IApiService
import com.factorymarket.rxelm.sample.repo.model.InitRepo
import com.factorymarket.rxelm.sample.repo.model.LoadRepo
import com.factorymarket.rxelm.sample.repo.model.RepoLoaded
import com.factorymarket.rxelm.sample.repo.model.RepoState
import com.factorymarket.rxelm.sample.repo.view.IRepoView
import io.reactivex.Single
import org.eclipse.egit.github.core.RepositoryId
import javax.inject.Inject
import javax.inject.Named

class RepoPresenter @Inject constructor(
        private val view: IRepoView,
        @Named("repo_id") private val repoId: String,
        programBuilder: ProgramBuilder,
        private val apiService: IApiService
) : CoroutineComponent<RepoState>, Renderable<RepoState> {

    override fun render(state: RepoState) {
        view.showLoading(state.isLoading)
        state.repository?.let(view::showRepo)
    }

    private val program: Program<RepoState> = programBuilder.build(this)

    fun init() {
        program.run(initialState = initialState(), initialMsg = InitRepo)
    }

    fun initialState(): RepoState {
        return RepoState(openRepoId = repoId, isLoading = true)
    }

    override suspend fun callCoroutine(cmd: Cmd): Msg {
        return when (cmd) {
            is LoadRepo -> RepoLoaded(apiService.getRepo2(RepositoryId.createFromUrl(cmd.id)))
            else -> Idle
        }
    }

    override fun update(msg: Msg, state: RepoState): Update<RepoState> {
        return when (msg) {
            is InitRepo -> Update.update(state.copy(isLoading = true), LoadRepo(state.openRepoId))
            is RepoLoaded -> Update.state(state.copy(isLoading = false, repository = msg.repo))
            else -> Update.idle()
        }
    }

    fun destroy() {
        program.stop()
    }


}