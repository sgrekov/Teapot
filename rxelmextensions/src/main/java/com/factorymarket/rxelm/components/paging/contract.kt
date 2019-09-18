package com.factorymarket.rxelm.components.paging

import io.reactivex.Single

data class PagingResult<T>(val items: List<T>, val totalPages: Int, val totalCount: Int? = null)

interface RxPagingCommandsHandler<T, PARAMS> {

    fun fetchPage(page: Int, params: PARAMS): Single<PagingResult<T>>
}

interface CoPagingCommandsHandler<T, PARAMS> {

    suspend fun fetchPage(page: Int, params: PARAMS): PagingResult<T>
}

interface PagingView  {
    fun setRefreshingVisible(isVisible: Boolean)
    fun setRefreshingEnabled(isEnabled: Boolean)
    fun setErrorStateVisible(isVisible: Boolean)
    fun setFullScreenSpinnerVisible(isVisible: Boolean)
    fun setListVisible(isVisible: Boolean)
}

interface ErrorLogger {

    fun logError(err : Throwable)
}