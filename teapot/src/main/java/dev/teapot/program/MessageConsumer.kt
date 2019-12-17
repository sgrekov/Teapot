package dev.teapot.program

import dev.teapot.msg.Msg

interface MessageConsumer {

    fun accept(msg : Msg)

}