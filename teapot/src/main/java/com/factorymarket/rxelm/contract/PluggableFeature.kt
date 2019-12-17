package com.factorymarket.rxelm.contract

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.msg.Msg

interface PluggableFeature<S : State> : Upd<S> {

    /**
     * Optional method.
     * This is useful when there several identical Components in CompositeFeature, in order not to intercept
     * messages of each other
     */
    fun handlesMessage(msg: Msg): Boolean

    /**
     * Optional method.
     * This is useful when there several identical Components in CompositeFeature, in order not to intercept
     * commands of each other
     */
    fun handlesCommands(cmd: Cmd): Boolean

    fun initialState(): S
}