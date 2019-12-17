package com.factorymarket.rxelm.components.cachedpaging

import com.factorymarket.rxelm.cmd.BatchCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.components.paging.ErrorLogger
import com.factorymarket.rxelm.components.paging.LogThrowableCmd
import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.statelessEffect
import io.reactivex.Single

/**
 * For cached paging we have 2 side effect-requests -
 * 1. Request for items in cache
 * 2. Request for items in server
 *
 * Algorithm for paging with cache:
 *
 * Let's assume we have 3 pages of elements
 * 1. On initial step we do two parallel requests for 1st page of cache and 1st page for server
 * 2. Once cache received, we show it.
 * Base idea is that we do not wait for response of syncing request. User can scroll down start to load next pages
 * of cache.
 * 3. Sync response arrived, we save items to cache, and sending another request to 1st cache of cache
 * 4. Fresh items from cache arrive
 * 5. User scrolls down to the bottom and we create another two requests - 1 and 2 page of cache, and sync request
 * for 2nd page
 * 6. Show cache 2 pages of cache
 * 7. Once sync response arrives, again create request for 1 and 2 pages of cache
 * 8. User scrolls down to the bottom and we create another two requests - 1 and 2 and 3 page of cache,
 * and sync request for 3nd page
 * 9. Show 3 pages of cache
 * 7. Once sync response arrives, again create request for 1 and 2 and 3 pages of cache
 * 8. Show 3 pages of cache and that's all
 */

