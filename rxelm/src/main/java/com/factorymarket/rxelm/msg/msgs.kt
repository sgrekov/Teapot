package com.factorymarket.rxelm.msg

import com.factorymarket.rxelm.cmd.Cmd

sealed class AbstractMsg
/**
 * Base class for representing common messages (events).
 * Messages could be like `user pressed button`, `network data received` etc
 */
open class Msg : AbstractMsg()
object Idle : Msg()
object Init : Msg()
class ErrorMsg(val err: Throwable, val cmd: Cmd) : Msg()
