package com.factorymarket.rxelm.sub

import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.ds.DoubleLinkedList
import com.factorymarket.rxelm.program.MessageConsumer
import com.factorymarket.rxelm.program.Program
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

/**
 * Container for storing a collection of [Observables][Observable]
 * and [Disposables][io.reactivex.disposables.Disposable]
 *
 * Initialized with a set of [Observable][Observable] or [Observables][Observable] with Predicates
 *
 * [Program] during it's work calls [subscribe] on every message update, which
 * make all unconditional subscriptions and suitable conditional subscriptions to subscribe
 * and pass their return to [Program.accept]
 */
class RxSub<S : State>(private val outputScheduler : Scheduler) : Sub<S> {

    private val subs: DoubleLinkedList<Observable<out Msg>> = DoubleLinkedList()
    private val conditionalSubs: DoubleLinkedList<Pair<(S) -> Boolean, Observable<out Msg>>> = DoubleLinkedList()
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var messageConsumer : MessageConsumer? = null

    /**
     * Adds <[Msg]> observable to collection which result will be passed
     * to [Program's accept(Message)][Program.accept] method
     */
    fun addMessageObservable(observable: Observable<out Msg>): RxSub<S> {
        subs.addLast(observable)
        return this
    }

    /**
     * Same as [addMessageObservable], but will be ignored if Predicate will fail on first check
     */
    fun addConditionalMessageObservable(
        predicate: (S) -> Boolean,
        observable: Observable<out Msg>
    ): RxSub<S> {
        conditionalSubs.addLast(predicate to observable)
        return this
    }

    override fun setMessageConsumer(mc: MessageConsumer) {
        this.messageConsumer = mc
    }

    /**
     * Subscribe all data sources to [Program.accept(Message)][Program.accept]
     *
     * Checks if conditional data sources' predicate is true.
     *
     * If it is, subscribes them if not - just delete.
     */
    override fun subscribe(state: S) {
        moveConditionalSubsToSubsIfTheyMatchPredicate(state)

        if (subs.isEmpty()) {
            return
        }
        var sub : Observable<out Msg>? = subs.last()
        while (sub != null) {
            val disposable = sub
                .observeOn(outputScheduler)
                    //TODO: add error handler
                .subscribe { msg ->
                    messageConsumer?.accept(msg)
                }
            compositeDisposable.add(disposable)
            subs.removeLast()
            sub = if (!subs.isEmpty()) subs.last() else null
        }
    }

    /**
     * Dispose all subscriptions
     */
    override fun dispose() {
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
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
                subs.addLast(delayedSub.second)
            }
        }
    }
}
