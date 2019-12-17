package com.factorymarket.rxelm

import com.factorymarket.rxelm.cmd.BatchCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Msg
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