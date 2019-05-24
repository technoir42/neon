package com.sch.neon

import com.sch.rxjava2.extensions.DisposableObservable
import com.sch.rxjava2.extensions.autoConnectDisposable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.UnicastSubject

class MainLoop<Event : Any, State : Any, Effect : Any>(
    private val reducer: StateReducer<Event, State, Effect>,
    private val effectHandler: EffectHandler<Effect, Event>? = null,
    private val externalEvents: Observable<out Event> = Observable.empty(),
    private val listener: Listener<Event, State, Effect>? = null
) {
    private val events = UnicastSubject.create<Event>()
    private val effects = UnicastSubject.create<Effect>()

    fun dispatch(event: Event) {
        events.onNext(event)
    }

    fun loop(initialState: State, vararg initialEffects: Effect): DisposableObservable<State> {
        return loop(next(initialState, *initialEffects))
    }

    fun loop(initialStateAndEffects: StateWithEffects<State, Effect>): DisposableObservable<State> {
        val effectHandlerEvents = effects
            .observeOn(Schedulers.io())
            .publish { effects -> effectHandler?.handle(effects) ?: Observable.never() }

        return Observable.merge(events, externalEvents, effectHandlerEvents)
            .observeOn(Schedulers.computation())
            .doOnNext { event -> listener?.onEvent(event) }
            .scan(initialStateAndEffects) { stateWithEffects, event -> reducer.reduce(stateWithEffects.state, event) }
            .map { stateWithEffects ->
                stateWithEffects.effects.forEach { effect ->
                    listener?.onEffect(effect)
                    effects.onNext(effect)
                }
                stateWithEffects.state
            }
            .distinctUntilChanged()
            .doOnNext { state -> listener?.onState(state) }
            .replay(1)
            .autoConnectDisposable()
    }

    interface Listener<in Event : Any, in State : Any, in Effect : Any> {
        fun onEvent(event: Event)

        fun onState(state: State)

        fun onEffect(effect: Effect)
    }
}

fun <Event : Any, State : Any, Effect : Any> compose(vararg listeners: MainLoop.Listener<Event, State, Effect>): MainLoop.Listener<Event, State, Effect> {
    return object : MainLoop.Listener<Event, State, Effect> {
        override fun onEvent(event: Event) {
            listeners.forEach { it.onEvent(event) }
        }

        override fun onState(state: State) {
            listeners.forEach { it.onState(state) }
        }

        override fun onEffect(effect: Effect) {
            listeners.forEach { it.onEffect(effect) }
        }
    }
}
