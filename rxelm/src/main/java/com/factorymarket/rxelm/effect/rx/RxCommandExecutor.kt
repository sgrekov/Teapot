package com.factorymarket.rxelm.effect.rx

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.RxEffectHandler
import com.factorymarket.rxelm.effect.BaseCommandExecutor
import com.factorymarket.rxelm.effect.RunningEffect
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Msg
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class RxCommandExecutor(
        private val effectHandler: RxEffectHandler,
        logTag: String,
        private val handleCmdErrors: Boolean,
        private val outputScheduler: Scheduler,
        logger: RxElmLogger?) : BaseCommandExecutor<RxRunningEffect>(logTag, logger) {

    /**
     * Since we can cancel commands by their class, we hold commands in map bags by the hashcode
     * of the class.
     */
    private var disposables: CompositeDisposable = CompositeDisposable()

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

    private fun cmdCall(cmd: Cmd): Observable<Msg> {
        return if (handleCmdErrors) {
            effectHandler.call(cmd)
                    .onErrorResumeNext { err ->
                        logger?.takeIf { it.logType().needToShowCommands() }?.error(logTag, err)
                        Single.just(ErrorMsg(err, cmd))
                    }
                    .toObservable()
        } else {
            effectHandler.call(cmd)
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