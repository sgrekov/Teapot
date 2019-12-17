package com.factorymarket.rxelm.feature

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.CoroutinesEffectHandler
import com.factorymarket.rxelm.contract.Renderable
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder

class CoroutineCompositeFeature<S : State>(val programBuilder: ProgramBuilder,
                                    renderer: Renderable<S>)
    : CompositeFeature<S>(renderer), CoroutinesEffectHandler {

    override fun buildProgram(): Program<S> {
        return programBuilder.build(this, this)
    }

    override suspend fun call(cmd: Cmd): Msg {
        components.forEach { (component, _) ->
            if (component.handlesCommands(cmd) && component is CoroutinesEffectHandler) {
                return component.call(cmd)
            }
        }
        return Idle
    }

}