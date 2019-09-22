package com.factorymarket.rxelm.effect.rx

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.SwitchCmd
import com.factorymarket.rxelm.contract.RxEffectHandler
import com.factorymarket.rxelm.effect.BaseCommandExecutor
import com.factorymarket.rxelm.effect.RunningEffect
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Msg
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class RxCommandExecutor(
        private val component: RxEffectHandler,
        logTag: String,
        private val handleCmdErrors: Boolean,
        private val outputScheduler: Scheduler,
        logger: RxElmLogger?) : BaseCommandExecutor<RxRunningEffect>(logTag, logger) {

    /**
     * Since we can cancel commands by their class, we hold commands in map bags by the hashcode
     * of the class.
     */
    private val switchRelayHolder: HashMap<String, Relay<SwitchCmd>> = HashMap()
    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun handleSwitchCmd(cmd: SwitchCmd) {
        val relay = getSwitchRelay(cmd)
        relay.accept(cmd)
    }

    private fun handleResponse(observable: Observable<Msg>): Disposable {
        return observable
                .observeOn(outputScheduler)
                .subscribe { msg ->
                    messageConsumer.accept(msg)
                }
    }

    override fun handleCmd(cmd: Cmd) {
        logCmd("elm call cmd:$cmd")

        val cmdObservable = cmdCall(cmd).subscribeOn(Schedulers.io())
        val disposable = handleResponse(cmdObservable)
        saveRunningEffect(cmd, RxRunningEffect(disposable))
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

    private fun subscribeSwitchRelay(relay: BehaviorRelay<SwitchCmd>) {
        val switchDisposable = handleResponse(
                relay.switchMap { cmd ->
                    logCmd("elm call cmd: $cmd")

                    cmdCall(cmd)
                            .subscribeOn(Schedulers.io())
                            .doOnDispose {
                                logCmd("elm dispose cmd:$cmd")
                            }
                })

        disposables.add(switchDisposable)
    }

    private fun cmdCall(cmd: Cmd): Observable<Msg> {
        return if (handleCmdErrors) {
            component.call(cmd)
                    .onErrorResumeNext { err ->
                        logger?.takeIf { it.logType().needToShowCommands() }?.error(logTag, err)
                        Single.just(ErrorMsg(err, cmd))
                    }
                    .toObservable()
        } else {
            component.call(cmd)
                    .toObservable()
        }
    }

    override fun saveOldEffect(oldDisposable: RxRunningEffect) {
        disposables.add(oldDisposable.disposable)
    }

    override fun stop() {
        super.stop()
        disposables.clear()
    }

}

class RxRunningEffect(val disposable: Disposable) : RunningEffect() {

    override fun cancel() {
        disposable.dispose()
    }

    override fun isRunning(): Boolean = !disposable.isDisposed

}