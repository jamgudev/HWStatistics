@file:Suppress("unused")

package com.jamgu.hwstatistics.power

import com.jamgu.hwstatistics.power.mobiledata.cpu.model.CPU
import com.jamgu.hwstatistics.util.roundToDecimals

private const val TAG = "StatisticDoMain2"

/**
 * Created by jamgu on 2021/11/03
 *
 * 用于存储所有收集的信息
 */
class StatisticDoMain2 internal constructor(private val builder: Builder2){

    fun getCurTimeMills(): String = builder.curTimeMills

    fun getSystemOn(): Int = builder.isSystemOn

    fun getScreenOn(): Int = builder.isScreenOn

    fun getScreenBrightness(): Int = builder.screenBrightness

    fun getMusicOn(): Int = builder.isMusicOn

    fun getPhoneRing(): Int = builder.isPhoneRinging

    fun getPhoneOffHook(): Int = builder.isPhoneOffHook

    fun wifiNetwork(): Int = builder.isWifiNetwork

    fun is2GNetwork(): Int = builder.is2GNetwork
    fun is3GNetwork(): Int = builder.is3GNetwork
    fun is4GNetwork(): Int = builder.is4GNetwork
    fun is5GNetwork(): Int = builder.is5GNetwork
    fun isOtherNetwork(): Int = builder.isOtherNetwork

    fun networkSpeed(): Float = builder.netWorkSpeed

    fun cpu0(): Float = builder.cpu0

    fun cpu1(): Float = builder.cpu1

    fun cpu2(): Float = builder.cpu2

    fun cpu3(): Float = builder.cpu3

    fun cpu4(): Float = builder.cpu4

    fun cpu5(): Float = builder.cpu5

    fun cpu6(): Float = builder.cpu6

    fun cpu7(): Float = builder.cpu7

    fun gpuCurFreq(): Float = builder.gpuCurFreq

    fun gpuCurUtil(): Float = builder.gpuCurUtil

    fun blEnabled(): Int = builder.blEnabled

    fun blConnectedNum(): Int = builder.blConnectedNum

    fun memCurAvailable(): Float = builder.memCurAvailable

}

class Builder2 {
    var curTimeMills: String = ""

    // system_on
    var isSystemOn: Int = 0

    // screen brightness
    var screenBrightness: Int = 0

    // screen_on
    var isScreenOn: Int = 0

    // phone_ring
    var isPhoneRinging: Int = 0

    // phone_off_hook
    var isPhoneOffHook: Int = 0

    // music_status
    var isMusicOn: Int = 0

    // network_wifi
    var isWifiNetwork: Int = 0

    // mobile data
    var is2GNetwork: Int = 0
    var is3GNetwork: Int = 0
    var is4GNetwork: Int = 0
    var is5GNetwork: Int = 0
    var isOtherNetwork: Int = 0

    // network speed kb
    var netWorkSpeed: Float = 0.0f
    var isWifiApEnable: Int = 0

    // cpu0 当前频率
    var cpu0: Float = 0.0f

    // cpu0 频率x温度
    var cpu1: Float = 0.0f

    // cpu0 频率x温度
    var cpu2: Float = 0.0f

    // cpu0 频率x温度
    var cpu3: Float = 0.0f

    // cpu0 频率x温度
    var cpu4: Float = 0.0f

    // cpu0 频率x温度
    var cpu5: Float = 0.0f

    // cpu0 频率x温度
    var cpu6: Float = 0.0f

    // cpu0 频率x温度
    var cpu7: Float = 0.0f

    var cpuTemp0: Float = 0.0f
    var cpuTemp1: Float = 0.0f
    var cpuTemp2: Float = 0.0f
    var cpuTemp3: Float = 0.0f
    var cpuTemp4: Float = 0.0f
    var cpuTemp5: Float = 0.0f
    var cpuTemp6: Float = 0.0f
    var cpuTemp7: Float = 0.0f

    // 利用率
    var totalCpu: Float = 0.0f

    var cpu0utils: Float = 0.0f
    var cpu1utils: Float = 0.0f
    var cpu2utils: Float = 0.0f
    var cpu3utils: Float = 0.0f
    var cpu4utils: Float = 0.0f
    var cpu5utils: Float = 0.0f
    var cpu6utils: Float = 0.0f
    var cpu7utils: Float = 0.0f

    // gpu 当前频率
    var gpuCurFreq: Float = 0.0f
    // gpu 当前利用率
    var gpuCurUtil: Float = 0.0f

    // 蓝牙是否开启
    var blEnabled: Int = 0
    // 蓝牙历史连接数量
    var blConnectedNum: Int = 0

    // ------ mem info
    var memFree: Float = 0.0f

