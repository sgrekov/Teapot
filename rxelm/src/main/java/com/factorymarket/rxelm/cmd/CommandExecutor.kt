package com.factorymarket.rxelm.cmd

import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.program.MessageConsumer

interface CommandExecutor<S : State> {

    fun executeCmd(cmd: Cmd)

    fun stop()

    fun addMessageConsumer(mc : MessageConsumer)

}