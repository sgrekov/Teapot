package com.factorymarket.rxelm.components.paging

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.RxEffectHandler
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.statelessEffect
import io.reactivex.Single

class RxPagingFeature<T, FETCH_PARAMS>(
        private val cmdHandlerPaging: RxPagingCommandsHandler<T, FETCH_PARAMS>,
        fetchParams: FETCH_PARAMS?,
        errorLogger: ErrorLogger? = null,
        namespace: String = ""
) : PagingFeature<T, FETCH_PARAMS>(fetchParams, errorLogger, namespace), RxEffectHandler {

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

}

interface RxPagingCommandsHandler<T, PARAMS> {

    fun fetchPage(page: Int, params: PARAMS?): Single<PagingResult<T>>
}