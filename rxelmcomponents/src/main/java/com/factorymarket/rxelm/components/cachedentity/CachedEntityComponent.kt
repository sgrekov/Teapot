package com.factorymarket.rxelm.components.cachedentity

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.contract.PluginComponent
import com.factorymarket.rxelm.msg.Msg
import io.reactivex.Single


class CachedEntityComponent<T, FETCH_PARAMS>(
    private val params: FETCH_PARAMS,
    private val commandHandler: CachedEntityCommandHandler<T, FETCH_PARAMS>,
    private val namespace: String = ""
) : PluginComponent<CachedEntityState<T, FETCH_PARAMS>> {

    override fun handlesMessage(msg: Msg): Boolean = msg is CachedEntityMsg && msg.namespace == namespace

    override fun handlesCommands(cmd: Cmd): Boolean = cmd is CachedEntityCmd && cmd.namespace == namespace

    override fun initialState(): CachedEntityState<T, FETCH_PARAMS> = CachedEntityState.initial(params)

    @Suppress("UnsafeCast")
    override fun call(cmd: Cmd): Single<Msg> {
        return when (val cached = cmd as CachedEntityCmd) {
            is LoadFromCacheCmd<*> -> commandHandler.retrieveFromCache(cached.params as FETCH_PARAMS)
                .map { EntityLoadedFromCacheMsg(it) as Msg }
                .onErrorResumeNext { Single.just(EntityNotFoundInCacheMsg(ns = namespace)) }
            is LoadFromCloudCmd<*> -> commandHandler.fetchFromCloud(cached.params as FETCH_PARAMS)
                .map { EntityLoadedFromCloudMsg(it) as Msg }
                .onErrorResumeNext { Single.just(ErrorWhileLoadingFromCloudMsg(it, ns = namespace)) }
        }
    }

    @Suppress("UnsafeCast")
    override fun update(
        msg: Msg,
        state: CachedEntityState<T, FETCH_PARAMS>
    ): Pair<CachedEntityState<T, FETCH_PARAMS>, Cmd> {
        return when (val cachedMsg = msg as CachedEntityMsg) {
            is StartLoadMsg<*> -> state to LoadFromCacheCmd(state.fetchParams, namespace)
            is EntityLoadedFromCacheMsg<*> -> state.loadedFromCacheState(cachedMsg.entity as T) to LoadFromCloudCmd(
                state.fetchParams,
                namespace
            )
            is EntityNotFoundInCacheMsg -> state.loadedFromCacheState(null) to LoadFromCloudCmd(
                state.fetchParams,
                namespace
            )
            is EntityLoadedFromCloudMsg<*> -> state.syncedWithCloudState(entity = cachedMsg.entity as T) to None
            is ErrorWhileLoadingFromCloudMsg -> state.errorState(cachedMsg.throwable) to None
        }
    }
}