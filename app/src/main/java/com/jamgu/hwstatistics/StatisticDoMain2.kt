package com.jamgu.hwstatistics

import com.jamgu.hwstatistics.cpu.CPU
import com.jamgu.hwstatistics.cpu.CPUInfoManager
import com.jamgu.hwstatistics.util.roundToDecimals

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

    fun mobileNetwork(): Int = builder.isMobileNetwork

    fun networkSpeed(): Float = builder.netWorkSpeed

    fun cpu0(): Float = builder.cpu0

    fun cpu1(): Float = builder.cpu1

    fun cpu2(): Float = builder.cpu2

    fun cpu3(): Float = builder.cpu3

    fun cpu4(): Float = builder.cpu4

    fun cpu5(): Float = builder.cpu5

    fun cpu6(): Float = builder.cpu6

    fun cpu7(): Float = builder.cpu7

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
    var isMobileNetwork: Int = 0

    // network speed kb
    var netWorkSpeed: Float = 0.0f

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

    fun curTimeMills(curTimeMills: String): Builder2 {
        this.curTimeMills = curTimeMills
        return this
    }

    fun systemOn(isOn: Int): Builder2 {
        this.isSystemOn = isOn
        return this
    }

    fun screenBrightness(brightness: Int): Builder2 {
        this.screenBrightness = brightness
        return this
    }

    fun screenOn(isOn: Int): Builder2 {
        this.isScreenOn = isOn
        return this
    }

    fun phoneRing(isRing: Int): Builder2 {
        this.isPhoneRinging = isRing
        return this
    }

    fun phoneOffHook(isOffHook: Int): Builder2 {
        this.isPhoneOffHook = isOffHook
        return this
    }

    fun musicOn(isMusicOn: Int): Builder2 {
        this.isMusicOn = isMusicOn
        return this
    }

    fun wifiNetwork(isWifi: Int): Builder2 {
        this.isWifiNetwork = isWifi
        return this
    }

    fun mobileNetwork(isMobile: Int): Builder2 {
        this.isMobileNetwork = isMobile
        return this
    }

    fun networkSpeed(speed: Float): Builder2 {
        this.netWorkSpeed = speed
        return this
    }

    fun cpus(cpus: ArrayList<CPU>): Builder2 {
        if (cpus.isNullOrEmpty() || cpus.size != 8) {
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

        this.cpu0 = cpus[0].curFreq.roundToDecimals(2)
        this.cpu1 = cpus[1].curFreq.roundToDecimals(2)
        this.cpu2 = cpus[2].curFreq.roundToDecimals(2)
        this.cpu3 = cpus[3].curFreq.roundToDecimals(2)
        this.cpu4 = cpus[4].curFreq.roundToDecimals(2)
        this.cpu5 = cpus[5].curFreq.roundToDecimals(2)
        this.cpu6 = cpus[6].curFreq.roundToDecimals(2)
        this.cpu7 = cpus[7].curFreq.roundToDecimals(2)

        this.cpuTemp0 = cpus[0].temp.roundToDecimals(2)
        this.cpuTemp1 = cpus[1].temp.roundToDecimals(2)
        this.cpuTemp2 = cpus[2].temp.roundToDecimals(2)
        this.cpuTemp3 = cpus[3].temp.roundToDecimals(2)
        this.cpuTemp4 = cpus[4].temp.roundToDecimals(2)
        this.cpuTemp5 = cpus[5].temp.roundToDecimals(2)
        this.cpuTemp6 = cpus[6].temp.roundToDecimals(2)
        this.cpuTemp7 = cpus[7].temp.roundToDecimals(2)

        if (this.totalCpu != 0f) {
            this.cpu0utils = cpus[0].utilization.roundToDecimals(2)
            this.cpu1utils = cpus[1].utilization.roundToDecimals(2)
            this.cpu2utils = cpus[2].utilization.roundToDecimals(2)
            this.cpu3utils = cpus[3].utilization.roundToDecimals(2)
            this.cpu4utils = cpus[4].utilization.roundToDecimals(2)
            this.cpu5utils = cpus[5].utilization.roundToDecimals(2)
            this.cpu6utils = cpus[6].utilization.roundToDecimals(2)
            this.cpu7utils = cpus[7].utilization.roundToDecimals(2)
        }

        return this
    }

    fun totalCpu(totalCpuUsage: Float): Builder2 {
        this.totalCpu = totalCpuUsage.roundToDecimals(2)
        return this
    }

    fun build(): StatisticDoMain2 {
        return StatisticDoMain2(this)
    }


    fun buildArray(): ArrayList<Any> {
        return arrayListOf(
            curTimeMills, isSystemOn, isScreenOn, screenBrightness,
            isMusicOn, isPhoneRinging, isPhoneOffHook,
            isWifiNetwork, isMobileNetwork, netWorkSpeed,
            cpu0, cpu1, cpu2, cpu3, cpu4, cpu5, cpu6, cpu7,
//            cpuTemp0, cpuTemp1, cpuTemp2, cpuTemp3, cpuTemp4, cpuTemp5, cpuTemp6, cpuTemp7,
            totalCpu,
            cpu0utils, cpu1utils, cpu2utils, cpu3utils, cpu4utils, cpu5utils, cpu6utils, cpu7utils
        )
    }

}