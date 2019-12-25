package dev.teapot.program

import io.reactivex.Scheduler
import java.lang.IllegalArgumentException
import dev.teapot.contract.*
import dev.teapot.effect.coroutine.CoroutinesCommandExecutor
import dev.teapot.effect.rx.RxCommandExecutor
import dev.teapot.log.TeapotLogger
import dev.teapot.middleware.Middleware
import dev.teapot.sub.FlowSub
import dev.teapot.sub.RxSub
import kotlinx.coroutines.CoroutineDispatcher

class ProgramBuilder {

    private var outputScheduler: Scheduler? = null
    private var outputDispatcher: CoroutineDispatcher? = null
    var logger: TeapotLogger? = null
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

    fun logger(logger: TeapotLogger): ProgramBuilder {
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