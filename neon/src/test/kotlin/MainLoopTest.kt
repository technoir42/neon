package com.sch.neon

import com.sch.neon.TestEffect.FirstEffect
import com.sch.neon.TestEvent.FirstEvent
import com.sch.neon.TestEvent.SecondEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ConcurrentLinkedQueue

@ExtendWith(RxSchedulersOverrideExtension::class)
class MainLoopTest {
    private val inputEvents = PublishSubject.create<TestEvent>()
    private val effects = ReplaySubject.create<TestEffect>()
    private val effectHandlerEvents = PublishSubject.create<TestEvent>()
    private val listener = TestListener()

    private val loop = MainLoop(
        reducer = TestStateReducer(),
        externalEvents = inputEvents,
        effectHandler = ::effectHandler,
        listener = listener
    )

    @Test
    fun `Emits initial state to subscriber`() {
        val initialState = TestViewState()
        val state = loop.loop(initialState)

        state.test().assertValuesOnly(initialState)
    }

    @Test
    fun `Dispatches initial effects to effect handler`() {
        val initialEffect1 = FirstEffect(1)
        val initialEffect2 = FirstEffect(2)
        val state = loop.loop(TestViewState(), initialEffect1, initialEffect2)

        state.test()

        assertThat(effects.values).containsExactly(initialEffect1, initialEffect2)
    }

    @Test
    fun `Passes dispatched events to reducer`() {
        val initialState = TestViewState()
        val state = loop.loop(initialState)

        val ts = state.test()
        loop.dispatch(FirstEvent(1))
        ts.assertValuesOnly(initialState, TestViewState(1))
    }

    @Test
    fun `Accumulates dispatched events until first subscriber`() {
        val initialState = TestViewState()
        val state = loop.loop(initialState)

        loop.dispatch(FirstEvent(1))
        loop.dispatch(FirstEvent(2))

        state.test().assertValuesOnly(initialState, TestViewState(1), TestViewState(2))
    }

    @Test
    fun `Emits last state to new subscribers`() {
        val initialState = TestViewState()
        val state = loop.loop(initialState)

        val ts = state.test()
        loop.dispatch(FirstEvent(1))
        ts.cancel()

        state.test().assertValuesOnly(TestViewState(1))
    }

    @Test
    fun `Passes input events to reducer`() {
        val initialState = TestViewState()
        val state = loop.loop(initialState)

        val ts = state.test()
        inputEvents.onNext(FirstEvent(1))
        ts.assertValuesOnly(initialState, TestViewState(1))
    }

    @Test
    fun `Ignores input events until first subscriber`() {
        val initialState = TestViewState()
        val state = loop.loop(initialState)

        inputEvents.onNext(FirstEvent(1))

        state.test()
            .assertValuesOnly(initialState)
    }

    @Test
    fun `Disposing state observable terminates subscriptions`() {
        val state = loop.loop(TestViewState())

        state.test()

        assertTrue(inputEvents.hasObservers())
        assertTrue(effectHandlerEvents.hasObservers())

        state.dispose()

        assertFalse(inputEvents.hasObservers())
        assertFalse(effectHandlerEvents.hasObservers())
    }

    private fun effectHandler(effects: Observable<TestEffect>): Observable<TestEvent> {
        effects.subscribe(this.effects)
        return effectHandlerEvents
    }
}

private sealed class TestEvent : Event() {
    data class FirstEvent(val counter: Int) : TestEvent()
    object SecondEvent : TestEvent()
}

private sealed class TestEffect : Effect() {
    data class FirstEffect(val counter: Int) : TestEffect()
}

private data class TestViewState(
    val counter: Int = 0
)

private class TestStateReducer : StateReducer<TestEvent, TestViewState, TestEffect> {
    override fun reduce(state: TestViewState, event: TestEvent): StateWithEffects<TestViewState, TestEffect> = when (event) {
        is FirstEvent -> {
            next(state.copy(counter = event.counter))
        }

        SecondEvent -> {
            next(state, FirstEffect(state.counter))
        }
    }
}

private class TestListener : MainLoop.Listener<Any, Any, Any> {
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
