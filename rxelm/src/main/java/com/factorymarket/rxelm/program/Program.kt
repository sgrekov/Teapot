package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.cmd.*
import com.factorymarket.rxelm.contract.*
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.log.LogType
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.sub.RxElmSubscriptions
import com.factorymarket.rxelm.sub.Sub
import java.util.ArrayDeque

/**
 * How to use these class:
 *
 *
 * All interactions happen in cycle:
 *
 * [Msg]
 * -> update(Message, State)[Component.update] : [Pair]<[State], [Cmd]>
 * -> (Optional)render(State)[Component.render]
 * -> call(Command)[Component.call]
 * -> [Msg].
 *
 *
 * Messages are being passed to [Program] using [accept(Message)][accept] method.
 *
 * Function [render()][RenderableComponent.render] renders view in declarative style according to [State].
 * No other changes of View can happen outside of this function
 *
 * All changes of state must be made only in function [Update][Component.update], which is a pure function.
 * There cannot happen any calls to side effect, like IO work, HTTP requests, etc
 * All user interactions are processed through inheritances of Msg class.
 * Function [Update][Component.update] returns new State with changed fields and [Command][Cmd].
 *
 * Class [Cmd] represents desired Side Effect. If you want do some side effect,
 * you return a [Command][Cmd] from [Update()][Component.update] method
 * and in function [Call][Component.call] do the side effect itself.
 * Results wrapped in resulting [Msg] go to [Update][Component.update] method.
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
        private val component: Component<S, out Effect>,
        private val logger: RxElmLogger?) : MessageConsumer {

    /** Here messages are kept until they can be passed to messageRelay */
    private var messageQueue = ArrayDeque<Msg>()

    /** State at this moment */
    private lateinit var state: S

    private lateinit var commandExecutor: CommandExecutor<S>

    private var lock: Boolean = false
    private var isRendering: Boolean = false
    private var sub: Sub<S>? = null

    fun run(initialState: S, sub: Sub<S>? = null, initialMsg: Msg = Init) {
        init(initialState, sub)

        accept(initialMsg)
    }

    fun run(initialState: S, sub: Sub<S>? = null, initialMsgs: List<Msg>) {
        init(initialState, sub)

        initialMsgs.forEach {
            accept(it)
        }
    }

    private fun init(initialState: S, sub: Sub<S>?) {
        this.state = initialState
        this.sub = sub
    }

    fun runCycle() {
        if (messageQueue.isEmpty()) {
            return
        }

        val msg = messageQueue.first
        val update = update(msg, component, logger)
        val command = update.cmds
        val newState = update.updatedState ?: state

        if (newState !== this.state) {
            isRendering = true
            if (component is Renderable<*>) {
                (component as Renderable<S>).render(newState)
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

    fun isRendering(): Boolean = isRendering

    private fun update(msg: Msg, component: Component<S, out Effect>, logger: RxElmLogger?): Update<S> {
        logUpdate(logger, msg)

        val updateResult = component.update(msg, this.state)

        if (messageQueue.size > 0) {
            messageQueue.removeFirst()
        }

        return updateResult
    }

    private fun logUpdate(logger: RxElmLogger?, msg: Msg) {
        logger?.takeIf { it.logType().needToShowUpdates() }
                ?.log(this.state.javaClass.simpleName, "update with msg:${msg.javaClass.simpleName} ")
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
        if (component is Renderable<*>) {
            (component as Renderable<S>).render(this.state)
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

    private fun logAccept(logger: RxElmLogger?, msg: Msg) {
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

    fun addCommandExecutor(ce: CommandExecutor<S>) {
        ce.addMessageConsumer(this)
        commandExecutor = ce
    }

}