class CachedPagingFeature<T, FETCH_PARAMS>(
    private val cmdHandlerPaging: CachedPagingCommandsHandler<T, FETCH_PARAMS>,
    private val params: FETCH_PARAMS,
    private val pageSize: Int,
    private val errorLogger: ErrorLogger? = null
) : PluggableFeature<CachedPagingState<T, FETCH_PARAMS>>, RxEffectHandler {

    private val messages = listOf(
        CachedPagingStartMsg::class,
        CachedPagingStartWithParamsMsg::class,
        CachedPagingOnScrolledToEndMsg::class,
        CachedPagingItemsLoadedFromCacheMsg::class,
        CachedPagingSyncedItemsMsg::class,
        CachedPagingOnRetryListButtonClickMsg::class,
        CachedPagingOnRetryAfterErrorButtonClickMsg::class,
        CachedPagingSyncAfterErrorMsg::class,
        CachedPagingOnSwipeMsg::class,
        CachedPagingErrorMsg::class,
        CachedPagingReloadCacheMsg::class
    )

    private val commands = listOf(
        CachedPagingLoadItemsFromCacheCmd::class,
        CachedPagingSyncItemsCmd::class,
        LogThrowableCmd::class
    )

    override fun initialState(): CachedPagingState<T, FETCH_PARAMS> {
        return CachedPagingState(fetchParams = params, pageSize = pageSize)
    }

    override fun handlesMessage(msg: Msg): Boolean {
        return messages.contains(msg::class)
    }

    override fun handlesCommands(cmd: Cmd): Boolean {
        return commands.contains(cmd::class)
    }

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    override fun update(
        msg: Msg,
        state: CachedPagingState<T, FETCH_PARAMS>
    ): Update<CachedPagingState<T, FETCH_PARAMS>> =
        when (msg) {
            is CachedPagingStartMsg -> startPaging(state)
            is CachedPagingStartWithParamsMsg<*, *> -> startPaging(
                state,
                msg.params as? FETCH_PARAMS
            )
            is CachedPagingOnScrolledToEndMsg -> loadNextPage(state)
            is CachedPagingItemsLoadedFromCacheMsg<*> -> itemsLoadedFromCache(
                state,
                msg.items as List<T>,
                msg.limitRequested
            )
            is CachedPagingSyncedItemsMsg -> Update.update(
                state.copy(
                    isSyncing = false,
                    isCacheLoading = true,
                    totalPages = msg.totalPages
                ), CachedPagingLoadItemsFromCacheCmd(
                    (state.nextPage - 1) * state.pageSize,
                    state.fetchParams
                )
            )
            is CachedPagingOnRetryListButtonClickMsg -> loadNextPage(state)
            is CachedPagingSyncAfterErrorMsg -> Update.update(
                state.copy(
                    nextPage = 2,
                    hasSyncError = false,
                    isSyncing = true,
                    isCacheLoading = true,
                    isRefreshingEnabled = true,
                    isRefreshingVisible = true
                ), CachedPagingSyncItemsCmd(1, state.pageSize, state.fetchParams)
            )
            is CachedPagingOnRetryAfterErrorButtonClickMsg -> startPaging(state)
            is CachedPagingOnSwipeMsg -> Update.update(
                state.toRefreshingState()
                    .copy(
                        isSyncing = true,
                        isCacheLoading = true
                    ), BatchCmd(
                    CachedPagingLoadItemsFromCacheCmd(1 * state.pageSize, state.fetchParams),
                    CachedPagingSyncItemsCmd(1, state.pageSize, state.fetchParams)
                )
            )
            is CachedPagingErrorMsg -> onErrorMsg(msg, state)
            is CachedPagingReloadCacheMsg -> Update.update(
                state.copy(isCacheLoading = true), CachedPagingLoadItemsFromCacheCmd(
                    (state.nextPage - 1) * state.pageSize,
                    state.fetchParams
                )
            )
            else -> throw IllegalArgumentException("Unsupported message $msg")
        }

    private fun startPaging(
        state: CachedPagingState<T, FETCH_PARAMS>,
        fetchParams: FETCH_PARAMS? = null
    ): Update<CachedPagingState<T, FETCH_PARAMS>> {
        if (state.isCacheLoading) {
            return Update.idle()
        }

        return Update.update(
            state.copy(
                nextPage = 2,
                hasSyncError = false,
                isSyncing = true,
                isCacheLoading = true,
                isStarted = true,
                fetchParams = fetchParams ?: state.fetchParams
            ), BatchCmd(
                CachedPagingLoadItemsFromCacheCmd(state.nextPage * state.pageSize, state.fetchParams),
                CachedPagingSyncItemsCmd(state.nextPage, state.pageSize, state.fetchParams)
            )
        )
    }

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    fun itemsLoadedFromCache(
        state: CachedPagingState<T, FETCH_PARAMS>, items: List<T>, limitRequested: Int
    ): Update<CachedPagingState<T, FETCH_PARAMS>> {
        val isResultForFirstPage = limitRequested == state.pageSize
        val isNoMoreCache = items.size < limitRequested

        return Update.state(state.run {
            return@run when {
                items.isEmpty() -> when {
                    isSyncing -> toFullscreenLoadingState()
                    hasSyncError -> toFullscreenErrorState()
                    else -> toNoItemsState()
                }
                hasSyncError && isNoMoreCache -> toListStateWithSyncError()
                hasSyncError -> toLoadingStateWithSyncError()
                isSyncing && isResultForFirstPage -> if (items.size == state.pageSize) {
                    toRefreshingStateWithSpinner()
                } else toRefreshingState()
                isSyncing -> toPageLoadingState()
                totalPages != null && nextPage > totalPages -> toCompletelyLoadedState()
                else -> toPageLoadingState()
            }
        }.copy(isCacheLoading = false, isNoMoreCache = isNoMoreCache, items = items))
    }

    @Suppress("UnsafeCast")
    override fun call(cmd: Cmd): Single<Msg> = when (cmd) {
        is CachedPagingLoadItemsFromCacheCmd<*> -> cmdHandlerPaging.getCachedItems(
            cmd.limit,
            cmd.params as FETCH_PARAMS
        ).map {
            CachedPagingItemsLoadedFromCacheMsg(
                it.items,
                cmd.limit
            ) as Msg
        }.onErrorResumeNext { Single.just(CachedPagingErrorMsg(it, cmd)) }

        is CachedPagingSyncItemsCmd<*> -> cmdHandlerPaging.fetchPage(
            cmd.page,
            cmd.pageSize,
            cmd.params as FETCH_PARAMS
        ).map {
            CachedPagingSyncedItemsMsg(
                cmd.page,
                it.totalPages
            ) as Msg
        }.onErrorResumeNext { Single.just(CachedPagingErrorMsg(it, cmd)) }

        is LogThrowableCmd -> statelessEffect {
            errorLogger?.logError(cmd.error)
        }
        else -> throw IllegalArgumentException("Unsupported message $cmd")
    }

    private fun onErrorMsg(
        msg: CachedPagingErrorMsg,
        state: CachedPagingState<T, FETCH_PARAMS>
    ) = when (msg.cmd) {
        is CachedPagingLoadItemsFromCacheCmd<*> -> {
            Update.update(
                if (state.items.isEmpty()) {
                    state.toFullscreenErrorState()
                } else {
                    state.toRetryPageState()
                }.copy(
                    nextPage = state.nextPage.dec(),
                    isCacheLoading = false
                ), LogThrowableCmd(msg.err)
            )
        }
        is CachedPagingSyncItemsCmd<*> -> Update.update(state.run {
            return@run when {
                items.isEmpty() && isCacheLoading -> toFullscreenLoadingState()
                items.isEmpty() -> toFullscreenErrorState()
                items.size < pageSize -> toListStateWithSyncError()
                else -> toLoadingStateWithSyncError()
            }
        }.copy(isSyncing = false, hasSyncError = true), LogThrowableCmd(msg.err))
        else -> throw IllegalArgumentException("Can't handle msg $msg")
    }

    private fun <T> loadNextPage(state: CachedPagingState<T, FETCH_PARAMS>):
            Update<CachedPagingState<T, FETCH_PARAMS>> {
        if (state.isCacheLoading) {
            return Update.idle()
        }

        return Update.update(
            state.toPageLoadingState().copy(
                nextPage = state.nextPage.inc(), isCacheLoading = true, isSyncing = true
            ), BatchCmd(
                CachedPagingLoadItemsFromCacheCmd(state.nextPage * state.pageSize, state.fetchParams),
                CachedPagingSyncItemsCmd(state.nextPage, state.pageSize, state.fetchParams)
            )
        )
    }
}