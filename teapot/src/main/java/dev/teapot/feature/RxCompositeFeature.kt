package dev.teapot.feature

import dev.teapot.cmd.Cmd
import dev.teapot.contract.Renderable
import dev.teapot.contract.RxEffectHandler
import dev.teapot.contract.State
import dev.teapot.msg.Idle
import dev.teapot.msg.Msg
import dev.teapot.program.Program
import dev.teapot.program.ProgramBuilder
import io.reactivex.Single

class RxCompositeFeature<S : State>(val programBuilder: ProgramBuilder,
                                    renderer: Renderable<S>)
    : CompositeFeature<S>(renderer), RxEffectHandler {

    override fun buildProgram(): Program<S> {
        return programBuilder.build(this, this)
    }

    override fun call(cmd: Cmd): Single<Msg> {
        components.forEach { (component, _) ->
            if (component.handlesCommands(cmd) && component is RxEffectHandler) {
                return component.call(cmd)
            }
        }
        return Single.just(Idle)
    }

}