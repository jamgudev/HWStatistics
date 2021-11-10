package com.jamgu.hwstatistics.thread

interface ThreadPoolMonitor {

    fun beforeSubmitWorker(var1: Long, var2: ThreadPool, var3: Runnable)

    fun onSubmitWorker(var1: Long, var2: ThreadPool, var3: Runnable)

    fun onWorkerBegin(var1: Long, var2: ThreadPool, var3: Runnable)

    fun onWorkerCancel(var1: Long, var2: ThreadPool, var3: Runnable)

    fun onWorkerFinish(var1: Long, var2: ThreadPool, var3: Runnable)


}