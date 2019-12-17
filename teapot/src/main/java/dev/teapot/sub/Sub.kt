package dev.teapot.sub

import dev.teapot.contract.State
import dev.teapot.program.MessageConsumer

interface Sub<S : State> {

    fun setMessageConsumer(mc : MessageConsumer)

    fun subscribe(state: S)

    fun dispose()
}