package com.jamgu.hwstatistics

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.timer.VATimer
import com.jamgu.hwstatistics.IOnDataEnough.Companion.THRESH_ONE_HOUR
import com.jamgu.hwstatistics.bluetooth.BluetoothManager
import com.jamgu.hwstatistics.brightness.BrightnessManager
import com.jamgu.hwstatistics.cpu.CPUInfoManager
import com.jamgu.hwstatistics.cpu.model.CPU
import com.jamgu.hwstatistics.mediastate.MediaStateManager
import com.jamgu.hwstatistics.memory.MemInfoManager
import com.jamgu.hwstatistics.network.NetWorkManager
import com.jamgu.hwstatistics.phonestate.PhoneStateManager
import com.jamgu.hwstatistics.system.SystemManager
import com.jamgu.hwstatistics.util.roundToDecimals
import com.permissionx.guolindev.PermissionX
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by jamgu on 2021/10/14
 */
class StatisticsLoader() : INeedPermission {

    private lateinit var weakContext: WeakReference<Context>

    companion object {
        private const val TAG = "StatisticsLoader"
        private const val TOTAL_LENGTH = 100000
    }

    private var mTimer: VATimer? = null

    private var uiCallback: ((String) -> Unit)? = null
    private val mData: ArrayList<ArrayList<Any>> = ArrayList()

    private var mOnDataEnough: IOnDataEnough? = null
    private var mDataNumThreshold: Int = THRESH_ONE_HOUR

    fun setOnDataEnoughListener(threshold: Int, onDataEnough: IOnDataEnough) {
        mDataNumThreshold = threshold
        mOnDataEnough = onDataEnough
    }

    fun init(ctx: Context, callback: ((String) -> Unit)?): StatisticsLoader {

        uiCallback = callback
        weakContext = WeakReference(ctx)
        mData.clear()
        val context = weakContext.get()
        PhoneStateManager.register(context)
        BrightnessManager.registerReceiver(context)
        SystemManager.registerSystemReceiver(context)
//        SensorsInfoManager.registerSensorListener(context)
        return this
    }

