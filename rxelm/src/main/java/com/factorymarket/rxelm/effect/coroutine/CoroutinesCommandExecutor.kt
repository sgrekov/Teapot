package com.factorymarket.rxelm.effect.coroutine

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.cmd.ViewCmd
import com.factorymarket.rxelm.contract.CoroutinesEffectHandler
import com.factorymarket.rxelm.effect.BaseCommandExecutor
import com.factorymarket.rxelm.effect.RunningEffect
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Msg
import kotlinx.coroutines.*

class RunningJob(val job: Job) : RunningEffect() {

    override fun cancel() {
        job.cancel()
    }

    override fun isRunning(): Boolean = job.isActive

}

class CoroutinesCommandExecutor(
        private val effectHandler: CoroutinesEffectHandler,
        private val outputDispatcher: CoroutineDispatcher,
        logTag: String,
        private val handleCmdErrors: Boolean,
        logger: RxElmLogger?) : BaseCommandExecutor<RunningJob>(logTag, logger) {

    private val parentJob = SupervisorJob()
    private val executorScope = CoroutineScope(parentJob)

    override fun handleCmd(cmd: Cmd) {
        val cmdDispatcher = if (cmd is ViewCmd) outputDispatcher else Dispatchers.IO

        val cmdJob = executorScope.launch {
            val msg = if (handleCmdErrors) {
                try {
                    callComponent(cmdDispatcher, cmd)
                } catch (e: Exception) {
                    ErrorMsg(e, cmd)
                }
            } else {
                callComponent(cmdDispatcher, cmd)
            }
            withContext(outputDispatcher) {
                messageConsumer.accept(msg)
            }
        }
        saveRunningEffect(cmd, RunningJob(cmdJob))
    }

    private suspend fun callComponent(cmdDispatcher: CoroutineDispatcher, cmd: Cmd): Msg {
        return withContext(cmdDispatcher) {
            effectHandler.call(cmd)
        }
    }

    override fun stop() {
        super.stop()
        parentJob.cancel()
    }

}