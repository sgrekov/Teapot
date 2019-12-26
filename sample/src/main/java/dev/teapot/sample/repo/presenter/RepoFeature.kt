package dev.teapot.sample.repo.presenter

import dev.teapot.msg.Idle
import dev.teapot.msg.Msg
import dev.teapot.program.Program
import dev.teapot.program.ProgramBuilder
import dev.teapot.sample.data.RepoService
import dev.teapot.sample.repo.model.InitRepo
import dev.teapot.sample.repo.model.LoadRepo
import dev.teapot.sample.repo.model.RepoLoaded
import dev.teapot.sample.repo.model.RepoState
import dev.teapot.sample.repo.view.RepoView
import dev.teapot.cmd.Cmd
import dev.teapot.contract.CoroutineFeature
import dev.teapot.contract.Renderable
import dev.teapot.contract.Update
import org.eclipse.egit.github.core.RepositoryId
import javax.inject.Inject
import javax.inject.Named

class RepoFeature @Inject constructor(
        private val view: RepoView,
        @Named("repo_id") private val repoId: String,
        programBuilder: ProgramBuilder,
        private val apiService: RepoService
) : CoroutineFeature<RepoState>, Renderable<RepoState> {

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

    override suspend fun call(cmd: Cmd): Msg {
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