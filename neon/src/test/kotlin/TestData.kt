package com.sch.neon

import com.sch.neon.TestEffect.FirstEffect
import com.sch.neon.TestEvent.FirstEvent
import com.sch.neon.TestEvent.SecondEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import java.util.concurrent.ConcurrentLinkedQueue

sealed class TestEvent : Event() {
    data class FirstEvent(val counter: Int) : TestEvent()
    object SecondEvent : TestEvent()
}

sealed class TestEffect : Effect() {
    data class FirstEffect(val counter: Int) : TestEffect()
}

data class TestViewState(
    val counter: Int = 0
)

class TestStateReducer : StateReducer<TestEvent, TestViewState, TestEffect> {
    override fun reduce(state: TestViewState, event: TestEvent): StateWithEffects<TestViewState, TestEffect> = when (event) {
        is FirstEvent -> {
            next(state.copy(counter = event.counter))
        }

        SecondEvent -> {
            next(state, FirstEffect(state.counter))
        }
    }
}

class TestEffectHandler : EffectHandler<TestEffect, TestEvent> {
    val effects = ReplaySubject.create<TestEffect>()
    val events = PublishSubject.create<TestEvent>()

    override fun handle(effects: Observable<TestEffect>): Observable<TestEvent> {
        effects.subscribe(this.effects)
        return events
    }
}

class TestListener : MainLoop.Listener<Any, Any, Any> {
    val events = ConcurrentLinkedQueue<Any>()
    val states = ConcurrentLinkedQueue<Any>()
    val effects = ConcurrentLinkedQueue<Any>()

    override fun onEvent(event: Any) {
        events += event
    }

    override fun onState(state: Any) {
        states += state
    }

    override fun onEffect(effect: Any) {
        effects += effect
    }
}
