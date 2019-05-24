package com.sch.neon

interface StateReducer<Event : Any, State : Any, Effect : Any> {
    fun reduce(state: State, event: Event): StateWithEffects<State, Effect>
}
