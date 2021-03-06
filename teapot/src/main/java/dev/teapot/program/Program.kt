package dev.teapot.program

import dev.teapot.cmd.BatchCmd
import dev.teapot.cmd.Cmd
import dev.teapot.cmd.CommandExecutor
import dev.teapot.cmd.None
import dev.teapot.contract.Renderable
import dev.teapot.contract.State
import dev.teapot.contract.Upd
import dev.teapot.contract.Update
import dev.teapot.log.LogType
import dev.teapot.log.TeapotLogger
import dev.teapot.middleware.Middleware
import dev.teapot.msg.*
import dev.teapot.sub.Sub
import java.util.ArrayDeque

/**
 * How to use these class:
 *
 *
 * All interactions happen in cycle:
 *
 * -> update(Message, State)[Program.update] : [Update]<[State], [Cmd]>
 * -> (Optional)render(State)[Program.render]
 * -> call(Command)[CommandExecutor.executeCmd]
 *
 *
 * Messages are being passed to [Program] using [accept(Message)][accept] method.
 *
 * Function [render()][RenderableComponent.render] renders view in declarative style according to [State].
 * No other changes of View can happen outside of this function
 *
 * All changes of state must be made only in function [Update][Feature.update], which is a pure function.
 * There cannot happen any calls to side effect, like IO work, HTTP requests, etc
 * All user interactions are processed through inheritances of Msg class.
 * Function [Update][Feature.update] returns new State with changed fields and [Command][Cmd].
 *
 * Class [Cmd] represents desired Side Effect. If you want do some side effect,
 * you return a [Command][Cmd] from [Update()][Feature.update] method
 * and in function [Call][Feature.call] do the side effect itself.
 * Results wrapped in resulting [Msg] go to [Update][Feature.update] method.
 *
 * Program executes [Commands][Cmd] in [flatMap][Observable.flatMap], that means
 * they will be executed in parallel in [io() scheduler][Schedulers.io].
 * If you want to cancel current command when queueing new one,
 * you must send [Command][Cmd] which inherits [SwitchCmd], this will
 * do all side effect in rx [switchMap][Observable.switchMap] operator
 *
 * @param outputScheduler the scheduler to [observe on][Observable.observeOn]
 */
class Program<S : State> internal constructor(
        private val feature: Upd<S>,
        private val logger: TeapotLogger?,
        private val middlewares: List<Middleware>) : MessageConsumer {

    /** Here messages are kept until they can be passed to messageRelay */
    private var messageQueue = ArrayDeque<Msg>()

    /** State at this moment */
    private lateinit var state: S

    private lateinit var commandExecutor: CommandExecutor

    private var isStarted : Boolean = false
    private var lock: Boolean = false
    private var isRendering: Boolean = false
    private var sub: Sub<S>? = null

    fun run(initialState: S, sub: Sub<S>? = null, initialMsg: Msg = Init) {
        if (isStarted) return

        init(initialState, sub)

        accept(initialMsg)
    }

    fun run(initialState: S, sub: Sub<S>? = null, initialMsgs: List<Msg>) {
        if (isStarted) return

        init(initialState, sub)

        initialMsgs.forEach {
            accept(it)
        }
    }

    private fun init(initialState: S, sub: Sub<S>?) {
        this.state = initialState
        this.sub = sub
        this.sub?.setMessageConsumer(this)
        this.isStarted = true
    }

    fun runCycle() {
        if (messageQueue.isEmpty()) {
            return
        }

        val msg = messageQueue.first

        processBeforeUpdateToMiddlewares(msg, this.state)

        val update = update(msg, feature, logger)
        val command = update.cmds
        val newState = update.updatedState ?: state

        processAfterUpdateMiddlewares(msg, newState)

        if (newState !== this.state) {
            isRendering = true
            if (feature is Renderable<*>) {
                (feature as Renderable<S>).render(newState)
            }
            isRendering = false
        }

        state = newState
        lock = false

        sub?.subscribe(newState)

        if (command !== None) {
            if (command is BatchCmd) {
                command.cmds.forEach { innerCmd ->
                    if (innerCmd !is None) {
                        commandExecutor.executeCmd(innerCmd)
                    }
                }
            } else {
                commandExecutor.executeCmd(command)
            }
        }

        pickNextMessageFromQueue()
    }

    private fun processAfterUpdateMiddlewares(msg: Msg, newState: S) {
        middlewares.forEach { middleware ->
            middleware.afterUpdate(msg, newState)
        }
    }

    private fun processBeforeUpdateToMiddlewares(msg: Msg, oldState: S) {
        middlewares.forEach { middleware ->
            middleware.beforeUpdate(msg, oldState)
        }
    }

    fun isRendering(): Boolean = isRendering

    private fun update(msg: Msg, feature: Upd<S>, logger: TeapotLogger?): Update<S> {
        logUpdate(logger, msg)

        val updateResult = if (msg is ProxyMsg) {
            Update.effect(msg.cmd)
        } else feature.update(msg, this.state)

        if (messageQueue.size > 0) {
            messageQueue.removeFirst()
        }

        return updateResult
    }

    private fun logUpdate(logger: TeapotLogger?, msg: Msg) {
        logger?.takeIf { it.logType().needToShowUpdates() }
                ?.log(this.state.javaClass.simpleName, "update with msg:${msg.javaClass.simpleName} ")
        if (msg is ErrorMsg){
            logger?.error(this.state.javaClass.simpleName, msg.err)
        }
    }

    private fun pickNextMessageFromQueue() {
        logPickNextMessageFromQueue()

        if (!lock && messageQueue.size > 0) {
            lock = true
            runCycle()
        }
    }

    private fun logPickNextMessageFromQueue() {
        logger?.takeIf { logger.logType() == LogType.All }?.let {
            logger.log(
                    this.state.javaClass.simpleName,
                    "pickNextMessageFromQueue, queue size:${messageQueue.size}"
            )
        }
    }

    fun render() {
        if (feature is Renderable<*>) {
            (feature as Renderable<S>).render(this.state)
        }
    }

    override fun accept(msg: Msg) {
        logAccept(logger, msg)

        if (msg !is Idle) {
            messageQueue.addLast(msg)
        }
        if (!lock && messageQueue.size == 1) {
            lock = true
            runCycle()
        }
    }

    fun acceptCommand(cmd : Cmd){
        accept(ProxyMsg(cmd))
    }

    private fun logAccept(logger: TeapotLogger?, msg: Msg) {
        logger?.takeIf { logger.logType() == LogType.All }?.log(
                this.state.javaClass.simpleName,
                "accept msg: ${msg.javaClass.simpleName}, queue size:${messageQueue.size} lock:$lock "
        )
    }

    fun getState(): S? {
        return if (this::state.isInitialized) {
            state
        } else null
    }

    fun stop() {
        commandExecutor.stop()
        sub?.dispose()
    }

    fun addCommandExecutor(ce: CommandExecutor) {
        ce.addMessageConsumer(this)
        commandExecutor = ce
    }

}