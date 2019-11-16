package com.factorymarket.rxelm.feature

import com.factorymarket.rxelm.cmd.BatchCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.msg.ProxyMsg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.sub.Sub

abstract class CompositeFeature<S : State>(
        protected var renderer: Renderable<S>
) : Upd<S>, Renderable<S> {

    protected val components: MutableList<
            Triple<PluggableFeature<State>, ((mainState: S) -> State)?, ((subState: State, mainState: S) -> S)?>> =
            mutableListOf()

    protected val program: Program<S> by lazy(LazyThreadSafetyMode.NONE) { buildProgram() }

    abstract fun buildProgram(): Program<S>

    fun accept(msg: Msg) {
        program.accept(msg)
    }

    fun acceptCommand(cmd : Cmd){
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
    fun <SS : State> addFeature(
            component: PluggableFeature<SS>,
            toSubStateFun: (mainState: S) -> SS,
            toMainStateFun: (subState: SS, mainState: S) -> S
    ) {
        components.add(
                Triple(
                        component,
                        toSubStateFun,
                        toMainStateFun
                ) as Triple<PluggableFeature<State>, (mainState: S) -> State, (subState: State, mainState: S) -> S>
        )
    }

    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    fun setMainFeature(component: PluggableFeature<S>) {
        components.add(
                Triple(
                        component,
                        null,
                        null
                ) as Triple<PluggableFeature<State>, ((mainState: S) -> State)?, ((subState: State, mainState: S) -> S)?>
        )
    }

    override fun render(state: S) {
        renderer.render(state)
    }


    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    override fun update(msg: Msg, state: S): Update<S> {
        var combinedCmd = BatchCmd()
        var mainComponentState = state

        components.forEach { (component, toSubStateFun, toMainStateFun) ->
            if (component.handlesMessage(msg)) {
                val componentStateBeforeUpdate = toSubStateFun?.invoke(mainComponentState) ?: mainComponentState
                val updateResult = component.update(msg, componentStateBeforeUpdate)
                val componentStateAfterUpdate = updateResult.updatedState
                val updatedState = componentStateAfterUpdate ?: componentStateBeforeUpdate
                mainComponentState = toMainStateFun?.invoke(updatedState, mainComponentState)
                        ?: run { if (componentStateAfterUpdate != null) componentStateAfterUpdate as S else mainComponentState }
                combinedCmd = combinedCmd.merge(updateResult.cmds)
            }
        }
        return Update.update(mainComponentState, (combinedCmd as Cmd))
    }
}