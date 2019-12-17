package dev.teapot

import dev.teapot.cmd.BatchCmd
import dev.teapot.cmd.Cmd
import dev.teapot.msg.Idle
import dev.teapot.msg.Msg
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers


inline fun statelessEffect(crossinline operations: () -> Unit): Single<Msg> {
    return Single.fromCallable {
        operations()
    }.subscribeOn(Schedulers.io()).map { Idle }
}

fun cmds(vararg cmds: Cmd): BatchCmd {
    return BatchCmd(cmds = cmds.toMutableSet())
}