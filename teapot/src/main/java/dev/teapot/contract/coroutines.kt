package dev.teapot.contract

import dev.teapot.cmd.Cmd
import dev.teapot.msg.Msg

interface CoroutineFeature<S : State> : Upd<S>, CoroutinesEffectHandler

interface CoroutinesEffectHandler {
    suspend fun call(cmd: Cmd): Msg
}