package com.sch.neon

import com.github.technoir42.rxjava2.junit5.OverrideSchedulersExtension
import com.sch.neon.TestEffect.FirstEffect
import com.sch.neon.TestEvent.FirstEvent
import com.sch.neon.TestEvent.SecondEvent
import io.reactivex.subjects.PublishSubject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(OverrideSchedulersExtension::class)
class MainLoopTest {
    private val stateReducer = TestStateReducer()
    private val effectHandler = TestEffectHandler()
    private val inputEvents = PublishSubject.create<TestEvent>()
    private val listener = TestListener()

    private val loop = MainLoop(
        reducer = stateReducer,
        effectHandler = effectHandler,
        externalEvents = inputEvents,
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

        effectHandler.effects
            .test()
            .assertValues(initialEffect1, initialEffect2)
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
        assertTrue(effectHandler.events.hasObservers())

        state.dispose()

        assertFalse(inputEvents.hasObservers())
        assertFalse(effectHandler.events.hasObservers())
    }

    @Test
    fun `Dispatches effects from reducer to effect handler`() {
        loop.loop(TestViewState(1)).test()

        loop.dispatch(SecondEvent)

        effectHandler.effects
            .test()
            .assertValues(FirstEffect(1))
    }

    @Test
    fun `Dispatches events from effect handler to reducer`() {
        val ts = loop.loop(TestViewState()).test()

        effectHandler.events.onNext(FirstEvent(1))

        ts.assertValues(
            TestViewState(),
            TestViewState(1)
        )
    }
}
