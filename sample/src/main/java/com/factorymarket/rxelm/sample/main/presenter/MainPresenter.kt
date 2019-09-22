package com.factorymarket.rxelm.sample.main.presenter


import com.factorymarket.rxelm.cmd.CancelByClassCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.ProxyCmd
import com.factorymarket.rxelm.components.paging.*
import com.factorymarket.rxelm.contract.PluggableFeature
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
import com.factorymarket.rxelm.sub.FlowSub
import com.paginate.Paginate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.egit.github.core.Repository
import timber.log.Timber
import javax.inject.Inject

class MainPresenter @Inject constructor(
        val view: IMainView,
        programBuilder: ProgramBuilder,
        private val service: RepoService,
        private val navigator: Navigator
) : PluggableFeature<MainState>, Renderable<MainState>, CoPagingCommandsHandler<Repository, String>, Paginate.Callbacks {

    private val program: CoroutineCompositeFeature<MainState> = CoroutineCompositeFeature(programBuilder, this)
    private val pagingFeature = CoPagingFeature(this, service.getUserName())

    override suspend fun fetchPage(page: Int, userName: String?): PagingResult<Repository> {
        return service.getStarredRepos2(userName, page)
    }

    fun flow1(): Flow<Msg> = flow {
        for (i in 1..5) {
            delay(1000)
            emit(SubMsg(i * 1000))
        }
    }

    fun flow2(): Flow<Msg> = flow {
        for (i in 1..5) {
            delay(1000)
            emit(Sub2Msg(i * -1000))
        }
    }

    fun init(restoredState: MainState?) {
        val flow1 = flow1()
        val flow2 = flow2()

        program.addComponent(pagingFeature,
                { mainState -> mainState.reposList },
                { repos, mainState -> mainState.copy(reposList = repos) })
        program.addMainComponent(this)
        program.run(
                initialState = restoredState ?: initialState(),
                initialMsg = PagingStartMsg(),
                sub = FlowSub<MainState>(Dispatchers.Main)
                        .addMessageFlow(flow1)
                        .addConditionalMessageObservable({ state -> state.reposList.items.isNotEmpty() }, flow2)
        )
    }

    override fun update(msg: Msg, state: MainState): Update<MainState> {
        return when (msg) {
            is CancelMsg -> Update.update(
                    state.copy(
                            isCanceled = true,
                            reposList = state.reposList.toErrorState()),
                    CancelByClassCmd(cmdClass = PagingRefreshItemsCmd::class))
            is RefreshMsg -> Update.update(state.copy(isCanceled = false), ProxyCmd(PagingStartMsg()))
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
                    view.setRepos(listOf())
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

    override fun onLoadMore() {
        program.accept(PagingOnScrolledToEndMsg())
    }

    override fun isLoading(): Boolean = program.state()?.reposList?.isBlockedForLoadingNextPage()
            ?: true

    override fun hasLoadedAllItems() = program.state()?.reposList?.hasLoadedAllItems() ?: false


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
