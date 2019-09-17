package com.factorymarket.rxelm.effect.rx

import com.factorymarket.rxelm.cmd.*
import com.factorymarket.rxelm.contract.RxComponent
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.MessageConsumer
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.collections.HashMap

class RxCommandExecutor<S : State>(
        private val component: RxComponent<S>,
        private val logTag : String,
        private val handleCmdErrors: Boolean,
        private val outputScheduler: Scheduler,
        private val logger: RxElmLogger?) : CommandExecutor<S> {

    /**
     * Since we can cancel commands by their class, we hold commands in map bags by the hashcode
     * of the class.
     */
    private val commandsDisposablesMap: MutableMap<Int, MutableMap<Int, Disposable>> = TreeMap()
    private val switchRelayHolder: HashMap<String, Relay<SwitchCmd>> = HashMap()
    private var disposables: CompositeDisposable = CompositeDisposable()
    lateinit var messageConsumer : MessageConsumer

    override fun executeCmd(cmd: Cmd) {
        when (cmd) {
            is SwitchCmd -> {
                val relay = getSwitchRelay(cmd)
                relay.accept(cmd)
            }
            is CancelCmd -> {
                val commandDisposablesMap = commandsDisposablesMap[cmd.cancelCmd::class.hashCode()]
                        ?: return

                val commandDisposables = commandDisposablesMap[cmd.cancelCmd.hashCode()]
                if (commandDisposables != null && !commandDisposables.isDisposed) {
                    logCmd("elm cancel cmd:${cmd.cancelCmd}")
                    commandDisposables.dispose()
                }
            }
            is CancelByClassCmd<*> -> {
                val commandDisposablesMap = commandsDisposablesMap[cmd.cmdClass.hashCode()]
                        ?: return
                commandDisposablesMap.values.forEach { disposable ->
                    if (!disposable.isDisposed) {
                        disposable.dispose()
                    }
                }
            }
            else -> handleCmd(cmd)
        }
    }

    private fun handleResponse(observable: Observable<Msg>): Disposable {
        return observable
                .observeOn(outputScheduler)
                .subscribe { msg ->
                    messageConsumer.accept(msg)
                }
    }


    private fun handleCmd(cmd: Cmd) {
        logCmd("elm call cmd:$cmd")

        val cmdObservable = cmdCall(cmd).subscribeOn(Schedulers.io())
        val disposable = handleResponse(cmdObservable)
        val cmdDisposablesMap = commandsDisposablesMap[cmd::class.hashCode()]
        if (cmdDisposablesMap != null) {
            val oldDisposable = cmdDisposablesMap[cmd.hashCode()]
            if (oldDisposable != null && !oldDisposable.isDisposed) {
                disposables.add(oldDisposable)
            }
            cmdDisposablesMap[cmd.hashCode()] = disposable
        } else {
            val disposablesMap = TreeMap<Int, Disposable>()
            disposablesMap[cmd.hashCode()] = disposable
            commandsDisposablesMap[cmd::class.hashCode()] = disposablesMap
        }
    }

    private fun getSwitchRelay(cmd: SwitchCmd): Relay<SwitchCmd> {
        val cmdName = cmd.javaClass::getSimpleName.toString()
        var relay: Relay<SwitchCmd>? = switchRelayHolder[cmdName]
        if (relay == null) {
            relay = BehaviorRelay.create()
            switchRelayHolder[cmdName] = relay
            subscribeSwitchRelay(relay)
        }
        return relay
    }

    private fun logCmd(message: String) {
        logger?.takeIf { it.logType().needToShowCommands() }?.log(logTag, message)
    }

    private fun subscribeSwitchRelay(relay: BehaviorRelay<SwitchCmd>) {
        val switchDisposable = handleResponse(relay.switchMap { cmd ->
            logCmd("elm call cmd: $cmd")

            cmdCall(cmd).subscribeOn(Schedulers.io()).doOnDispose {
                logCmd("elm dispose cmd:$cmd")
            }
        })

        disposables.add(switchDisposable)
    }

    private fun cmdCall(cmd: Cmd): Observable<Msg> {

        return if (handleCmdErrors) {
            component.call(cmd).toSingle()
                    .onErrorResumeNext { err ->
                        logger?.takeIf { it.logType().needToShowCommands() }?.error(logTag, err)
                        Single.just(ErrorMsg(err, cmd))
                    }
                    .toObservable()
        } else {
            component.call(cmd).toSingle()
                    .toObservable()
        }
    }

    override fun stop() {
        commandsDisposablesMap.values.forEach {
            it.values.forEach { disposable ->
                if (!disposable.isDisposed) {
                    disposable.dispose()
                }
            }
        }
    }

    override fun addMessageConsumer(mc: MessageConsumer) {
        messageConsumer = mc
    }

}