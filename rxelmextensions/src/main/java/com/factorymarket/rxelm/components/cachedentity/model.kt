package com.factorymarket.rxelm.components.cachedentity

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.msg.Msg
import io.reactivex.Single

data class CachedEntityState<T, FETCH_PARAMS>(
    val entity: T?,
    val fetchParams: FETCH_PARAMS,
    val isLoadingFromCache: Boolean,
    val isLoadingFromCloud: Boolean,
    val error: Throwable?
) : State() {
    companion object {

        fun <T, FETCH_PARAMS> initial(params: FETCH_PARAMS): CachedEntityState<T, FETCH_PARAMS> {
            return CachedEntityState(
                entity = null,
                fetchParams = params,
                isLoadingFromCache = true,
                isLoadingFromCloud = true,
                error = null
            )
        }
    }

    fun isLoading(): Boolean {
        return (this.isLoadingFromCache || this.isLoadingFromCloud) && entity == null
    }

    fun loadedFromCacheState(entity: T?): CachedEntityState<T, FETCH_PARAMS> {
        return copy(
            entity = entity,
            isLoadingFromCache = false,
            isLoadingFromCloud = true
        )
    }

    fun syncedWithCloudState(entity: T): CachedEntityState<T, FETCH_PARAMS> {
        return copy(
            entity = entity,
            isLoadingFromCache = false,
            isLoadingFromCloud = false
        )
    }

    fun errorState(err: Throwable): CachedEntityState<T, FETCH_PARAMS> {
        return copy(
            entity = null,
            isLoadingFromCache = false,
            isLoadingFromCloud = false,
            error = err
        )
    }
}

sealed class CachedEntityMsg(val namespace: String) : Msg()
sealed class CachedEntityCmd(val namespace: String) : Cmd()

data class StartLoadMsg<FETCH_PARAMS>(val params: FETCH_PARAMS, val ns: String = "") : CachedEntityMsg(ns)
data class EntityLoadedFromCacheMsg<T>(val entity: T, val ns: String = "") : CachedEntityMsg(ns)
data class EntityNotFoundInCacheMsg(val ns: String = "") : CachedEntityMsg(ns)
data class EntityLoadedFromCloudMsg<T>(val entity: T, val ns: String = "") : CachedEntityMsg(ns)
data class ErrorWhileLoadingFromCloudMsg(val throwable: Throwable, val ns: String = "") : CachedEntityMsg(ns)

data class LoadFromCacheCmd<FETCH_PARAMS>(val params: FETCH_PARAMS, val ns: String = "") : CachedEntityCmd(ns)
data class LoadFromCloudCmd<FETCH_PARAMS>(val params: FETCH_PARAMS, val ns: String = "") : CachedEntityCmd(ns)

interface CachedEntityCommandHandler<T, FETCH_PARAMS> {

    fun retrieveFromCache(params: FETCH_PARAMS): Single<T>

    fun fetchFromCloud(params: FETCH_PARAMS): Single<T>
}

abstract class CloudEntityCommandHandler<T, FETCH_PARAMS> : CachedEntityCommandHandler<T, FETCH_PARAMS> {

    override fun retrieveFromCache(params: FETCH_PARAMS): Single<T> {
        return Single.error(NoSuchElementException("no need for cache lookup"))
    }
}
