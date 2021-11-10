package com.jamgu.hwstatistics.thread

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class PriorityThreadFactory(name: String, priority: Int): ThreadFactory {

    private val mPriority: Int = priority
    private val mName: String = name

    private val mNumber = AtomicInteger()

    override fun newThread(r: Runnable?): Thread {
        return object : Thread(r, this.mName + '-' + this.mNumber.getAndIncrement()) {
            override fun run() {
                android.os.Process.setThreadPriority(this@PriorityThreadFactory.mPriority)
                super.run()
            }
        }
    }
}