    @SuppressLint("SimpleDateFormat")
    private fun start() {
        if (mTimer == null) {
            mTimer = VATimer()
        }

        mData.clear()

        var lastTimeString = ""
        var dataTemp = ArrayList<Any>()
        var tempDataTimes = 1f
        mTimer?.run({
            // do something
            val currentTimeMillis = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            val curTimeString: String = sdf.format(Date(currentTimeMillis))

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
                    mData.add(dataTemp.divideBy(tempDataTimes))
                    if (mData.size >= mDataNumThreshold) {
                        mOnDataEnough?.onDataEnough()
                    }
//                    JLog.d(TAG, "data belong to: $lastTimeString, data_num = $tempDataTimes")
                }
                tempDataTimes = 1f
                dataTemp = newData
            }

            uiCallback?.invoke("TS: $curTimeString, TM: $currentTimeMillis")

            lastTimeString = curTimeString
//            JLog.d(TAG, "curVal = $it, curRepeat = ${mTimer?.getCurrentRepeatCount()}, info: | ${newData[3]}")

        }, 200)
    }

    private fun getDataWithTitle(curTimeString: String, currentTimeMillis: Long): ArrayList<Any> {
//        val screenOn = getScreenStatus()
        val screenBrightness = getScreenBrightness()
        val phoneState = getPhoneState()
//        val systemOnStatus = getSystemStatus()
        val musicState = getMusicState()
//        val musicVolume = getMusicVolume()
//        JLog.d(TAG, "musicVolume = $musicVolume")
        val networkType = getNetworkType()
        val netWorkSpeed = getNetWorkSpeed()
        val cpuInfo = getCpuInfo()
        val cpuTotalUsage = getCpuTotalUsage()

//        val gpu3DCurUtil = GPUManager.getGpuUtilization()
//        val gpu3DCurFreq = GPUManager.getGpuCurFreq()

        val memInfoFromFile = MemInfoManager.getMemInfoFromFile()


        // 蓝牙
        val bluetoothData = BluetoothManager.getBluetoothData()
        val blEnabled = if (bluetoothData?.enabled == true) 1 else 0
        val blConnectedNum = bluetoothData?.bondedDevices?.size ?: 0

//        GPUManager.getMaxCpuFreq()
//        GPUManager.getGpuUtilization()


        return Builder2().apply {
            curTimeMills(curTimeString)
//            screenOn(screenOn)
            screenBrightness(screenBrightness)
            if (phoneState.size == 2) {
                phoneRing(phoneState[0])
                phoneOffHook(phoneState[1])
            }
//            systemOn(systemOnStatus)
            musicOn(musicState)
            when(networkType) {
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
//            gpuCurFreq(gpu3DCurFreq)
//            gpuCurUtil(gpu3DCurUtil)
        }.buildArray()
    }


    fun stop() {
        mTimer?.stop()
    }

    fun isStarted(): Boolean {
        return mTimer?.isStarted() ?: false
    }

    fun startNonMainThread() {
        start()
    }

    fun release() {
        mTimer?.release()
        val context = weakContext.get()
        PhoneStateManager.unregister(context)
        BrightnessManager.unregisterReceiver(context)
        SystemManager.unregisterSystemReceiver(context)
//        SensorsInfoManager.unregisterSensorListener()
        mData.clear()
        weakContext.clear()
    }

    fun getRawData(): ArrayList<ArrayList<Any>> {
        return mData
    }

    fun getDataWithTitle(): ArrayList<ArrayList<Any>> {
        mData.add(
            0,
            arrayListOf(
                "cur_time_mills",
//                "system_on",
//                "screen_on",
                "screen_brightness",
                "music_on",
//                "music_volume",
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
//                "cpuTemp0",
//                "cpuTemp1",
//                "cpuTemp2",
//                "cpuTemp3",
//                "cpuTemp4",
//                "cpuTemp5",
//                "cpuTemp6",
//                "cpuTemp7",
//                "cpu_total_util",
//                "cpu0_util",
//                "cpu1_util",
//                "cpu2_util",
//                "cpu3_util",
//                "cpu4_util",
//                "cpu5_util",
//                "cpu6_util",
//                "cpu7_util",
                "bluetooth",
//                "mem_cur_free",
//                "mem_free",
                "mem_available",
                "mem_active",
//                "mem_inactive",
                "mem_dirty",
                "mem_anonPages",
                "mem_mapped",
//                "gpu_cur_freq",
//                "gpu_cur_util",
                "avg_p",
            )
        )
        return mData
    }

    fun clearData() {
        mData.clear()
    }

    fun requestedPermission(context: FragmentActivity?): Boolean {
        val permissions = ArrayList<String>().apply {
            addAll(permission())
            addAll(NetWorkManager.permission())
        }
        val notGrantedPermission = permissions.filterNot { PermissionX.isGranted(context, it) }
        if (notGrantedPermission.isEmpty()) {
            return true
        } else {
            requestPermission(context, notGrantedPermission)
            return false
        }
    }

    private fun requestPermission(context: FragmentActivity?, notGrantedPermission: List<String>) {
        PermissionX.init(context)
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
                        // do nothing
                    } else {
                        ThreadPool.runUITask {
                            Toast.makeText(context, "These permissions are denied: $deniedList", Toast.LENGTH_LONG)
                                    .show()
                        }
                    }
                }
    }

    /**
     * 获取系统亮度信息
     */
    private fun getScreenBrightness(): Int {
        return BrightnessManager.getBrightness(weakContext.get())
    }

    /**
     * 获取屏幕状态
     */
    private fun getScreenStatus(): Int {
        return BrightnessManager.getScreenStatus()
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
        return SystemManager.getSystemOnStatus(weakContext.get())
    }

    /**
     * 获取手机Music状态
     */
    private fun getMusicState(): Int {
        return MediaStateManager.getMusicState(weakContext.get())
    }

    /**
     * 获取手机音乐音量
     */
    private fun getMusicVolume(): Int {
        return MediaStateManager.getMusicVolume(weakContext.get())
    }

    /**
     * 获取网络类型
     */
    private fun getNetworkType(): Int {
        return NetWorkManager.getNetworkType(weakContext.get())
    }

    /**
     * 获取网速
     */
    private fun getNetWorkSpeed(): Float {
        return NetWorkManager.getNetWorkSpeed(weakContext.get())
    }

    /**
     * 获取cpu相关信息
     */
    private fun getCpuInfo(): ArrayList<CPU> {
        val cpuNumb = CPUInfoManager.getCpuCoresNumb()
        val cpus = ArrayList<CPU>()
//        val cpuUtilization = CPUInfoManager.getCpuUtilization(weakContext.get(), cpuNumb)
//        val cpuUsageBeforeO = TempCpu().getCpuUsageBeforeO(cpuNumb)
//        JLog.d(TAG, "cpu = $cpuUsageBeforeO")
//        val cpuUsage = CpuUtil.getCpuUsage()
//        JLog.d(TAG, "cpuUsage = $cpuUsage")
//        val cpuUsageBeforeO = TempCpu().getCpuUsageBeforeO(cpuNumb)

        val cpuUtils: List<Float>?
        val cpuInfo = CPUInfoManager.getCpuUtilization()
        cpuUtils = cpuInfo?.getPerCpuUtilisation()

        for (i in 0 until cpuNumb) {
//            val cpuMaxFreq = CPUInfoManager.getCpuMaxFreq(i)
//            val cpuMinFreq = CPUInfoManager.getCpuMinFreq(i)
            val cpuTemp = CPUInfoManager.getCpuTemp(i)
            val cpuMaxFreq = 0f
            val cpuMinFreq = 0f
            val cpuRunningFreq = CPUInfoManager.getCpuRunningFreq(i)
//            val cpuTemp = 0f
            if (cpuUtils != null && cpuUtils.isNotEmpty()) {
                val cpu = CPU(cpuMaxFreq, cpuMinFreq, cpuRunningFreq, cpuTemp, cpuUtils[i.coerceAtMost(cpuUtils.size)])
                cpus.add(cpu)
            } else {
                val cpu = CPU(cpuMaxFreq, cpuMinFreq, cpuRunningFreq, cpuTemp, 0f)
                cpus.add(cpu)
            }
        }

//        JLog.d(TAG, "cpu's number = ${cpus.size}")

        return cpus
    }

    private fun getCpuTotalUsage(): Float {
        return CPUInfoManager.getCpuUtilization()?.overallCpu ?: 0f
    }

    override fun permission(): Array<String> = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
}

