package com.jamgu.hwstatistics.power

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.jamgu.common.util.timer.VATimer
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.hwstatistics.power.mobiledata.battery.JBatteryManager
import com.jamgu.hwstatistics.power.mobiledata.bluetooth.BLEManager
import com.jamgu.hwstatistics.power.mobiledata.brightness.BrightnessManager
import com.jamgu.hwstatistics.power.mobiledata.cpu.CPUInfoManager
import com.jamgu.hwstatistics.power.mobiledata.cpu.model.CPU
import com.jamgu.hwstatistics.power.mobiledata.gps.JGpsManager
import com.jamgu.hwstatistics.power.mobiledata.mediastate.MediaStateManager
import com.jamgu.hwstatistics.power.mobiledata.memory.MemInfoManager
import com.jamgu.hwstatistics.power.mobiledata.network.NetWorkManager
import com.jamgu.hwstatistics.power.mobiledata.phonestate.PhoneStateManager
import com.jamgu.hwstatistics.power.mobiledata.system.SystemManager
import com.jamgu.hwstatistics.power.permission.PermissionRequester
import com.jamgu.hwstatistics.util.divideBy
import com.jamgu.hwstatistics.util.plus
import com.jamgu.hwstatistics.util.timeStamp2DateString
import java.util.*

/**
 * Created by jamgu on 2021/10/14
 */
class StatisticsLoader(private val mContext: Context) : INeedPermission {

    companion object {
        private const val TAG = "StatisticsLoader"
        private const val TOTAL_LENGTH = 100000
    }

    private var mTimer: VATimer? = null
    private var uiCallback: ((String) -> Unit)? = null
    private val mPowerData: MutableList<ArrayList<Any>> = Collections.synchronizedList(ArrayList())

    private var mOnDataEnough: IOnDataEnough? = null
    private var mDataNumThreshold: Long = IOnDataEnough.ThreshLength.THRESH_ONE_MIN.length

    private val mPermissionRequester = PermissionRequester(mContext)

    /**
     * 数据查询间隔 in ms，越大采样率越低，最大不超过 1000 ms，默认为 200 ms，既每秒采样 5 次
     */
    private var mDataQueryInternal: Long = 200L
    private var isRegister = false

    fun getDataNumThreshold(): Long {
        return mDataNumThreshold
    }

    fun setOnDataEnoughListener(threshold: Long, onDataEnough: IOnDataEnough) {
        mDataNumThreshold = threshold
        mOnDataEnough = onDataEnough
    }

    private fun register() {
        PhoneStateManager.register(mContext)
        BrightnessManager.registerReceiver(mContext)
        SystemManager.registerSystemReceiver(mContext)
        isRegister = true
    }

    fun initOnCreate(callback: ((String) -> Unit)?): StatisticsLoader {
        uiCallback = callback
        mPowerData.clear()
        if (!isRegister) {
            register()
        }
        return this
    }

    fun startInInternal(internal: Long) {
        if (mContext is FragmentActivity && mPermissionRequester.requestedPermission()) {
            stop()
            start(internal)
        }
    }

    private fun start(internal: Long = 200) {
        if (mTimer == null) {
            mTimer = VATimer("PowerDataLoader").apply {
                setUncaughtExceptionHandler { t, e ->
                    DataSaver.addInfoTracker(
                        TAG,
                        "uncaughtException: threadName#${t.name}, e = ${e.stackTraceToString()}"
                    )
                }
            }
        }

        var lastTimeString = ""
        var dataTemp = ArrayList<Any>()
        var tempDataTimes = 1f
        mTimer?.run({ executeTimes ->
            // do something
            val currentTimeMillis = System.currentTimeMillis()
            val curTimeString: String = currentTimeMillis.timeStamp2DateString()

            val newData = getDataWithTitle(curTimeString, currentTimeMillis)

            // 先缓存
            if (curTimeString == lastTimeString) {
                // 还没数据，直接赋值
                if (dataTemp.isEmpty()) {
                    dataTemp = newData
                    tempDataTimes = 1f
                } else {    // 有数据了，叠加
                    dataTemp.plus(newData)
                    tempDataTimes += 1f
                }
            } else {
                if (dataTemp.isNotEmpty()) {
                    mPowerData.add(dataTemp.divideBy(tempDataTimes))
                    if (mPowerData.size >= mDataNumThreshold) {
                        mOnDataEnough?.onDataEnough()
                    }
                }
                tempDataTimes = 1f
                dataTemp = newData
            }

            uiCallback?.invoke("TS: $curTimeString, TM: $currentTimeMillis")

            lastTimeString = curTimeString
//            JLog.d(TAG, "curVal = $it, curRepeat = ${mTimer?.getCurrentRepeatCount()}, info: | ${newData[3]}")

        }, internal.toInt())
    }

