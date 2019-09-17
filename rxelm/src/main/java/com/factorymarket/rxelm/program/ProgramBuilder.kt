package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.contract.CoroutineComponent
import com.factorymarket.rxelm.contract.RxComponent
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.effect.coroutine.CoroutinesCommandExecutor
import com.factorymarket.rxelm.log.RxElmLogger
import io.reactivex.Scheduler
import java.lang.IllegalArgumentException
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.effect.rx.RxCommandExecutor
import kotlinx.coroutines.Dispatchers

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

    fun <S : State> build(component: RxComponent<S>): Program<S> {
        if (outputScheduler == null) {
            throw IllegalArgumentException("Output Scheduler must be provided!")
        }
        val commandExecutor = RxCommandExecutor(component, "", handleCmdErrors, outputScheduler!!, logger)
        val program = Program(component, logger)
        program.addCommandExecutor(commandExecutor)
        return program
    }

    fun <S : State> build(component: CoroutineComponent<S>): Program<S> {
        if (outputScheduler == null) {
            throw IllegalArgumentException("Output Scheduler must be provided!")
        }
        val commandExecutor = CoroutinesCommandExecutor(component, Dispatchers.Main, "", handleCmdErrors, logger)
        val program = Program(component, logger)
        program.addCommandExecutor(commandExecutor)
        return program
    }
}