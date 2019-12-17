package com.factorymarket.rxelm.feature

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.Program
import com.factorymarket.rxelm.program.ProgramBuilder
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