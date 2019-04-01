package com.factorymarket.rxelm.components.paging

import com.factorymarket.rxelm.cmd.BatchCmd
import com.factorymarket.rxelm.cmd.CancelCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.contract.PluginComponent
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.statelessEffect
import io.reactivex.Single

class PagingComponent<T, FETCH_PARAMS>(
    private val cmdHandlerPaging: PagingCommandsHandler<T, FETCH_PARAMS>,
    private val fetchParams: FETCH_PARAMS,
    private val errorLogger : ErrorLogger? = null,
    private val namespace: String = ""
) : PluginComponent<PagingState<T, FETCH_PARAMS>> {


    override fun initialState(): PagingState<T, FETCH_PARAMS> {
        return PagingState(fetchParams = fetchParams)
    }

    override fun handlesMessage(msg: Msg): Boolean {
        return msg is PagingMsg && msg.namespace == namespace
    }

    override fun handlesCommands(cmd: Cmd): Boolean {
        return cmd is PagingCmd && cmd.namespace == namespace || (cmd is LogThrowableCmd && cmd.ns == namespace)
    }

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    override fun update(msg: Msg, state: PagingState<T, FETCH_PARAMS>): Pair<PagingState<T, FETCH_PARAMS>, Cmd> =
        when (msg) {
            is PagingStartMsg -> startPaging(state)

            is PagingStartWithParamsMsg<*, *> -> startPaging(state, msg.params as? FETCH_PARAMS)

            is PagingOnScrolledToEndMsg -> loadNextPage(state)

            is PagingOnLoadedItemsMsg<*> -> itemsLoaded(state, state.items + msg.items, msg.totalPages)

            is PagingOnRefreshedItemsMsg<*> -> itemsLoaded(state, msg.items, msg.totalPages, msg.totalCount)

            is PagingOnRetryListButtonClickMsg -> loadNextPage(state)
            is PagingOnRetryAfterFullscreenErrorButtonClickMsg ->
                state.toFullscreenLoadingState().copy(isPageLoading = true) to PagingRefreshItemsCmd(
                    state.fetchParams,
                    ns = namespace
                )
            is PagingOnSwipeMsg -> state.toRefreshingState().copy(isPageLoading = true) to BatchCmd(
                //no matter what params we pass except namespace, since we've override hashcode() method
                CancelCmd(PagingLoadItemsCmd(1, state.fetchParams, namespace)),
                CancelCmd(PagingRefreshItemsCmd(state.fetchParams, namespace)),
                PagingRefreshItemsCmd(state.fetchParams, ns = namespace)
            )
            is PagingErrorMsg -> onErrorMsg(msg, state)
            else -> throw IllegalArgumentException("Unsupported message $msg")
        }

    private fun startPaging(state: PagingState<T, FETCH_PARAMS>, fetchParams: FETCH_PARAMS? = null):
            Pair<PagingState<T, FETCH_PARAMS>, Cmd> {

        return state.toFullscreenLoadingState()
            .copy(
                isPageLoading = true,
                isStarted = true,
                fetchParams = fetchParams ?: state.fetchParams
            ) to BatchCmd(
            //no matter what params we pass except namespace, since we've override hashcode() method
            CancelCmd(PagingLoadItemsCmd(1, state.fetchParams, namespace)),
            CancelCmd(PagingRefreshItemsCmd(state.fetchParams, namespace)),
            PagingRefreshItemsCmd(fetchParams ?: state.fetchParams, ns = namespace)
        )
    }

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    fun itemsLoaded(state: PagingState<T, FETCH_PARAMS>, items: List<Any?>, totalPages: Int, totalCount: Int? = null):
            Pair<PagingState<T, FETCH_PARAMS>, None> {
        val newState = state.copy(
            items = items as List<T>,
            totalPages = totalPages,
            isPageLoading = false,
            totalCount = totalCount ?: state.totalCount
        )
        return if (newState.hasLoadedAllItems()) {
            newState.toCompletelyLoadedState()
        } else {
            newState.toLoadingState()
        } to None
    }

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    override fun call(cmd: Cmd): Single<Msg> = when (cmd) {
        is PagingLoadItemsCmd<*> -> cmdHandlerPaging.fetchPage(cmd.page, cmd.params as FETCH_PARAMS).map {
            PagingOnLoadedItemsMsg(
                it.items,
                it.totalPages,
                cmd.page,
                ns = namespace
            ) as Msg
        }.onErrorResumeNext { Single.just(PagingErrorMsg(it, cmd, ns = namespace)) }

        is PagingRefreshItemsCmd<*> -> cmdHandlerPaging.fetchPage(1, cmd.params as FETCH_PARAMS).map {
            PagingOnRefreshedItemsMsg(
                it.items,
                it.totalPages,
                it.totalCount,
                ns = namespace
            ) as Msg
        }.onErrorResumeNext { Single.just(PagingErrorMsg(it, cmd, ns = namespace)) }

        is LogThrowableCmd -> statelessEffect {
            errorLogger?.logError(cmd.error)
        }
        else -> throw IllegalArgumentException("Unsupported message $cmd")
    }

    private fun onErrorMsg(
        msg: PagingErrorMsg,
        state: PagingState<T, FETCH_PARAMS>
    ) = when (msg.cmd) {
        is PagingLoadItemsCmd<*> -> {
            if (state.items.isEmpty()) {
                state.toErrorState()
            } else {
                state.toRetryState()
            }.copy(nextPage = state.nextPage.dec()) to LogThrowableCmd(msg.err)
        }
        is PagingRefreshItemsCmd<*> -> state.toErrorState() to LogThrowableCmd(msg.err)
        else -> throw IllegalArgumentException("Can't handle msg $msg")
    }

    private fun <T> loadNextPage(state: PagingState<T, FETCH_PARAMS>): Pair<PagingState<T, FETCH_PARAMS>, Cmd> {
        if (state.isPageLoading) {
            return state to None
        }

        return state.toLoadingState().copy(
            nextPage = state.nextPage.inc(), isPageLoading = true

        ) to PagingLoadItemsCmd(state.nextPage, state.fetchParams, ns = namespace)
    }
}
