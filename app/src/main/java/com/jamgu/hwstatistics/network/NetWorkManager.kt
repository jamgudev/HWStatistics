package com.jamgu.hwstatistics.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.TrafficStats
import com.jamgu.hwstatistics.INeedPermission

/**
 * Created by jamgu on 2021/10/15
 */
object NetWorkManager: INeedPermission {
    private const val NETWORK_SPEED_NOT_AVAIABLE = -1L

    private var mLastRxBytes: Long = NETWORK_SPEED_NOT_AVAIABLE
    private var mLastTimeStamp: Long = -1L

    private fun getTotalRxBytes(context: Context?): Long {
        context ?: return NETWORK_SPEED_NOT_AVAIABLE
        return if (TrafficStats.getUidRxBytes(context.applicationInfo.uid) == TrafficStats.UNSUPPORTED.toLong()) 0
        else TrafficStats.getTotalRxBytes() / 1024 //转为KB
    }

    fun getNetWorkSpeed(context: Context?): Float {
        val curRxBytes = getTotalRxBytes(context)
        val currentTimeMillis = System.currentTimeMillis()
        return if (mLastRxBytes < 0) {
            mLastRxBytes = curRxBytes
            mLastTimeStamp = currentTimeMillis
            0.0f
        } else {
            val speed: Long = (curRxBytes - mLastRxBytes) * 1000 / (currentTimeMillis - mLastTimeStamp) //毫秒转换
            val speed2: Long = (curRxBytes - mLastRxBytes) * 1000 % (currentTimeMillis - mLastTimeStamp) //毫秒转换

            mLastRxBytes = curRxBytes
            mLastTimeStamp = currentTimeMillis

            // kb/s
            String.format("%.2f", "$speed.$speed2".toFloat()).toFloat()
        }
    }

    @SuppressLint("MissingPermission")
    fun getNetworkType(context: Context?): Int {
        return CpNetUtil.getInstance().getNetType(context).value
    }

    override fun permission(): Array<String> = arrayOf(android.Manifest.permission.READ_PHONE_STATE)
}