package com.sch.neon.timber

import com.sch.neon.MainLoop
import timber.log.Timber

class TimberLogger(private val tag: String) : MainLoop.Listener<Any, Any, Any> {
    override fun onEvent(event: Any) {
        Timber.tag(tag).d("%s", event)
    }

    override fun onState(state: Any) {
        Timber.tag(tag).d("%s", state)
    }

    override fun onEffect(effect: Any) {
        Timber.tag(tag).d("%s", effect)
    }
}
