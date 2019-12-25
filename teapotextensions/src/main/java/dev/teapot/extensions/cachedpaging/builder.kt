package dev.teapot.extensions.cachedpaging

import dev.teapot.program.ProgramBuilder

fun <T, FETCH_PARAMS> ProgramBuilder.buildRxCachedPagingFeature(cmdHandlerPaging: CachedPagingCommandsHandler<T, FETCH_PARAMS>,
                                                                fetchParams: FETCH_PARAMS,
                                                                pageSize: Int): CachedPagingFeature<T, FETCH_PARAMS> {
    return CachedPagingFeature(cmdHandlerPaging, fetchParams, pageSize, logger)
}

