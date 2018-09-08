package com.factorymarket.rxelm.cmd

sealed class AbstractCmd
open class Cmd : AbstractCmd()
open class SwitchCmd : Cmd()
object None : Cmd()
data class BatchCmd(val cmds: MutableSet<Cmd>) : Cmd() {
    constructor(vararg commands: Cmd) : this(commands.toMutableSet())
}