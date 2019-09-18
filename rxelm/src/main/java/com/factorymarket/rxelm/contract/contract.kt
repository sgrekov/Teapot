package com.factorymarket.rxelm.contract

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.msg.Msg

/**
 * Base class representing some state.
 */
open class State

/**
 * An object which renders state[State], executes commands
 * and transform messages [Msg] into new state [State] and emits side effects[Cmd]
 */

interface Update1<S : State> {
    /**
     * Pure function, returns a pair of [State] and Command[Cmd]
     * @param msg a message (event) which comes from outer world (eg user, system, network etc)
     */
    fun update(msg: Msg, state: S): Update<S>
}

class Update<S : State> private constructor(val updatedState: S?, val cmds: Cmd) {

    companion object {
        fun <S : State> idle(): Update<S> {
            return Update(null, None)
        }

        fun <S : State> state(newState: S): Update<S> {
            return Update(newState, None)
        }

        fun <S : State> effect(cmd: Cmd): Update<S> {
            return Update(null, cmd)
        }

        fun <S : State> update(newState: S, cmd: Cmd): Update<S> {
            return Update(newState, cmd)
        }
    }
}


interface Renderable<S : State> {

    /** Just render current state, no changes invoked */
    fun render(state: S)

    fun isRendering(): Boolean = false

}
