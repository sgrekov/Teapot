package dev.teapot.middleware

import dev.teapot.contract.State
import dev.teapot.msg.Msg

interface Middleware {

    fun beforeUpdate(msg: Msg, oldState: State)

    fun afterUpdate(msg: Msg, newState: State)

}