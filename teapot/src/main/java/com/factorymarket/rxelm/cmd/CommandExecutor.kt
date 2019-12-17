package com.factorymarket.rxelm.cmd

import com.factorymarket.rxelm.program.MessageConsumer

interface CommandExecutor {

    fun executeCmd(cmd: Cmd)

    fun stop()

    fun addMessageConsumer(mc : MessageConsumer)

}