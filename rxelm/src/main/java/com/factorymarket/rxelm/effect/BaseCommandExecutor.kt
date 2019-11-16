package com.factorymarket.rxelm.effect

import com.factorymarket.rxelm.cmd.*
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.program.MessageConsumer

abstract class RunningEffect {

    abstract fun cancel()

    abstract fun isRunning() : Boolean
}

abstract class BaseCommandExecutor<T : RunningEffect>(
        val logTag : String,
        val logger: RxElmLogger?
) : CommandExecutor {

    /**
     * Since we can cancel commands by their class, we hold commands in map bags by the hashcode
     * of the class.
     */
    val runningEffectsHolder: MutableMap<Int, MutableMap<Int, T>> = mutableMapOf()
    lateinit var messageConsumer : MessageConsumer

    override fun executeCmd(cmd: Cmd) {
        when (cmd) {
            is SwitchCmd -> {
                val cmdDisposablesMap = runningEffectsHolder[cmd::class.hashCode()]
                if (cmdDisposablesMap != null) {
                    cmdDisposablesMap.values.forEach {
                        if (it.isRunning()){
                            it.cancel()
                        }
                    }
                    cmdDisposablesMap.clear()
                }
                handleCmd(cmd)
            }
            is CancelCmd -> {
                val runningEffectMap = runningEffectsHolder[cmd.cancelCmd::class.hashCode()]
                        ?: return

                val runningEffect = runningEffectMap[cmd.cancelCmd.hashCode()]
                if (runningEffect != null && runningEffect.isRunning()) {
                    logCmd("elm cancel cmd:${cmd.cancelCmd}")
                    runningEffect.cancel()
                }
            }
            is CancelByClassCmd<*> -> {
                val runningEffectMap = runningEffectsHolder[cmd.cmdClass.hashCode()]
                        ?: return
                runningEffectMap.values.forEach { effect ->
                    if (effect.isRunning()) {
                        logCmd("elm cancel cmd:${cmd.cmdClass}")
                        effect.cancel()
                    }
                }
            }
            is ProxyCmd -> messageConsumer.accept(cmd.msg)
            else -> handleCmd(cmd)
        }
    }

    abstract fun handleCmd(cmd: Cmd)

    fun logCmd(message: String) {
        logger?.takeIf { it.logType().needToShowCommands() }?.log(logTag, message)
    }

    override fun stop() {
        runningEffectsHolder.values.forEach {
            it.values.forEach { disposable ->
                if (disposable.isRunning()) {
                    disposable.cancel()
                }
            }
        }
    }

    fun saveRunningEffect(cmd: Cmd, runningEffect: T) {
        val cmdDisposablesMap = runningEffectsHolder[cmd::class.hashCode()]
        if (cmdDisposablesMap != null) {
            val oldDisposable = cmdDisposablesMap[cmd.hashCode()]
            if (oldDisposable != null && oldDisposable.isRunning()) {
                saveOldEffect(oldDisposable)
            }
            cmdDisposablesMap[cmd.hashCode()] = runningEffect
        } else {
            val disposablesMap = mutableMapOf<Int, T>()
            disposablesMap[cmd.hashCode()] = runningEffect
            runningEffectsHolder[cmd::class.hashCode()] = disposablesMap
        }
    }

    open fun saveOldEffect(oldDisposable: T) {

    }

    override fun addMessageConsumer(mc: MessageConsumer) {
        messageConsumer = mc
    }

}