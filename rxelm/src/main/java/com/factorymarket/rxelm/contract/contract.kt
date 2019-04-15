package com.factorymarket.rxelm.contract

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.msg.Msg
import io.reactivex.Single

/**
 * Base class representing some state.
 */
open class State

/**
 * An object which renders [states][State], executes commands
 * and transform events ([Messages][Msg] into new [states][State] and [Commands][Cmd]
 */
interface Component<S : State> {

    /**
     * Pure function, returns a pair of [State] and [Command][Cmd]
     * @param msg a message (event) which comes from outer world (eg user, system, network etc)
     */
    fun update(msg: Msg, state: S): Update<S>

    /**
     * Execute given [Command][Cmd] and returns its answer as [Message][Msg]
     */
    fun call(cmd: Cmd): Single<Msg>
}

data class Update<S : State>(val updatedState : S?, val cmds : Cmd) {

    companion object {
        fun <S : State> idle() : Update<S> {
            return Update(null, None)
        }

        fun <S : State> state(newState : S) : Update<S> {
            return Update(newState, None)
        }

        fun <S : State> effect(cmd : Cmd) : Update<S> {
            return Update(null, cmd)
        }

        fun <S : State> update(newState : S, cmd : Cmd) : Update<S> {
            return Update(newState, cmd)
        }
    }
}


interface RenderableComponent<S : State> : Component<S> {

    /** Just render current state, no changes invoked */
    fun render(state: S)

}

interface Renderable<S : State> {

    /** Just render current state, no changes invoked */
    fun render(state: S)

    fun isRendering(): Boolean = false

}
