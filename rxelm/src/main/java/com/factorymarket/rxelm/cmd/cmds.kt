package com.factorymarket.rxelm.cmd

sealed class AbstractCmd
open class Cmd : AbstractCmd()
open class SwitchCmd : Cmd()
data class CancelCmd(val cancelCmd: Cmd) : Cmd()
object None : Cmd()

data class BatchCmd(val cmds: MutableSet<Cmd>) : Cmd() {
    constructor(vararg commands: Cmd) : this(commands.toMutableSet())

    fun merge(cmd: Cmd): BatchCmd {
        when (cmd) {
            is BatchCmd -> {
                cmds.addAll(cmd.cmds)
            }
            is None -> {
            }
            else -> {
                cmds.add(cmd)
            }
        }
        return this
    }
}