    private fun getDataWithTitle(curTimeString: String, currentTimeMillis: Long): ArrayList<Any> {
        val screenBrightness = getScreenBrightness()
        val phoneState = getPhoneState()
        val musicState = getMusicState()
        val networkType = getNetworkType()
        val wifiApOpen = NetWorkManager.isWifiApEnable(mContext)
        val netWorkSpeed = getNetWorkSpeed()
        val cpuInfo = getCpuInfo()
        val cpuTotalUsage = getCpuTotalUsage()

        val memInfoFromFile = MemInfoManager.getMemInfoFromFile()

        // 蓝牙
        val bluetoothData = BLEManager.getBluetoothData()
        val blEnabled = if (bluetoothData?.enabled == true) 1 else 0
        val blConnectedNum = bluetoothData?.bondedDevices?.size ?: 0

        val batteryVoltage = JBatteryManager.getBatteryVoltage(mContext)
        val batteryCharging = JBatteryManager.isBatteryCharging(mContext)
        val batteryCurrent = getBatteryCurrent()

        val gpsEnable = JGpsManager.isGpsEnable(mContext)

        return Builder2().apply {
            curTimeMills(curTimeString)
            screenBrightness(screenBrightness)
            if (phoneState.size == 2) {
                phoneRing(phoneState[0])
                phoneOffHook(phoneState[1])
            }
            musicOn(musicState)
            when (networkType) {
                0 -> {
                    isOtherNetwork(1)
                }
                1 -> {
                    isWifiNetwork(1)
                }
                2 -> {
                    is2GNetwork(1)
                }
                3 -> {
                    is3GNetwork(1)
                }
                4 -> {
                    is4GNetwork(1)
                }
                5 -> {
                    is5GNetwork(1)
                }
            }
            isWifiApEnable(wifiApOpen)
            networkSpeed(netWorkSpeed)
            totalCpu(cpuTotalUsage)
            cpus(cpuInfo)
            blEnabled(blEnabled)
            blConnectedNum(blConnectedNum)
            memAllInfo(memInfoFromFile)
            gpsEnable(gpsEnable)
            batteryVoltage(batteryVoltage)
            batteryCharging(batteryCharging)
            batteryCurrent(batteryCurrent)
        }.buildArray()
    }

    fun stop() {
        mTimer?.stop()
    }

    fun isStarted(): Boolean {
        return mTimer?.isStarted() ?: false
    }

