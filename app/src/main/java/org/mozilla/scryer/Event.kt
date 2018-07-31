package org.mozilla.scryer

import android.arch.lifecycle.Observer

open class Event<out T>(private val content: T) {
    private var handled = false

    fun getUnhandledContent(): T? {
        return if (handled) {
            null
        } else {
            handled = true
            content
        }
    }
}

class EventObserver<T>(private val onEvent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(t: Event<T>?) {
        t?.getUnhandledContent()?.let(onEvent)
    }
}
