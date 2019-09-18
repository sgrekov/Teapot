package com.factorymarket.rxelm.contract

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.msg.Msg

interface CoroutineFeature<S : State> : Update1<S>, CoroutinesEffectHandler

interface CoroutinesEffectHandler {
    suspend fun call(cmd: Cmd): Msg
}