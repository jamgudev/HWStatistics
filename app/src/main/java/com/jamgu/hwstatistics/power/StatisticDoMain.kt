package com.jamgu.hwstatistics.power

import com.jamgu.hwstatistics.power.mobiledata.cpu.model.CPU
import com.jamgu.hwstatistics.util.roundToDecimals

/**
 * Created by jamgu on 2021/11/03
 *
 * 用于存储所有收集的信息
 */
@Deprecated("use {@link StatisticDoMain2} instead")
class StatisticDoMain {
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

    // cpu0 频率x温度
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
}

class Builder {

    private val doMain: StatisticDoMain = StatisticDoMain()

    fun curTimeMills(curTimeMills: String): Builder {
        this.doMain.curTimeMills = curTimeMills
        return this
    }

    fun systemOn(isOn: Int): Builder {
        this.doMain.isSystemOn = isOn
        return this
    }

    fun screenBrightness(brightness: Int): Builder {
        this.doMain.screenBrightness = brightness
        return this
    }

    fun screenOn(isOn: Int): Builder {
        this.doMain.isScreenOn = isOn
        return this
    }

    fun phoneRing(isRing: Int): Builder {
        this.doMain.isPhoneRinging = isRing
        return this
    }

    fun phoneOffHook(isOffHook: Int): Builder {
        this.doMain.isPhoneOffHook = isOffHook
        return this
    }

    fun musicOn(isMusicOn: Int): Builder {
        this.doMain.isMusicOn = isMusicOn
        return this
    }

    fun wifiNetwork(isWifi: Int): Builder {
        this.doMain.isWifiNetwork = isWifi
        return this
    }

    fun mobileNetwork(isMobile: Int): Builder {
        this.doMain.isMobileNetwork = isMobile
        return this
    }

    fun networkSpeed(speed: Float): Builder {
        this.doMain.netWorkSpeed = speed.roundToDecimals(2)
        return this
    }

    fun cpus(cpus: ArrayList<CPU>): Builder {
        if (cpus.isNullOrEmpty() || cpus.size != 8) {
            return this
        }

        this.doMain.cpu0 = (cpus[0].curFreq * cpus[0].temp).roundToDecimals(2)
        this.doMain.cpu1 = (cpus[1].curFreq * cpus[1].temp).roundToDecimals(2)
        this.doMain.cpu2 = (cpus[2].curFreq * cpus[2].temp).roundToDecimals(2)
        this.doMain.cpu3 = (cpus[3].curFreq * cpus[3].temp).roundToDecimals(2)
        this.doMain.cpu4 = (cpus[4].curFreq * cpus[4].temp).roundToDecimals(2)
        this.doMain.cpu5 = (cpus[5].curFreq * cpus[5].temp).roundToDecimals(2)
        this.doMain.cpu6 = (cpus[6].curFreq * cpus[6].temp).roundToDecimals(2)
        this.doMain.cpu7 = (cpus[7].curFreq * cpus[7].temp).roundToDecimals(2)

        return this
    }

    fun build(): StatisticDoMain {
        return doMain
    }

    fun buildArray(): ArrayList<Any> {
        return arrayListOf(
            doMain.curTimeMills, doMain.isSystemOn, doMain.isScreenOn, doMain.screenBrightness,
            doMain.isMusicOn, doMain.isPhoneRinging, doMain.isPhoneOffHook,
            doMain.isWifiNetwork, doMain.isMobileNetwork, doMain.netWorkSpeed,
            doMain.cpu0, doMain.cpu1, doMain.cpu2, doMain.cpu3, doMain.cpu4, doMain.cpu5, doMain.cpu6, doMain.cpu7,
        )
    }

}