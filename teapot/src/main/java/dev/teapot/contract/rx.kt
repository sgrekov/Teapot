package dev.teapot.contract

import dev.teapot.cmd.Cmd
import dev.teapot.msg.Msg
import io.reactivex.Single

interface RxFeature<S : State> : Upd<S>, RxEffectHandler

interface RxEffectHandler  {
    fun call(cmd: Cmd): Single<Msg>
}