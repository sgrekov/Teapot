package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.log.RxElmLogger
import io.reactivex.Scheduler
import java.lang.IllegalArgumentException

class ProgramBuilder {

    private var outputScheduler: Scheduler? = null
    private var logger: RxElmLogger? = null
    private var handleCmdErrors: Boolean = true

    fun outputScheduler(scheduler: Scheduler): ProgramBuilder {
        this.outputScheduler = scheduler
        return this
    }

    fun logger(logger: RxElmLogger): ProgramBuilder {
        this.logger = logger
        return this
    }

    fun handleCmdErrors(handle : Boolean) : ProgramBuilder{
        this.handleCmdErrors = handle
        return this
    }

    fun <S : State> build(): Program<S> {
        if (outputScheduler == null) {
            throw IllegalArgumentException("Output Scheduler must be provided!")
        }
        return Program(outputScheduler!!, logger, handleCmdErrors)
    }
}