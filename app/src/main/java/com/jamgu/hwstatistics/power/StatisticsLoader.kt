package com.jamgu.hwstatistics.power

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.timer.VATimer
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.power.mobiledata.bluetooth.BluetoothManager
import com.jamgu.hwstatistics.power.mobiledata.brightness.BrightnessManager
import com.jamgu.hwstatistics.power.mobiledata.cpu.CPUInfoManager
import com.jamgu.hwstatistics.power.mobiledata.cpu.model.CPU
import com.jamgu.hwstatistics.power.mobiledata.mediastate.MediaStateManager
import com.jamgu.hwstatistics.power.mobiledata.memory.MemInfoManager
import com.jamgu.hwstatistics.power.mobiledata.network.NetWorkManager
import com.jamgu.hwstatistics.power.mobiledata.phonestate.PhoneStateManager
import com.jamgu.hwstatistics.power.mobiledata.system.SystemManager
import com.jamgu.hwstatistics.util.divideBy
import com.jamgu.hwstatistics.util.plus
import com.jamgu.hwstatistics.util.timeStamp2DateString
import com.permissionx.guolindev.PermissionX
import java.util.*

/**
 * Created by jamgu on 2021/10/14
 */
class StatisticsLoader(private val mContext: FragmentActivity) : INeedPermission {

    companion object {
        private const val TAG = "StatisticsLoader"
        private const val TOTAL_LENGTH = 100000
    }

    private var mTimer: VATimer? = null
    private var uiCallback: ((String) -> Unit)? = null
    private val mPowerData: MutableList<ArrayList<Any>> = Collections.synchronizedList(ArrayList<ArrayList<Any>>())

    private var mOnDataEnough: IOnDataEnough? = null
    private var mDataNumThreshold: IOnDataEnough.ThreshLength = IOnDataEnough.ThreshLength.THRESH_FOR_ERROR

    /**
     * 数据查询间隔 in ms，越大采样率越低，最大不超过 1000 ms，默认为 200 ms，既每秒采样 5 次
     */
    private var mDataQueryInternal: Long = 200L

    /**
     * 设置数据采样间隔，in ms，采样间隔超过 1000ms 会被置为 1000 ms
     */
    fun setOnDataEnoughListener(threshold: IOnDataEnough.ThreshLength, onDataEnough: IOnDataEnough) {
        mDataNumThreshold = threshold
        mOnDataEnough = onDataEnough
    }

    fun initOnCreate(callback: ((String) -> Unit)?): StatisticsLoader {
        uiCallback = callback
        mPowerData.clear()
        PhoneStateManager.register(mContext)
        BrightnessManager.registerReceiver(mContext)
        SystemManager.registerSystemReceiver(mContext)
        return this
    }

    fun startInInternal(internal: Long) {
        if (mContext is FragmentActivity && requestedPermission()) {
            stop()
            start(internal)
        }
    }

    private fun start(internal: Long = 200) {
        if (mTimer == null) {
            mTimer = VATimer()
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
                    if (mPowerData.size >= mDataNumThreshold.length) {
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
        val netWorkSpeed = getNetWorkSpeed()
        val cpuInfo = getCpuInfo()
        val cpuTotalUsage = getCpuTotalUsage()

        val memInfoFromFile = MemInfoManager.getMemInfoFromFile()

        // 蓝牙
        val bluetoothData = BluetoothManager.getBluetoothData()
        val blEnabled = if (bluetoothData?.enabled == true) 1 else 0
        val blConnectedNum = bluetoothData?.bondedDevices?.size ?: 0

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
            networkSpeed(netWorkSpeed)
            totalCpu(cpuTotalUsage)
            cpus(cpuInfo)
            blEnabled(blEnabled)
            blConnectedNum(blConnectedNum)
            memAllInfo(memInfoFromFile)
        }.buildArray()
    }

    fun stop() {
        mTimer?.stop()
    }

    fun isStarted(): Boolean {
        return mTimer?.isStarted() ?: false
    }

    fun startNonMainThread() {
        if (requestedPermission()) {
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
        mPowerData.clear()
    }

    fun getRawData(): ArrayList<ArrayList<Any>> {
        return ArrayList(mPowerData)
    }

    fun getDataWithTitle(): ArrayList<ArrayList<Any>> {
        mPowerData.add(
            0,
            arrayListOf(
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
                "avg_p",
            )
        )
        return ArrayList(mPowerData)
    }

    fun clearData() {
        mPowerData.clear()
    }

    fun requestedPermission(): Boolean {
        val permissions = ArrayList<String>().apply {
            addAll(permission())
            addAll(NetWorkManager.permission())
            addAll(BluetoothManager.permission())
        }
        val notGrantedPermission = permissions.filterNot { PermissionX.isGranted(mContext, it) }
        return if (notGrantedPermission.isEmpty()) {
            true
        } else {
            requestPermission(notGrantedPermission)
            false
        }
    }

    private fun requestPermission(notGrantedPermission: List<String>) {
        PermissionX.init(mContext)
            .permissions(notGrantedPermission)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList, "获取网络类型需要申请读取手机状态权限",
                    "好的", "拒绝"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList, "You need to allow necessary permissions in Settings manually",
                    "OK", "Cancel"
                )
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    start()
                } else {
                    ThreadPool.runUITask {
                        Toast.makeText(
                            mContext,
                            "These permissions are denied: $deniedList",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }
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
            val cpuTemp = CPUInfoManager.getCpuTemp(i)
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
        THRESH_FIVE_MINS(60),
        THRESH_FOR_CHARGE(4),
    }

    fun onDataEnough()
}