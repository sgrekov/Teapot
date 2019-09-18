package com.factorymarket.rxelm.effect.coroutine

import com.factorymarket.rxelm.cmd.*
import com.factorymarket.rxelm.contract.CoroutinesEffectHandler
import com.factorymarket.rxelm.log.RxElmLogger
import com.factorymarket.rxelm.msg.ErrorMsg
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.MessageConsumer
import kotlinx.coroutines.*
import java.util.*

class CoroutinesCommandExecutor(
        private val effectHandler: CoroutinesEffectHandler,
        private val outputDispatcher: CoroutineDispatcher,
        private val logTag: String,
        private val handleCmdErrors: Boolean,
        private val logger: RxElmLogger?) : CommandExecutor {

    lateinit var messageConsumer: MessageConsumer
    private val parentJob = SupervisorJob()
    private val executorScope = CoroutineScope(parentJob)
    /**
     * Since we can cancel commands by their class, we hold commands in map bags by the hashcode
     * of the class.
     */
    private val commandsJobsMap: MutableMap<Int, MutableMap<Int, Job>> = TreeMap()

    override fun executeCmd(cmd: Cmd) {
        when (cmd) {
            is SwitchCmd -> {

            }
            is CancelCmd -> {
                val jobBag = commandsJobsMap[cmd.cancelCmd::class.hashCode()] ?: return

                val commandJob = jobBag[cmd.cancelCmd.hashCode()]
                if (commandJob != null && commandJob.isActive && !commandJob.isCompleted) {
                    logCmd("elm cancel cmd:${cmd.cancelCmd}")
                    commandJob.cancel()
                }
            }
            is CancelByClassCmd<*> -> {
                val jobBag = commandsJobsMap[cmd.cmdClass.hashCode()] ?: return

                jobBag.values.forEach { job ->
                    if (job.isActive && !job.isCompleted) {
                        logCmd("elm cancel cmd:${cmd.cmdClass}")
                        job.cancel()
                    }
                }
            }
            is ProxyCmd -> messageConsumer.accept(cmd.msg)
            else -> handleCmd(cmd)
        }
    }

    private fun handleCmd(cmd: Cmd) {
        val cmdDispatcher = if (cmd is ViewCmd) outputDispatcher else Dispatchers.IO

        val cmdJob = executorScope.launch(Dispatchers.Main) {
            val msg = if (handleCmdErrors) {
                try {
                    callComponent(cmdDispatcher, cmd)
                } catch (e: Exception) {
                    ErrorMsg(e, cmd)
                }
            } else {
                callComponent(cmdDispatcher, cmd)
            }
            withContext(Dispatchers.Main) {
                messageConsumer.accept(msg)
            }
        }
        saveJob(cmd, cmdJob)
    }

    private suspend fun callComponent(cmdDispatcher: CoroutineDispatcher, cmd: Cmd): Msg {
        return withContext(cmdDispatcher) {
            effectHandler.call(cmd)
        }
    }

    private fun saveJob(cmd: Cmd, cmdJob: Job) {
        val bag = commandsJobsMap[cmd::class.hashCode()]
        if (bag == null) {
            val newBag = TreeMap<Int, Job>()
            newBag[cmd.hashCode()] = cmdJob
            commandsJobsMap[cmd::class.hashCode()] = newBag
        } else {
            bag[cmd.hashCode()] = cmdJob
        }
    }

    private fun logCmd(message: String) {
        logger?.takeIf { it.logType().needToShowCommands() }?.log(logTag, message)
    }

    override fun stop() {
        parentJob.cancel()
    }

    override fun addMessageConsumer(mc: MessageConsumer) {
        messageConsumer = mc
    }
}