/**
 * 传入newData，会将两者相加
 */
private fun ArrayList<Any>.plus(newData: ArrayList<Any>?): ArrayList<Any> {
    if (newData.isNullOrEmpty() || this.isNullOrEmpty()) return this

    this.forEachIndexed { i, it ->
        if (i == 0) return@forEachIndexed
        if (it is Number) {
            if (it is Float) {
                val newVal = it.plus(newData[i].toString().toFloat())
                this[i] = newVal
            } else if (it is Int) {
                val newVal = it.plus(newData[i].toString().toInt())
                this[i] = newVal
            }
        }
    }

    return this
}

/**
 * 传入分母，会将列表内各元素分别处于它
 */
private fun ArrayList<Any>.divideBy(divider: Float): ArrayList<Any> {
    if (this.isNullOrEmpty()) return this

    this.forEachIndexed { i, it ->
        if (i == 0) return@forEachIndexed

        if (it is Number) {
            if (it is Float) {
                val newVal = it / divider
                this[i] = newVal.roundToDecimals(2)
            } else if (it is Int) {
                val newVal = it / divider
                this[i] = newVal.roundToDecimals(2)
            }
        }
    }

    return this
}

interface IOnDataEnough {

    companion object {
        const val THRESH_ONE_HOUR = 3600
        const val THRESH_HALF_HOUR = 1800
    }

    fun onDataEnough()
}