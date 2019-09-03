package com.factorymarket.rxelm.component

import com.factorymarket.rxelm.cmd.BatchCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.sub.Sub
import io.reactivex.Single

class CompositeComponent<S : State>(
    private val program: Program<S>,
//    private val programBuilder: ProgramBuilder,
    private var renderer: Renderable<S>
) : Component<S, Effect>, Renderable<S> {

    private val components: MutableList<
            Triple<PluginComponent<State>, ((mainState: S) -> State)?, ((subState: State, mainState: S) -> S)?>> =
        mutableListOf()

//    private val program: Program<S> = programBuilder.build(this)

    fun accept(msg: Msg) {
        program.accept(msg)
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
    fun <SS : State> addComponent(
        component: PluginComponent<SS>,
        toSubStateFun: (mainState: S) -> SS,
        toMainStateFun: (subState: SS, mainState: S) -> S
    ) {
        components.add(
            Triple(
                component,
                toSubStateFun,
                toMainStateFun
            ) as Triple<PluginComponent<State>, (mainState: S) -> State, (subState: State, mainState: S) -> S>
        )
    }

    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    fun addMainComponent(component: PluginComponent<S>) {
        components.add(
            Triple(
                component,
                null,
                null
            ) as Triple<PluginComponent<State>, ((mainState: S) -> State)?, ((subState: State, mainState: S) -> S)?>
        )
    }

    override fun render(state: S) {
        renderer.render(state)
    }

    override fun call(cmd: Cmd): Effect {
        components.forEach { (component, _) ->
            if (component.handlesCommands(cmd)) {
                return component.call(cmd)
            }
        }
        return RxEffect(Single.just(Idle)) //TODO: fix
    }

    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    override fun update(msg: Msg, state: S): Update<S> {
        var combinedCmd = BatchCmd()
        var combinedState = state
        components.forEach { (component, toSubStateFun, toMainStateFun) ->
            if (component.handlesMessage(msg)) {
                val subState = toSubStateFun?.let { it(combinedState) } ?: combinedState
                val (componentState, componentCmd) = component.update(msg, subState)
                val updatedState = componentState ?: subState
                combinedState = toMainStateFun?.let { it(updatedState, combinedState) } ?: componentState as S
                combinedCmd = combinedCmd.merge(componentCmd)
            }
        }
        return Update.update(combinedState, (combinedCmd as Cmd))
    }
}