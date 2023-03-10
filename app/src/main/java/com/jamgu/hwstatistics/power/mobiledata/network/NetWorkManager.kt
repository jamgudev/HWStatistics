package com.jamgu.hwstatistics.power.mobiledata.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.TrafficStats
import android.net.wifi.WifiManager
import com.jamgu.hwstatistics.power.INeedPermission
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


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
            if  (currentTimeMillis - mLastTimeStamp == 0L) return 0.0f

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

    /**
     * 判断热点是否开启
     * @param context
     * @return
     */
    fun isWifiApEnable(context: Context): Boolean {
        try {
            val manager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            //通过放射获取 getWifiApState()方法
            val method: Method = manager.javaClass.getDeclaredMethod("getWifiApState")
            //调用getWifiApState() ，获取返回值
            val state = method.invoke(manager) as Int
            //通过放射获取 WIFI_AP的开启状态属性
            val field: Field = manager.javaClass.getDeclaredField("WIFI_AP_STATE_ENABLED")
            //获取属性值
            val value = field.get(manager) as Int
            //判断是否开启
            return state == value
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun permission(): Array<String> = arrayOf(android.Manifest.permission.READ_PHONE_STATE)
}