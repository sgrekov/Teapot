package com.factorymarket.rxelm.sub

import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.program.MessageConsumer
import com.factorymarket.rxelm.program.Program
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.util.*

fun SubScope(dispatcher: CoroutineDispatcher): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

class FlowSub<S : State>(dispatcher: CoroutineDispatcher) : Sub<S>, CoroutineScope by SubScope(dispatcher) {

    private var messageConsumer: MessageConsumer? = null
    private val subs: Queue<Flow<Msg>> = LinkedList()
    private val conditionalSubs: Queue<Pair<(S) -> Boolean, Flow<Msg>>> = LinkedList()

    override fun setMessageConsumer(mc: MessageConsumer) {
        messageConsumer = mc
    }

    /**
     * Subscribe all data sources to [Program.accept(Message)][Program.accept]
     *
     *
     * Checks if conditional data sources' predicate is true.
     *
     * If it is, subscribes them if not - just delete.
     */
    @InternalCoroutinesApi
    override fun subscribe(state: S) {
        println("subscribe")
        moveConditionalSubsToSubsIfTheyMatchPredicate(state)

        if (subs.isEmpty()) {
            return
        }

        subs.forEach { flow ->
            launch {
                flow?.collect {
                    messageConsumer?.accept(it)
                }
            }
        }
        subs.clear()
    }

    override fun dispose() {
        cancel()
    }

    /**
     * Adds [Msg] flow to collection which result will be passed
     * to [Program's accept(Message)][Program.accept] method
     */
    fun addMessageFlow(messageFlow: Flow<out Msg>): FlowSub<S> {
        subs.add(messageFlow)
        return this
    }

    /**
     * Same as [addMessageObservable], but will be ignored if Predicate will fail on first check
     */
    fun addConditionalMessageObservable(
            predicate: (S) -> Boolean,
            flow: Flow<out Msg>
    ): FlowSub<S> {
        conditionalSubs.add(predicate to flow)
        return this
    }

    private fun moveConditionalSubsToSubsIfTheyMatchPredicate(state: S) {
        if (conditionalSubs.isEmpty()) {
            return
        }

        val iter = conditionalSubs.iterator()
        while (iter.hasNext()) {
            val delayedSub = iter.next()
            if (delayedSub.first.invoke(state)) {
                iter.remove()
                subs.add(delayedSub.second)
            }
        }
    }

}