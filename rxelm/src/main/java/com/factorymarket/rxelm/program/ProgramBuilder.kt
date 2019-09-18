package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.effect.coroutine.CoroutinesCommandExecutor
import com.factorymarket.rxelm.log.RxElmLogger
import io.reactivex.Scheduler
import java.lang.IllegalArgumentException
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.effect.rx.RxCommandExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ProgramBuilder {

    private var outputScheduler: Scheduler? = null
    private var outputDispatcher: CoroutineDispatcher? = null
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

    /**
     * @param dispatcher must be single threaded, like Dispatchers.Main,
     * since Program's implementation is not thread safe
     */
    fun outputDispatcher(d: CoroutineDispatcher): ProgramBuilder {
        this.outputDispatcher = d
        return this
    }

    fun logger(logger: RxElmLogger): ProgramBuilder {
        this.logger = logger
        return this
    }

    /**
     * By default handleCmdErrors is set to true and RxElm handles errors from side effect and sends them in [ErrorMsg]
     * If pass handle=false, then all unhandled errors from [Feature.call] will lead to crash
     */
    fun handleCmdErrors(handle: Boolean): ProgramBuilder {
        this.handleCmdErrors = handle
        return this
    }

    fun <S : State> build(feature: RxFeature<S>): Program<S> {
        return build(feature, feature)
    }

    fun <S : State> build(update: Update1<S>, effectHandler: RxEffectHandler): Program<S> {
        if (outputScheduler == null) {
            throw IllegalArgumentException("Output Scheduler must be provided!")
        }
        val commandExecutor = RxCommandExecutor(effectHandler, "", handleCmdErrors, outputScheduler!!, logger)
        val program = Program(update, logger)
        program.addCommandExecutor(commandExecutor)
        return program
    }

    fun <S : State> build(feature: CoroutineFeature<S>): Program<S> {
        return build(feature, feature)
    }

    fun <S : State> build(update1: Update1<S>, effectHandler: CoroutinesEffectHandler): Program<S> {
        val dispatcher = outputDispatcher ?: throw IllegalArgumentException("Output Dispatcher must be provided!")
        val commandExecutor = CoroutinesCommandExecutor(effectHandler, dispatcher, "", handleCmdErrors, logger)
        val program = Program(update1, logger)
        program.addCommandExecutor(commandExecutor)
        return program
    }
}