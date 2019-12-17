package dev.teapot.cmd

import dev.teapot.program.MessageConsumer

interface CommandExecutor {

    fun executeCmd(cmd: Cmd)

    fun stop()

    fun addMessageConsumer(mc : MessageConsumer)

}