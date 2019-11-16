package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.effect.coroutine.CoroutinesCommandExecutor
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.middleware.Middleware
import io.reactivex.Scheduler
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.effect.rx.RxCommandExecutor
import com.factorymarket.rxelm.sub.FlowSub
import com.factorymarket.rxelm.sub.RxSub
import kotlinx.coroutines.CoroutineDispatcher

class ProgramBuilder {

    private var outputScheduler: Scheduler? = null
    private var outputDispatcher: CoroutineDispatcher? = null
    private var logger: RxElmLogger? = null
    private var handleCmdErrors: Boolean = true
    private var middlewares: MutableList<Middleware> = mutableListOf()

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

    fun addMiddleware(m : Middleware) {
        middlewares.add(m)
    }

    /**
     * By default handleCmdErrors is set to true and RxElm handles errors from side effect and sends them in [ErrorMsg]
     * If pass handle=false, then all unhandled errors from [Feature.call] will lead to crash
     */
    fun handleCmdErrors(handle: Boolean): ProgramBuilder {
        this.handleCmdErrors = handle
        return this
    }

    fun <S : State> buildRxSub() : RxSub<S> {
        val scheduler = outputScheduler ?: throw IllegalArgumentException("Output Scheduler must be provided!")
        return RxSub(scheduler)
    }

    fun <S : State> buildFLowSub() : FlowSub<S> {
        val dispatcher = outputDispatcher ?: throw IllegalArgumentException("Output Dispatcher must be provided!")
        return FlowSub(dispatcher)
    }

    fun <S : State> build(feature: RxFeature<S>): Program<S> {
        return build(feature, feature)
    }

    fun <S : State> build(update: Upd<S>, effectHandler: RxEffectHandler): Program<S> {
        val scheduler = outputScheduler ?: throw IllegalArgumentException("Output Scheduler must be provided!")
        val commandExecutor = RxCommandExecutor(effectHandler, "", handleCmdErrors, scheduler, logger)
        val program = Program(update, logger, middlewares)
        program.addCommandExecutor(commandExecutor)
        return program
    }

    fun <S : State> build(feature: CoroutineFeature<S>): Program<S> {
        return build(feature, feature)
    }

    fun <S : State> build(upd: Upd<S>, effectHandler: CoroutinesEffectHandler): Program<S> {
        val dispatcher = outputDispatcher ?: throw IllegalArgumentException("Output Dispatcher must be provided!")
        val commandExecutor = CoroutinesCommandExecutor(effectHandler, dispatcher, "", handleCmdErrors, logger)
        val program = Program(upd, logger, middlewares)
        program.addCommandExecutor(commandExecutor)
        return program
    }
}