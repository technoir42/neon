@file:Suppress("NOTHING_TO_INLINE")

package com.sch.neon

data class StateWithEffects<out State : Any, out Effect : Any>(
    val state: State,
    val effects: List<Effect>
)

inline fun <State : Any, Effect : Any> next(state: State, vararg effects: Effect): StateWithEffects<State, Effect> =
    StateWithEffects(state, effects.toList())
