package com.jamgu.hwstatistics.timer

import android.os.Handler
import android.os.Looper

/**
 * Created by jamgu on 2021/11/11
 */
class RoughTimer(private val looper: Looper) {

    private var mHandler: Handler? = null

    private lateinit var mMission: Runnable

    private var isRunning: Boolean = false

    fun isStarted(): Boolean {
        return isRunning
    }

    fun run(mission: () -> Unit, delay: Long) {
        if (isRunning) return

        mHandler = Handler(looper)

        object : Runnable {
            override fun run() {
                mission.invoke()

                isRunning = true
                mHandler?.postDelayed(this, delay)
            }
        }.also { mMission = it }

        mHandler?.post(mMission)
    }

    fun close() {
        isRunning = false
        mHandler?.removeCallbacks(mMission)
        mHandler = null
    }

}