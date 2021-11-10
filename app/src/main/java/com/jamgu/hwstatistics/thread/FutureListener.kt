package com.jamgu.hwstatistics.thread

interface FutureListener<T> {

    fun onFutureBegin(var1: Future<T>)

    fun onFutureDone(var1: Future<T>)

}