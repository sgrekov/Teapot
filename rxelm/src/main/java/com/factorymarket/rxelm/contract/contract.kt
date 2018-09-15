package com.factorymarket.rxelm.contract

import com.factorymarket.rxelm.cmd.Cmd
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
    fun update(msg: Msg, state: S): Pair<S, Cmd>

    /**
     * Execute given [Command][Cmd] and returns its answer as [Message][Msg]
     */
    fun call(cmd: Cmd): Single<Msg>
}


interface RenderableComponent<S : State> : Component<S> {

    /** Just render current state, no changes invoked */
    fun render(state: S)

}
