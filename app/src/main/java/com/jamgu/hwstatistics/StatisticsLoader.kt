package com.jamgu.hwstatistics

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.animation.Animation.INFINITE
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.jamgu.hwstatistics.brightness.BrightnessManager
import com.jamgu.hwstatistics.cpu.CPU
import com.jamgu.hwstatistics.cpu.CPUInfoManager
import com.jamgu.hwstatistics.gpu.GPUManager
import com.jamgu.hwstatistics.mediastate.MediaStateManager
import com.jamgu.hwstatistics.network.NetWorkManager
import com.jamgu.hwstatistics.phonestate.PhoneStateManager
import com.jamgu.hwstatistics.system.SystemManager
import com.jamgu.hwstatistics.thread.ThreadPool
import com.jamgu.hwstatistics.timer.RoughTimer
import com.jamgu.hwstatistics.util.roundToDecimals
import com.permissionx.guolindev.PermissionX
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Created by jamgu on 2021/10/14
 */
class StatisticsLoader : INeedPermission {

    private lateinit var weakContext: WeakReference<Context>

    companion object {
        private const val TAG = "StatisticsLoader"
        private const val TOTAL_LENGTH = 100000
    }

    private var mTimer: ValueAnimator? = null
    private var mRoughTimer: RoughTimer? = null
    private var mHandlerThread: HandlerThread? = null

    private var uiCallback: ((String) -> Unit)? = null
    private val mData: ArrayList<ArrayList<Any>> = ArrayList()

    fun init(ctx: Context, callback: ((String) -> Unit)?): StatisticsLoader {
        mHandlerThread = HandlerThread("Statistics Thread")
        mHandlerThread?.start()

        uiCallback = callback
        weakContext = WeakReference(ctx)
        mData.clear()
        val context = weakContext.get()
        PhoneStateManager.register(context)
        BrightnessManager.registerReceiver(context)
        SystemManager.registerSystemReceiver(context)
        return this
    }

    @SuppressLint("SimpleDateFormat")
    private fun start() {
        if (mTimer == null) {
            mTimer = ValueAnimator.ofInt(0, TOTAL_LENGTH)
        }

        mData.clear()

        var lastVal = -1
        var currentRepeatCount = 0
        var lastTimeMills = 0L
        var lastTimeString = ""
        var dataTemp = ArrayList<Any>()
        var tempDataTimes = 1f
        mTimer?.apply {
            addUpdateListener {
                val curVal = it.animatedValue
                // 重置
                if (curVal is Int && lastVal - curVal >= 10000) {
                    lastVal = -1
                    currentRepeatCount++
                }

//                Log.d(TAG, "curVal = $curVal, lastVal = $lastVal")
                if (curVal is Int && (curVal - 200) >= lastVal) {
                    if (curVal == 10) currentRepeatCount++

                    // do something
                    val currentTimeMillis = System.currentTimeMillis()
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                    val curTimeString: String = sdf.format(Date(currentTimeMillis))

                    // 无限循环时，下一次开启计时的时间，与上一次结束的时间之间的间隔很短，
                    // 会多一次的数据，所以去重
//                    if (curTimeString == lastTimeString) {
//                        return@addUpdateListener
//                    }
//
//                    val data = getData(curTimeString, currentTimeMillis)
//
//                    mData.add(data)

                    val newData = getData(curTimeString, currentTimeMillis)

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
                            Log.d(TAG, "curTimeString: $curTimeString, data_num = $tempDataTimes")
                        }
                        tempDataTimes = 1f
                        dataTemp = newData
                    }

//                    uiCallback?.invoke("TS: $curTimeString, TM: $currentTimeMillis")

                    lastTimeString = curTimeString
                    lastTimeMills = currentTimeMillis
                    Log.d(TAG, "curVal = $curVal, curRepeat = $currentRepeatCount, info: | ${newData[3]}")
                    lastVal = curVal
                }
            }

