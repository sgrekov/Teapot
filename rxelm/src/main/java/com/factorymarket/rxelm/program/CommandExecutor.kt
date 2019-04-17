package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.cmd.CancelByClassCmd
import com.factorymarket.rxelm.cmd.CancelCmd
import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.SwitchCmd
import com.factorymarket.rxelm.contract.Component
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Msg
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class RxCommandExecutor<S : State>(
        private val component: Component<S>,
        private val handleCmdErrors: Boolean,
        private val outputScheduler: Scheduler,
        private val logger: RxElmLogger?) {

    private val commandsDisposablesMap: MutableMap<Int, MutableMap<Int, Disposable>> = TreeMap()
    private val switchRelayHolder: HashMap<String, Relay<SwitchCmd>> = HashMap()
    private var disposables: CompositeDisposable = CompositeDisposable()

    fun executeCmd(cmd: Cmd) {
        when (cmd) {
            is SwitchCmd -> {
                val relay = getSwitchRelay(cmd)
                relay.accept(cmd)
            }
            is CancelCmd -> {
                val commandDisposablesMap = commandsDisposablesMap[cmd.cancelCmd.hashCode()] ?: return

                val commandDisposables = commandDisposablesMap[cmd.cancelCmd.hashCode()]
                if (commandDisposables != null && !commandDisposables.isDisposed) {
                    logCmd("elm cancel cmd:${cmd.cancelCmd}")
                    commandDisposables.dispose()
                }
            }
            is CancelByClassCmd<*> -> {
                val commandDisposablesMap = commandsDisposablesMap[cmd.cmdClass.hashCode()] ?: return
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
                    if (msg !is Idle) {
                        messageQueue.addLast(msg)
                    }

                    pickNextMessageFromQueue()
                }
    }


    private fun handleCmd(cmd: Cmd) {
        logCmd("elm call cmd:$cmd")

        val cmdObservable = cmdCall(cmd).subscribeOn(Schedulers.io())
        val disposable = handleResponse(cmdObservable)
        val cmdDisposablesMap = commandsDisposablesMap[cmd::class.hashCode()]
        cmdDisposablesMap?.let {
            val oldDisposable = cmdDisposablesMap[cmd.hashCode()]
            if (oldDisposable != null && !oldDisposable.isDisposed) {
                disposables.add(oldDisposable)
            }
            cmdDisposablesMap[cmd.hashCode()] = disposable
        } ?: run {
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
        logger?.takeIf { it.logType().needToShowCommands() }
                ?.log(this.state.javaClass.simpleName, message)
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

    private fun cmdCall(cmd: Cmd): io.reactivex.Observable<Msg> {
        component
        return if (handleCmdErrors) {
            component.call(cmd)
                    .onErrorResumeNext { err ->
                        logger?.takeIf { it.logType().needToShowCommands() }?.error(this.state.javaClass.simpleName, err)
                        Single.just(ErrorMsg(err, cmd))
                    }
                    .toObservable()
        } else {
            component.call(cmd)
                    .toObservable()
        }
    }

    fun stop() {
        commandsDisposablesMap.values.forEach {
            it.values.forEach { disposable ->
                if (!disposable.isDisposed) {
                    disposable.dispose()
                }
            }
        }
    }

}
