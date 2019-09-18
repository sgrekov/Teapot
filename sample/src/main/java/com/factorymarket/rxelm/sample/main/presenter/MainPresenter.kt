package com.factorymarket.rxelm.sample.main.presenter


import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.components.paging.*
import com.factorymarket.rxelm.contract.PluginUpdate
import com.factorymarket.rxelm.contract.Renderable
import com.factorymarket.rxelm.contract.Update
import com.factorymarket.rxelm.feature.CoroutineCompositeFeature
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.ProgramBuilder
import com.factorymarket.rxelm.sample.data.RepoService
import com.factorymarket.rxelm.sample.main.model.*
import com.factorymarket.rxelm.sample.main.view.IMainView
import com.factorymarket.rxelm.sample.navigation.Navigator
import org.eclipse.egit.github.core.Repository
import timber.log.Timber
import javax.inject.Inject

class MainPresenter @Inject constructor(
        val view: IMainView,
        programBuilder: ProgramBuilder,
        private val service: RepoService,
        private val navigator: Navigator
) : PluginUpdate<MainState>, Renderable<MainState>, CoPagingCommandsHandler<Repository, String> {

    private val program: CoroutineCompositeFeature<MainState> = CoroutineCompositeFeature(programBuilder, this)
    private val pagingFeature = CoPagingFeature(this, service.getUserName())

    override suspend fun fetchPage(page: Int, userName: String?): PagingResult<Repository> {
        return service.getStarredRepos2(userName, page)
    }

    fun init(restoredState: MainState?) {
        program.addComponent(pagingFeature,
                { mainState -> mainState.reposList },
                { repos, mainState -> mainState.copy(reposList = repos) })
        program.addMainComponent(this)
        program.run(restoredState ?: initialState(), initialMsg = PagingStartMsg())
    }

    override fun update(msg: Msg, state: MainState): Update<MainState> {
        return when (msg) {
//            is Init -> Update.update(state.copy(isLoading = true), LoadReposCmd(state.userName))
//            is ReposLoadedMsg -> Update.state(state.copy(isLoading = false, reposList = msg.reposList))
//            is CancelMsg -> Update.update(state.copy(isLoading = false, isCanceled = true), CancelByClassCmd(cmdClass = LoadReposCmd::class))
//            is RefreshMsg -> Update.update(state.copy(isLoading = true, isCanceled = false, reposList = listOf()), LoadReposCmd(state.userName))
            is PagingErrorMsg -> {
                Timber.e(msg.err)
                Update.idle()
            }
            is ErrorMsg -> {
                Timber.e(msg.err)
                Update.idle()
            }
            else -> Update.idle()
        }
    }

    override fun handlesMessage(msg: Msg): Boolean = true

    override fun handlesCommands(cmd: Cmd): Boolean = true

    override fun initialState(): MainState = MainState(userName = service.getUserName(), reposList = pagingFeature.initialState())

    override fun render(state: MainState) {
        state.apply {
            view.setTitle(state.userName + "'s starred repos")

            when {
                reposList.isFullscreenLoaderVisible -> {
                    view.showErrorText(false)
                    view.showProgress()
                }
                reposList.isErrorStateVisible || state.isCanceled || reposList.items.isEmpty() -> {
                    view.hideProgress()
                    view.setErrorText(if (state.isCanceled) "Request is canceled" else "User has no starred repos")
                    view.showErrorText(true)
                }
                else -> {
                    view.showErrorText(false)
                    view.hideProgress()
                    view.setRepos(reposList.items)
                }
            }
        }
    }

    fun render() {
        program.state()?.let(program::render)
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
