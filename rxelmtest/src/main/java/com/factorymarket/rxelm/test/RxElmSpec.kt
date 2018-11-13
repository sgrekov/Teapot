package com.factorymarket.rxelm.test

import com.factorymarket.rxelm.cmd.BatchCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.msg.Msg
import org.junit.Assert
import org.junit.Assert.assertEquals


class RxElmSpec<S : State> constructor(val component: Component<S>) {

    private lateinit var state: S
    private var prevState: S? = null
    private lateinit var cmd: Cmd

    fun withState(state: S): RxElmSpec<S> {
        this.state = state
        return this
    }

    fun withState(state: S, oldState: S): RxElmSpec<S> {
        this.state = state
        this.prevState = oldState
        return this
    }

    fun withCmd(c: Cmd): RxElmSpec<S> {
        this.cmd = c
        return this
    }

    fun state(): S {
        return state
    }

    fun copy(): RxElmSpec<S> {
        return RxElmSpec(component).withCmd(this.cmd).withState(this.state)
    }

    fun whenMsg(msg: Msg): RxElmSpec<S> {
        val (newState, cmd) = component.update(msg, state)
        return this.withState(newState, state).withCmd(cmd)
    }

    fun thenCmd(assertionCmd: Cmd): RxElmSpec<S> {
        assertEquals(assertionCmd, cmd)
        return this
    }

    fun andCmd(assertionCmd: Cmd): RxElmSpec<S> {
        return thenCmd(assertionCmd)
    }

    fun thenCmdBatch(vararg cmds: Cmd): RxElmSpec<S> {
        Assert.assertEquals((this.cmd as BatchCmd).cmds.size, cmds.size)
        Assert.assertEquals(this.cmd, BatchCmd(cmds = cmds.toMutableSet()))
        return this
    }

    fun thenCmdBatchContains(vararg cmds: Cmd): RxElmSpec<S> {
        cmds.forEach {
            Assert.assertTrue((this.cmd as BatchCmd).cmds.contains(it))
        }
        return this
    }

    fun assertCmds(assert: (cmds: BatchCmd) -> Unit): RxElmSpec<S> {
        assert.invoke(this.cmd as BatchCmd)
        return this
    }

    fun assertCmd(assert: (cmds: Cmd) -> Unit): RxElmSpec<S> {
        assert.invoke(this.cmd)
        return this
    }

    fun andCmdBatch(vararg cmds: Cmd): RxElmSpec<S> {
        return thenCmdBatch(*cmds)
    }

    /** Asserts that state is exactly like [state] */
    fun andHasExactState(state: S): RxElmSpec<S> {
        Assert.assertEquals(state, this.state)
        return this
    }

    fun assertState(transform: (s: S) -> S): RxElmSpec<S> {
        Assert.assertEquals(transform(state), state)
        return this.withState(state)
    }

    fun diffState(transform: (prevState: S) -> S): RxElmSpec<S> {
        Assert.assertEquals(transform(prevState!!), state)
        return this.withState(state)
    }

    /**
     * alias to assertState
     */
    fun andState(transform: (s: S) -> S): RxElmSpec<S> {
        return assertState(transform)
    }

    /**
     * alias to assertState
     */
    fun thenState(transform: (s: S) -> S): RxElmSpec<S> {
        return assertState(transform)
    }

    fun checkState(assertion: (s: S) -> Unit): RxElmSpec<S> {
        assertion(this.state)
        return this
    }
}
