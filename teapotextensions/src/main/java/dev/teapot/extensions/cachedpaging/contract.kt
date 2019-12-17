package dev.teapot.extensions.cachedpaging

import io.reactivex.Single

data class CachedPagingResult<T>(val items: List<T>)
data class CachedPagingSyncResult<T>(val items: List<T>, val totalPages: Int)

/**
 * In order for cache paging worked properly, fetchPage method must save fetched items to cache
 */
interface CachedPagingCommandsHandler<T, PARAMS> {

    fun getCachedItems(limit: Int, params: PARAMS): Single<CachedPagingResult<T>>

    fun fetchPage(page: Int, pageSize: Int, params: PARAMS): Single<CachedPagingSyncResult<T>>
}