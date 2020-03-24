package dev.teapot.feature


import dev.teapot.cmd.BatchCmd
import dev.teapot.cmd.Cmd
import dev.teapot.contract.*
import dev.teapot.msg.Init
import dev.teapot.msg.Msg
import dev.teapot.msg.ProxyMsg
import dev.teapot.program.Program
import dev.teapot.sub.Sub

abstract class CompositeFeature<S : State>(
        protected var renderer: Renderable<S>
) : Upd<S>, Renderable<S> {

    protected val components: MutableList<
            Triple<PluggableFeature<State, out Any>, ((mainState: S) -> State)?, ((subState: State, mainState: S) -> S)?>> =
            mutableListOf()

    protected val program: Program<S> by lazy(LazyThreadSafetyMode.NONE) { buildProgram() }

    abstract fun buildProgram(): Program<S>

    fun accept(msg: Msg) {
        program.accept(msg)
    }

    fun acceptCommand(cmd: Cmd) {
        accept(ProxyMsg(cmd))
    }

    fun state(): S? {
        return program.getState()
    }

    fun stop() {
        program.stop()
    }

    fun run(
            initialState: S,
            sub: Sub<S>? = null,
            initialMsg: Msg = Init
    ) {
        if (components.isEmpty()) {
            throw IllegalStateException("No components defined!")
        }
        program.run(initialState, sub, initialMsg)
    }

    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    fun <SS : State, P : Any> addFeature(
            pluggableFeature: PluggableFeature<SS, P>,
            toSubStateFun: (mainState: S) -> SS,
            toMainStateFun: (subState: SS, mainState: S) -> S
    ) {
        components.add(
                Triple(
                        pluggableFeature,
                        toSubStateFun,
                        toMainStateFun
                ) as Triple<PluggableFeature<State, P>, (mainState: S) -> State, (subState: State, mainState: S) -> S>
        )
    }

    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    fun <P : Any> addMainFeature(feature: PluggableFeature<S, P>) {
        components.add(
                Triple(
                        feature,
                        null,
                        null
                ) as Triple<PluggableFeature<State, P>, ((mainState: S) -> State)?, ((subState: State, mainState: S) -> S)?>
        )
    }

    override fun render(state: S) {
        renderer.render(state)
    }


    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    override fun update(msg: Msg, state: S): Update<S> {
        var combinedCmd = BatchCmd()
        var mainFeatureState = state

        components.forEach { (feature, toSubStateFun, toMainStateFun) ->
            if (feature.handlesMessage(msg)) {
                val featureStateBeforeUpdate = toSubStateFun?.invoke(mainFeatureState) ?: mainFeatureState
                val updateResult = feature.update(msg, featureStateBeforeUpdate)
                val featureStateAfterUpdate = updateResult.updatedState
                val updatedState = featureStateAfterUpdate ?: featureStateBeforeUpdate
                mainFeatureState = toMainStateFun?.invoke(updatedState, mainFeatureState)
                        ?: run { if (featureStateAfterUpdate != null) featureStateAfterUpdate as S else mainFeatureState }
                combinedCmd = combinedCmd.merge(updateResult.cmds)
            }
        }
        return Update.update(mainFeatureState, (combinedCmd as Cmd))
    }
}