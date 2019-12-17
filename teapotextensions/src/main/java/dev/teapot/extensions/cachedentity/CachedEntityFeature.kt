package dev.teapot.extensions.cachedentity

import dev.teapot.msg.Msg
import dev.teapot.cmd.Cmd
import dev.teapot.contract.PluggableFeature
import dev.teapot.contract.RxEffectHandler
import dev.teapot.contract.Update
import io.reactivex.Single


class CachedEntityFeature<T, FETCH_PARAMS>(
        private val params: FETCH_PARAMS,
        private val commandHandler: CachedEntityCommandHandler<T, FETCH_PARAMS>,
        private val namespace: String = ""
) : PluggableFeature<CachedEntityState<T, FETCH_PARAMS>>, RxEffectHandler {

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
    ): Update<CachedEntityState<T, FETCH_PARAMS>> {
        return when (val cachedMsg = msg as CachedEntityMsg) {
            is StartLoadMsg<*> -> Update.effect(LoadFromCacheCmd(state.fetchParams, namespace))
            is EntityLoadedFromCacheMsg<*> -> Update.update(
                state.loadedFromCacheState(cachedMsg.entity as T),
                    LoadFromCloudCmd(state.fetchParams, namespace)
            )
            is EntityNotFoundInCacheMsg -> Update.update(
                state.loadedFromCacheState(null),
                    LoadFromCloudCmd(state.fetchParams, namespace)
            )
            is EntityLoadedFromCloudMsg<*> -> Update.state(state.syncedWithCloudState(entity = cachedMsg.entity as T))
            is ErrorWhileLoadingFromCloudMsg -> Update.state(state.errorState(cachedMsg.throwable))
        }
    }
}