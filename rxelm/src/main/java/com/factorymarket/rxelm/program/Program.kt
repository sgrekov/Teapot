package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.cmd.BatchCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.cmd.SwitchCmd
import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.contract.Renderable
import com.factorymarket.rxelm.contract.RenderableComponent
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.contract.Update
import com.factorymarket.rxelm.log.LogType
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.Init
import com.factorymarket.rxelm.sub.RxElmSubscriptions
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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
        val outputScheduler: Scheduler,
        private val logger: RxElmLogger?,
        handleCmdErrors: Boolean,
        private val component: Component<S>
) : MessageConsumer {

    private val messageRelay: BehaviorRelay<Msg> = BehaviorRelay.create()
    private val commandExecutor = RxCommandExecutor(component, handleCmdErrors, outputScheduler, logger)

    /** Here messages are kept until they can be passed to messageRelay */
    private var messageQueue = ArrayDeque<Msg>()

    /** State at this moment */
    private lateinit var state: S

    private var lock: Boolean = false
    private var isRendering: Boolean = false
    private var rxElmSubscriptions: RxElmSubscriptions<S>? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    fun run(initialState: S, rxElmSubscriptions: RxElmSubscriptions<S>? = null, initialMsg: Msg = Init) {
        init(initialState, rxElmSubscriptions)

        accept(initialMsg)
    }

    fun run(initialState: S, rxElmSubscriptions: RxElmSubscriptions<S>? = null, initialMsgs: List<Msg>) {
        init(initialState, rxElmSubscriptions)

        initialMsgs.forEach {
            accept(it)
        }
    }

    private fun init(initialState: S, rxElmSubscriptions: RxElmSubscriptions<S>?) {
        this.state = initialState
        this.rxElmSubscriptions = rxElmSubscriptions

        val loopDisposable = createLoop(component, logger)

        disposables.add(loopDisposable)
    }

    fun createLoop(component: Component<S>, logger: RxElmLogger?): Disposable {
        return messageRelay
                .observeOn(outputScheduler)
                .map { msg ->

                    val update = update(msg, component, logger)
                    val command = update.cmds
                    val newState = update.updatedState ?: state

                    if (newState !== this.state) {
                        isRendering = true
                        if (component is Renderable<*>) {
                            (component as Renderable<S>).render(newState)
                        } else if (component is RenderableComponent) {
                            component.render(newState)
                        }
                        isRendering = false
                    }

                    this.state = newState
                    lock = false

                    this.rxElmSubscriptions?.subscribe(this, newState)

                    pickNextMessageFromQueue()

                    return@map command
                }
                .filter { cmd -> cmd !== None }
                .subscribe { cmd ->
                    if (cmd is BatchCmd) {
                        cmd.cmds.filter { it !is None }.forEach { innerCmd ->
                            commandExecutor.executeCmd(innerCmd)
                        }
                    } else {
                        commandExecutor.executeCmd(cmd)
                    }

                }
    }

    fun isRendering(): Boolean = isRendering

    private fun update(msg: Msg, component: Component<S>, logger: RxElmLogger?): Update<S> {
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
            messageRelay.accept(messageQueue.first)
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
        if (component is RenderableComponent) {
            component.render(this.state)
        }
    }

    fun accept(msg: Msg) {
        logAccept(logger, msg)

        messageQueue.addLast(msg)
        if (!lock && messageQueue.size == 1) {
            lock = true
            messageRelay.accept(messageQueue.first)
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

    fun addEventObservable(eventSource: Observable<Msg>): Disposable {
        return eventSource.subscribe { msg -> accept(msg) }
    }

    fun stop() {
        if (!disposables.isDisposed) {
            disposables.dispose()
        }
        commandExecutor.stop()
        rxElmSubscriptions?.dispose()
    }

}