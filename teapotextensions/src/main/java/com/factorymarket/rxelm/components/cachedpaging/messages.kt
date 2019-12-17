package com.factorymarket.rxelm.components.cachedpaging

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.msg.Msg

object CachedPagingStartMsg : Msg()
data class CachedPagingStartWithParamsMsg<PARAMS, FETCH_PARAMS>(
    val customParams: PARAMS? = null,
    val params: FETCH_PARAMS? = null
) : Msg()

object CachedPagingOnScrolledToEndMsg : Msg()
data class CachedPagingItemsLoadedFromCacheMsg<T>(
    val items: List<T>,
    val limitRequested: Int
) : Msg()

data class CachedPagingSyncedItemsMsg(
    val syncedPage: Int,
    val totalPages: Int
) : Msg()

data class CachedPagingErrorMsg(val err: Throwable, val cmd: Cmd) : Msg()
object CachedPagingOnRetryListButtonClickMsg : Msg()
object CachedPagingOnRetryAfterErrorButtonClickMsg : Msg()
object CachedPagingOnSwipeMsg : Msg()
object CachedPagingSyncAfterErrorMsg : Msg()
object CachedPagingReloadCacheMsg : Msg()