    fun startNonMainThread() {
        if (mPermissionRequester.isPermissionAllGranted()) {
            mPowerData.clear()
            start()
        } else {
            Toast.makeText(
                mContext,
                mContext.getString(R.string.permission_not_allowed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun release() {
        mTimer?.release()
        PhoneStateManager.unregister(mContext)
        BrightnessManager.unregisterReceiver(mContext)
        SystemManager.unregisterSystemReceiver(mContext)
        isRegister = false
        mPowerData.clear()
    }

    fun getRawData(): ArrayList<ArrayList<Any>> {
        return ArrayList(mPowerData)
    }

    fun getDataWithTitle(): ArrayList<ArrayList<Any>> {
        return ArrayList(mPowerData).apply {
            add(
                0, arrayListOf(
                    "cur_time_mills",
                    "screen_brightness",
                    "music_on",
                    "phone_ring",
                    "phone_off_hook",
                    "wifi_network",
                    "2g_network",
                    "3g_network",
                    "4g_network",
                    "5g_network",
                    "other_network",
                    "is_wifi_enable",
                    "network_speed",
                    "cpu0",
                    "cpu1",
                    "cpu2",
                    "cpu3",
                    "cpu4",
                    "cpu5",
                    "cpu6",
                    "cpu7",
                    "bluetooth",
                    "mem_available",
                    "mem_active",
                    "mem_dirty",
                    "mem_anonPages",
                    "mem_mapped",
                    "gps_enable",
                    "battery_charging",
                    "battery_voltage",
                    "battery_current",
                    "avg_p",
                )
            )
        }
    }

    fun clearData() {
        mPowerData.clear()
    }

    /**
     * 获取屏幕状态
     */
    private fun getScreenStatus(): Int {
        return BrightnessManager.getScreenStatus()
    }

    /**
     * 获取系统亮度信息
     */
    private fun getScreenBrightness(): Int {
        return BrightnessManager.getBrightness(mContext)
    }

    /**
     * 获取手机PhoneCall状态
     * @return Array<Int>: 2元数组，idx0 = ringing_state, idx1 = off_hook_state
     */
    private fun getPhoneState(): Array<Int> {
        return PhoneStateManager.getPhoneState()
    }

    /**
     * 获取system_on信息
     */
    private fun getSystemStatus(): Int {
        return SystemManager.getSystemOnStatus(mContext)
    }

    /**
     * 获取手机Music状态
     */
    private fun getMusicState(): Int {
        return MediaStateManager.getMusicState(mContext)
    }

    /**
     * 获取手机音乐音量
     */
    private fun getMusicVolume(): Int {
        return MediaStateManager.getMusicVolume(mContext)
    }

    /**
     * 获取网络类型
     */
    private fun getNetworkType(): Int {
        return NetWorkManager.getNetworkType(mContext)
    }

    /**
     * 获取充电状态
     */
    private fun getBatteryStatus(): Int {
        return JBatteryManager.getBatteryStatus(mContext)
    }

    private fun getBatteryNum(): Int {
        return JBatteryManager.getBatteryScale(mContext)
    }

    private fun getBatteryCurrent(): Long {
        return JBatteryManager.getBatteryCurrent(mContext)
    }

    /**
     * 获取网速
     */
    private fun getNetWorkSpeed(): Float {
        return NetWorkManager.getNetWorkSpeed(mContext)
    }

    /**
     * 获取cpu相关信息
     */
    private fun getCpuInfo(): ArrayList<CPU> {
        val cpuNumb = CPUInfoManager.getCpuCoresNumb()
        val cpus = ArrayList<CPU>()

        val cpuUtils: List<Float>?
        val cpuInfo = CPUInfoManager.getCpuUtilization()
        cpuUtils = cpuInfo?.getPerCpuUtilisation()

        for (i in 0 until cpuNumb) {
//            val cpuTemp = CPUInfoManager.getCpuTemp(i)
            val cpuTemp = 0f
            val cpuMaxFreq = 0f
            val cpuMinFreq = 0f
            val cpuRunningFreq = CPUInfoManager.getCpuRunningFreq(i)
            if (cpuUtils != null && cpuUtils.isNotEmpty()) {
                val cpu = CPU(
                    cpuMaxFreq,
                    cpuMinFreq,
                    cpuRunningFreq,
                    cpuTemp,
                    cpuUtils[i.coerceAtMost(cpuUtils.size)]
                )
                cpus.add(cpu)
            } else {
                val cpu = CPU(cpuMaxFreq, cpuMinFreq, cpuRunningFreq, cpuTemp, 0f)
                cpus.add(cpu)
            }
        }

        return cpus
    }

    private fun getCpuTotalUsage(): Float {
        return CPUInfoManager.getCpuUtilization()?.overallCpu ?: 0f
    }

    override fun permission(): Array<String> =
        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

//    fun onCreate() {
//        mUsageLoader.onCreate()
//    }
//
//    fun onDestroy() {
//        mUsageLoader.onDestroy()
//    }
}

interface IOnDataEnough {

    enum class ThreshLength(val length: Long) {
        THRESH_ONE_HOUR(3600),
        THRESH_HALF_HOUR(1800),
        THRESH_FOR_TRACKER(10),
        THRESH_FOR_ERROR(1),
        THRESH_THREE_MINS(180),
        THRESH_ONE_MIN(60),
        THRESH_FOR_CHARGE(4),
    }

    fun onDataEnough()
}