    var memCurAvailable: Float = 0.0f

    var memActive: Float = 0.0f

    var memInactive: Float = 0.0f

    var memDirty: Float = 0.0f

    var memAnonPages: Float = 0.0f

    var memMapped: Float = 0.0f
    // ------ mem info

    fun curTimeMills(curTimeMills: String?): Builder2 {
        curTimeMills ?: return this
        this.curTimeMills = curTimeMills
        return this
    }

    fun systemOn(isOn: Int?): Builder2 {
        isOn ?: return this
        this.isSystemOn = isOn
        return this
    }

    fun screenBrightness(brightness: Int?): Builder2 {
        brightness ?: return this
        this.screenBrightness = brightness
        return this
    }

    fun screenOn(isOn: Int?): Builder2 {
        isOn ?: return this
        this.isScreenOn = isOn
        return this
    }

    fun phoneRing(isRing: Int?): Builder2 {
        isRing ?: return this
        this.isPhoneRinging = isRing
        return this
    }

    fun phoneOffHook(isOffHook: Int?): Builder2 {
        isOffHook ?: return this
        this.isPhoneOffHook = isOffHook
        return this
    }

    fun musicOn(isMusicOn: Int?): Builder2 {
        isMusicOn ?: return this
        this.isMusicOn = isMusicOn
        return this
    }

    fun isWifiNetwork(isWifi: Int?): Builder2 {
        isWifi ?: return this
        this.isWifiNetwork = isWifi
        return this
    }

    fun isWifiApEnable(enable: Boolean): Builder2 {
        this.isWifiApEnable = if (enable) 1 else 0
        return this
    }

    fun is2GNetwork(is2G: Int?): Builder2 {
        is2G ?: return this
        this.is2GNetwork = is2G
        return this
    }

    fun is3GNetwork(is3G: Int?): Builder2 {
        is3G ?: return this
        this.is3GNetwork = is3G
        return this
    }

    fun is4GNetwork(is4G: Int?): Builder2 {
        is4G ?: return this
        this.is4GNetwork = is4G
        return this
    }

    fun is5GNetwork(is5G: Int?): Builder2 {
        is5G ?: return this
        this.is5GNetwork = is5G
        return this
    }

    fun isOtherNetwork(isOther: Int?): Builder2 {
        isOther ?: return this
        this.isOtherNetwork = isOther
        return this
    }

    fun networkSpeed(speed: Float?): Builder2 {
        speed ?: return this
        this.netWorkSpeed = speed
        return this
    }

    fun cpus(cpus: ArrayList<CPU>?): Builder2 {
        cpus ?: return this
        if (cpus.isEmpty() || cpus.size != 8) {
            return this
        }
//        this.cpu0 = cpus[0].curFreq * cpus[0].temp
//        this.cpu1 = cpus[1].curFreq * cpus[1].temp
//        this.cpu2 = cpus[2].curFreq * cpus[2].temp
//        this.cpu3 = cpus[3].curFreq * cpus[3].temp
//        this.cpu4 = cpus[4].curFreq * cpus[4].temp
//        this.cpu5 = cpus[5].curFreq * cpus[5].temp
//        this.cpu6 = cpus[6].curFreq * cpus[6].temp
//        this.cpu7 = cpus[7].curFreq * cpus[7].temp

        this.cpu0 = (cpus[0].curFreq / 100f).roundToDecimals(2)
        this.cpu1 = (cpus[1].curFreq / 100f).roundToDecimals(2)
        this.cpu2 = (cpus[2].curFreq / 100f).roundToDecimals(2)
        this.cpu3 = (cpus[3].curFreq / 100f).roundToDecimals(2)
        this.cpu4 = (cpus[4].curFreq / 100f).roundToDecimals(2)
        this.cpu5 = (cpus[5].curFreq / 100f).roundToDecimals(2)
        this.cpu6 = (cpus[6].curFreq / 100f).roundToDecimals(2)
        this.cpu7 = (cpus[7].curFreq / 100f).roundToDecimals(2)

        this.cpuTemp0 = (cpus[0].temp / 10f).roundToDecimals(2)
        this.cpuTemp1 = (cpus[1].temp / 10f).roundToDecimals(2)
        this.cpuTemp2 = (cpus[2].temp / 10f).roundToDecimals(2)
        this.cpuTemp3 = (cpus[3].temp / 10f).roundToDecimals(2)
        this.cpuTemp4 = (cpus[4].temp / 10f).roundToDecimals(2)
        this.cpuTemp5 = (cpus[5].temp / 10f).roundToDecimals(2)
        this.cpuTemp6 = (cpus[6].temp / 10f).roundToDecimals(2)
        this.cpuTemp7 = (cpus[7].temp / 10f).roundToDecimals(2)

//        JLog.d(TAG, "cpu freq = ${this.cpu0}, temp = ${this.cpuTemp0}")
//        this.cpu0 = (this.cpu0.toDouble().pow(3.0) * this.cpuTemp0.toDouble().pow(2.0)).toFloat()
//        this.cpu1 = (this.cpu1.toDouble().pow(3.0) * this.cpuTemp1.toDouble().pow(2.0)).toFloat()
//        this.cpu2 = (this.cpu2.toDouble().pow(3.0) * this.cpuTemp2.toDouble().pow(2.0)).toFloat()
//        this.cpu3 = (this.cpu3.toDouble().pow(3.0) * this.cpuTemp3.toDouble().pow(2.0)).toFloat()
//        this.cpu4 = (this.cpu4.toDouble().pow(3.0) * this.cpuTemp4.toDouble().pow(2.0)).toFloat()
//        this.cpu5 = (this.cpu5.toDouble().pow(3.0) * this.cpuTemp5.toDouble().pow(2.0)).toFloat()
//        this.cpu6 = (this.cpu6.toDouble().pow(3.0) * this.cpuTemp6.toDouble().pow(2.0)).toFloat()
//        this.cpu7 = (this.cpu7.toDouble().pow(3.0) * this.cpuTemp7.toDouble().pow(2.0)).toFloat()

        /*if (this.totalCpu != 0f) {
            this.cpu0utils = cpus[0].utilization.roundToDecimals(2)
            this.cpu1utils = cpus[1].utilization.roundToDecimals(2)
            this.cpu2utils = cpus[2].utilization.roundToDecimals(2)
            this.cpu3utils = cpus[3].utilization.roundToDecimals(2)
            this.cpu4utils = cpus[4].utilization.roundToDecimals(2)
            this.cpu5utils = cpus[5].utilization.roundToDecimals(2)
            this.cpu6utils = cpus[6].utilization.roundToDecimals(2)
            this.cpu7utils = cpus[7].utilization.roundToDecimals(2)
        }*/

        return this
    }

