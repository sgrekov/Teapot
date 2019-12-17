package dev.teapot.effect.coroutine

import dev.teapot.cmd.Cmd
import dev.teapot.cmd.ViewCmd
import dev.teapot.contract.CoroutinesEffectHandler
import dev.teapot.effect.BaseCommandExecutor
import dev.teapot.effect.RunningEffect
import dev.teapot.log.RxElmLogger
import dev.teapot.msg.ErrorMsg
import dev.teapot.msg.Msg
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