package com.sch.neon

import io.reactivex.Observable

interface EffectHandler<Effect : Any, Event : Any> {
    fun handle(effects: Observable<Effect>): Observable<Event>
}
