package com.jamgu.hwstatistics.thread

interface Future<T> {

    fun cancel()

    fun isCancelled(): Boolean

    fun isDone(): Boolean

    fun get(): T

    fun waitDone()

    fun setCancelListener(var1: CancelListener)

    interface CancelListener {
        fun onCancel()
    }

}