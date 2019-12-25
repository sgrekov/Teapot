package dev.teapot.extensions.paging

import dev.teapot.msg.Idle
import dev.teapot.msg.Msg
import dev.teapot.cmd.Cmd
import dev.teapot.contract.CoroutinesEffectHandler
import dev.teapot.log.TeapotLogger

class CoPagingFeature<T, FETCH_PARAMS>(
        private val cmdHandlerPaging: CoPagingCommandsHandler<T, FETCH_PARAMS>,
        fetchParams: FETCH_PARAMS?,
        errorLogger: TeapotLogger? = null,
        namespace: String = ""
) : PagingFeature<T, FETCH_PARAMS>(fetchParams, errorLogger, namespace), CoroutinesEffectHandler {

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    override suspend fun call(cmd: Cmd): Msg = when (cmd) {
        is PagingLoadItemsCmd<*> -> {
            try {
                val result = cmdHandlerPaging.fetchPage(cmd.page, cmd.params as? FETCH_PARAMS)

                PagingOnLoadedItemsMsg(
                        result.items,
                        result.totalPages,
                        cmd.page,
                        ns = namespace)
            } catch (err: Exception) {
                PagingErrorMsg(err, cmd, ns = namespace)
            }
        }

        is PagingRefreshItemsCmd<*> -> {
            try {
                val result = cmdHandlerPaging.fetchPage(1, cmd.params as? FETCH_PARAMS)

                PagingOnRefreshedItemsMsg(
                        result.items,
                        result.totalPages,
                        result.totalCount,
                        ns = namespace
                )
            } catch (err: Exception) {
                PagingErrorMsg(err, cmd, ns = namespace)
            }
        }
        is LogThrowableCmd -> {
            errorLogger?.error("PagingState", cmd.error)
            Idle
        }
        else -> throw IllegalArgumentException("Unsupported message $cmd")
    }

}

interface CoPagingCommandsHandler<T, PARAMS> {

    suspend fun fetchPage(page: Int, params: PARAMS?): PagingResult<T>
}