    fun totalCpu(totalCpuUsage: Float?): Builder2 {
        totalCpuUsage ?: return this
        this.totalCpu = totalCpuUsage.roundToDecimals(2)
        return this
    }

    fun gpuCurFreq(gpuCurFreq: Float?): Builder2 {
        gpuCurFreq ?: return this
        this.gpuCurFreq = gpuCurFreq
        return this
    }

    fun gpuCurUtil(gpuCurUtil: Float?): Builder2 {
        gpuCurUtil ?: return this
        this.gpuCurUtil = gpuCurUtil
        return this
    }

    fun blEnabled(enabled: Int?): Builder2 {
        enabled ?: return this
        this.blEnabled = enabled
        return this
    }

    fun blConnectedNum(num: Int?): Builder2 {
        num ?: return this
        this.blConnectedNum = num
        return this
    }

    fun memCurAvailable(curAvailable: Float?): Builder2 {
        curAvailable ?: return this
        this.memCurAvailable = curAvailable
        return this
    }

    fun memAllInfo(memData: Array<Float>?): Builder2 {
        memData ?: return this

//        this.memFree = memData[0]
        this.memCurAvailable = memData[1]
        this.memActive = memData[2]
//        this.memInactive = memData[3]
        this.memDirty = memData[4]
        this.memAnonPages = memData[5]
        this.memMapped = memData[6]

        return this
    }

    fun build(): StatisticDoMain2 {
        return StatisticDoMain2(this)
    }

    fun buildArray(): ArrayList<Any> {
        return arrayListOf(
            curTimeMills, /*isSystemOn, *//*isScreenOn, */screenBrightness,
            isMusicOn, isPhoneRinging, isPhoneOffHook,
            isWifiNetwork, is2GNetwork, is3GNetwork, is4GNetwork, is5GNetwork, isOtherNetwork, isWifiApEnable, netWorkSpeed,
            cpu0, cpu1, cpu2, cpu3, cpu4, cpu5, cpu6, cpu7,
//            cpuTemp0, cpuTemp1, cpuTemp2, cpuTemp3, cpuTemp4, cpuTemp5, cpuTemp6, cpuTemp7,
            /*totalCpu,
            cpu0utils, cpu1utils, cpu2utils, cpu3utils, cpu4utils, cpu5utils, cpu6utils, cpu7utils,*/
            blEnabled,
            /*memFree,*/ memCurAvailable, memActive, /*memInactive,*/ memDirty, memAnonPages, memMapped,
//            gpuCurFreq, gpuCurUtil,
        )

    }

}