            interpolator = LinearInterpolator()
            duration = 100 * 1000L
            repeatCount = INFINITE
            start()
        }
    }

    private fun getData(curTimeString: String, currentTimeMillis: Long): ArrayList<Any> {
        val screenOn = getScreenStatus()
        val screenBrightness = getScreenBrightness()
        val phoneState = getPhoneState()
        val systemOnStatus = getSystemStatus()
        val musicState = getMusicState()
        val networkType = getNetworkType()
        val netWorkSpeed = getNetWorkSpeed()
        val cpuInfo = getCpuInfo()
        val cpuTotalUsage = getCpuTotalUsage()

        val gpu3DCurUtil = GPUManager.getGpuUtilization()
        val gpu3DCurFreq = GPUManager.getGpuCurFreq()
        Log.d(TAG, gpu3DCurUtil.toString())


        return Builder2().apply {
            curTimeMills(curTimeString)
            screenOn(screenOn)
            screenBrightness(screenBrightness)
            if (phoneState.size == 2) {
                phoneRing(phoneState[0])
                phoneOffHook(phoneState[1])
            }
            systemOn(systemOnStatus)
            musicOn(musicState)
            if (networkType >= 0) {
                // wifi
                if (networkType == 1) {
                    wifiNetwork(1)
                    mobileNetwork(0)
                } else { // mobile
                    wifiNetwork(0)
                    mobileNetwork(1)
                }
            }
            networkSpeed(netWorkSpeed)
            totalCpu(cpuTotalUsage)
            cpus(cpuInfo)
            gpuCurFreq(gpu3DCurFreq)
            gpuCurUtil(gpu3DCurUtil)
        }.buildArray()
    }

    @SuppressLint("SimpleDateFormat")
    private fun startRoughly() {
        val handlerThread = mHandlerThread ?: return

        if (mRoughTimer == null) {
            mRoughTimer = RoughTimer(handlerThread.looper)
        }

        mData.clear()

        var lastTimeString = ""
        var dataTemp = ArrayList<Any>()
        var tempDataTimes = 1f
        mRoughTimer?.run({
            // do something
            val currentTimeMillis = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            val curTimeString: String = sdf.format(Date(currentTimeMillis))

            val newData = getData(curTimeString, currentTimeMillis)

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
                    Log.d(TAG, "curTimeString: $curTimeString, data_num = $tempDataTimes")
                }
                tempDataTimes = 1f
                dataTemp = newData
            }

            lastTimeString = curTimeString
//            uiCallback?.invoke("TS: $curTimeString, TM: $currentTimeMillis")

        }, 200)

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


    fun stop() {
        if (mTimer?.isRunning == true || mRoughTimer?.isStarted() == true) {
            destroyTimer()
        }
    }

    fun isStarted(): Boolean {
        return mTimer?.isStarted == true || mRoughTimer?.isStarted() == true
    }

    fun startNonMainThread() {
        mHandlerThread?.let {
            Handler(it.looper).post {
                start()
//                startRoughly()
            }
        }
    }

    fun release() {
        destroyTimer()
        val context = weakContext.get()
        PhoneStateManager.unregister(context)
        BrightnessManager.unregisterReceiver(context)
        SystemManager.unregisterSystemReceiver(context)
        mData.clear()
        weakContext.clear()
        mHandlerThread?.quitSafely()
    }

    fun getData(): ArrayList<ArrayList<Any>> {
        mData.add(
            0,
            arrayListOf(
                "cur_time_mills",
                "system_on",
                "screen_on",
                "screen_brightness",
                "music_on",
                "phone_ring",
                "phone_off_hook",
                "wifi_network",
                "mobile_network",
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
                "cpu_total_util",
                "cpu0_util",
                "cpu1_util",
                "cpu2_util",
                "cpu3_util",
                "cpu4_util",
                "cpu5_util",
                "cpu6_util",
                "cpu7_util",
                "avg_p",
            )
        )
        return mData
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
                    Log.d("NetWorkManager", "permission granted!")
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

    private fun destroyTimer() {
        mTimer?.let {
            if (it.isRunning)
                it.cancel()
            it.removeAllUpdateListeners()
        }
        mTimer = null

        mRoughTimer?.let {
            if (it.isStarted())
                it.close()
        }
        mRoughTimer = null
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
//        Log.d(TAG, "cpu = $cpuUsageBeforeO")
//        val cpuUsage = CpuUtil.getCpuUsage()
//        Log.d(TAG, "cpuUsage = $cpuUsage")
//        val cpuUsageBeforeO = TempCpu().getCpuUsageBeforeO(cpuNumb)

        var cpuUtils: List<Float>? = null
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            val cpuInfo = CPUInfoManager.getCpuUtilizationFromFile()
            cpuUtils = cpuInfo?.getPerCpuUtilisation()
        }

        for (i in 0 until cpuNumb) {
//            val cpuMaxFreq = CPUInfoManager.getCpuMaxFreq(i)
//            val cpuMinFreq = CPUInfoManager.getCpuMinFreq(i)
//            val cpuTemp = CPUInfoManager.getCpuTemp(i)
            val cpuMaxFreq = 0f
            val cpuMinFreq = 0f
            val cpuRunningFreq = CPUInfoManager.getCpuRunningFreq(i)
            val cpuTemp = 0f
            val cpu = CPU(cpuMaxFreq, cpuMinFreq, cpuRunningFreq, cpuTemp, cpuUtils?.get(i) ?: 0f)
            cpus.add(cpu)
        }
        return cpus
    }

    private fun getCpuTotalUsage(): Float {
        return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            CPUInfoManager.getCpuUtilizationFromFile()?.overallCpu ?: 0f
        } else 0f
    }

    override fun permission(): Array<String> = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
}