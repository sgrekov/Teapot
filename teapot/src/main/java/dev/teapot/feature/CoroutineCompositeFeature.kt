package dev.teapot.feature

import dev.teapot.msg.Idle
import dev.teapot.msg.Msg
import dev.teapot.program.Program
import dev.teapot.program.ProgramBuilder
import dev.teapot.cmd.Cmd
import dev.teapot.contract.CoroutinesEffectHandler
import dev.teapot.contract.Renderable
import dev.teapot.contract.State

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