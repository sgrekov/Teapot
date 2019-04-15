package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.log.RxElmLogger
import io.reactivex.Scheduler
import java.lang.IllegalArgumentException
import com.factorymarket.rxelm.msg.ErrorMsg

class ProgramBuilder {

    private var outputScheduler: Scheduler? = null
    private var logger: RxElmLogger? = null
    private var handleCmdErrors: Boolean = true

    /**
     * @param scheduler must be single threaded, like Schedulers.single() or AndroidSchedulers.mainThread(),
     * since Program's implementation is not thread safe
     */
    fun outputScheduler(scheduler: Scheduler): ProgramBuilder {
        this.outputScheduler = scheduler
        return this
    }

    fun logger(logger: RxElmLogger): ProgramBuilder {
        this.logger = logger
        return this
    }

    /**
     * By default handleCmdErrors is set to true and RxElm handles errors from side effect and sends them in [ErrorMsg]
     * If pass handle=false, then all unhandled errors from [Component.call] will lead to crash
     */
    fun handleCmdErrors(handle: Boolean): ProgramBuilder {
        this.handleCmdErrors = handle
        return this
    }

    fun <S : State> build(component: Component<S>): Program<S> {
        if (outputScheduler == null) {
            throw IllegalArgumentException("Output Scheduler must be provided!")
        }

        return Program(outputScheduler!!, logger, handleCmdErrors, component)
    }
}