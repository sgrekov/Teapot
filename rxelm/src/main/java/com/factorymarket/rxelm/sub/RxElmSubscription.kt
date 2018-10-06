package com.factorymarket.rxelm.sub

import com.factorymarket.rxelm.msg.Msg
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.program.Program
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.LinkedList
import java.util.Queue

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
class RxElmSubscriptions<S : State> {

    private val subs: Queue<Observable<out Msg>> = LinkedList<Observable<out Msg>>()
    private val conditionalSubs: Queue<Pair<(S) -> Boolean, Observable<out Msg>>> =
        LinkedList<Pair<(S) -> Boolean, Observable<out Msg>>>()
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    /**
     * Adds <[Msg]> observable to collection which result will be passed
     * to [Program's accept(Message)][Program.accept] method
     */
    fun addMessageObservable(observable: Observable<out Msg>): RxElmSubscriptions<S> {
        subs.add(observable)
        return this
    }

    /**
     * Same as [addMessageObservable], but will be ignored if Predicate will fail on first check
     */
    fun addConditionalMessageObservable(
        predicate: (S) -> Boolean,
        observable: Observable<out Msg>
    ): RxElmSubscriptions<S> {
        conditionalSubs.add(predicate to observable)
        return this
    }

    /**
     * Subscribe all data sources to [Program.accept(Message)][Program.accept]
     *
     *
     * Checks if conditional data sources' predicate is true.
     *
     * If it is, subscribes them if not - just delete.
     */
    fun subscribe(program: Program<S>, state: S) {
        moveConditionalSubsToSubsIfTheyMatchPredicate(state)

        if (subs.isEmpty()) {
            return
        }
        var sub = subs.poll()
        while (sub != null) {
            val disposable = sub
                .observeOn(program.outputScheduler)
                .subscribe { msg ->
                    program.accept(msg)
                }
            compositeDisposable.add(disposable)
            sub = subs.poll()
        }
    }

    /**
     * Dispose all subscriptions
     */
    fun dispose() {
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
                subs.add(delayedSub.second)
            }
        }
    }
}
