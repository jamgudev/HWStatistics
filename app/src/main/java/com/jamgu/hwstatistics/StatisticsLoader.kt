package com.jamgu.hwstatistics

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.jamgu.hwstatistics.cpu.CpuUtil
import com.jamgu.hwstatistics.cpu.other.CpuUtilisationReader
import com.jamgu.hwstatistics.mediastate.MediaStateManager
import com.jamgu.hwstatistics.network.NetWorkManager
import com.jamgu.hwstatistics.phonestate.PhoneStateManager
import com.jamgu.hwstatistics.system.SystemManager
import com.jamgu.hwstatistics.thread.ThreadPool
import com.jamgu.hwstatistics.util.roundToDecimals
import com.permissionx.guolindev.PermissionX
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Created by jamgu on 2021/10/14
 */
class StatisticsLoader: INeedPermission {

    private lateinit var weakContext: WeakReference<Context>

    companion object {
        private const val TAG = "StatisticsLoader"
        private const val TOTAL_LENGTH = 10
    }

    private var mTimer: ValueAnimator? = null
    private var mHandlerThread: HandlerThread? = null

    private var uiCallback: ((String) -> Unit)? = null
    private val data: ArrayList<ArrayList<Any>> = ArrayList()

    fun init(ctx: Context, callback: ((String) -> Unit)?): StatisticsLoader {
        mHandlerThread = HandlerThread("Statistics Thread")
        mHandlerThread?.start()

        uiCallback = callback
        weakContext = WeakReference(ctx)
        data.clear()
        val context = weakContext.get()
        PhoneStateManager.register(context)
        BrightnessManager.registerReceiver(context)
        SystemManager.registerSystemReceiver(context)
        return this
    }

    @SuppressLint("SimpleDateFormat")
    private fun start() {
        if (mTimer == null) {
            mTimer = ValueAnimator.ofInt(TOTAL_LENGTH, 0)
        }

        data.clear()

        var lastVal = -1
        var currentRepeatCount = 0
        var lastTimeMills = 0L
        var lastTimeString = ""
        mTimer?.apply {
            addUpdateListener {
                val curVal = it.animatedValue
                if (curVal is Int && curVal != lastVal) {
                    if (curVal == 10) currentRepeatCount++

                    // do something
                    val currentTimeMillis = System.currentTimeMillis()
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                    val curTimeString: String = sdf.format(Date(currentTimeMillis))

                    // 无限循环时，下一次开启计时的时间，与上一次结束的时间之间的间隔很短，
                    // 会多一次的数据，所以去重
                    if (curTimeString == lastTimeString) {
                        return@addUpdateListener
                    }

                    val screenOn = getScreenStatus()
                    val screenBrightness = getScreenBrightness()
                    val phoneState = getPhoneState()
                    val systemOnStatus = getSystemStatus()
                    val musicState = getMusicState()
                    val networkType = getNetworkType()
                    val netWorkSpeed = getNetWorkSpeed()
                    val cpuInfo = getCpuInfo()
                    val cpuTotalUsage = getCpuTotalUsage()

                    val domain = Builder2().apply {
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
                    }.buildArray()

                    data.add(domain)

                    lastTimeString = curTimeString
                    lastTimeMills = currentTimeMillis

                    val statisticText = "curTimeMils: $curTimeString, $currentTimeMillis"
//                            + "screen_on: $screenOn，" +
//                            "screen_br: $screenBrightness, phone_state_ringing: ${phoneState[0]}, " +
//                            "phone_state_off_hook: ${phoneState[1]}, system_on: $systemOnStatus, " +
//                            "music_on: $musicState, network_type: $networkType, network_speed: $netWorkSpeed kb/s, " +
//                            "\n\n" +
//                            "cpus: $cpuInfo"

                    Log.d(TAG, "curVal = $curVal, curRepeat = $currentRepeatCount, info:| $statisticText")
                    uiCallback?.invoke(statisticText)

                    lastVal = curVal
                }
            }

            interpolator = LinearInterpolator()
            duration = TOTAL_LENGTH * 1000L
            repeatCount = INFINITE
            start()
        }
    }

    fun stop() {
        if (mTimer?.isRunning == true) {
            destroyTimer()
        }

    }

    fun isStarted(): Boolean {
        return mTimer?.isStarted == true
    }

    fun startNonMainThread() {
        mHandlerThread?.let {
            Handler(it.looper).post {
                start()
            }
        }
    }

    fun release() {
        destroyTimer()
        val context = weakContext.get()
        PhoneStateManager.unregister(context)
        BrightnessManager.unregisterReceiver(context)
        SystemManager.unregisterSystemReceiver(context)
        data.clear()
        weakContext.clear()
        mHandlerThread?.quitSafely()
    }

    fun getData() = data

    fun requestedPermission(context: FragmentActivity?): Boolean {
        val permissions = ArrayList<String>().apply {
            addAll(permission())
            addAll(NetWorkManager.permission())
        }
        val notGrantedPermission = permissions.filterNot { PermissionX.isGranted(context, it) }
        if (notGrantedPermission.isEmpty()) {
            return true
        } else {
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
            return false
        }
    }

    private fun destroyTimer() {
        mTimer?.let {
            if (it.isRunning)
                it.cancel()
            it.removeAllUpdateListeners()
        }
        mTimer = null
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

        for(i in 0 until cpuNumb) {
            val cpuMaxFreq = CPUInfoManager.getCpuMaxFreq(i)
            val cpuMinFreq = CPUInfoManager.getCpuMinFreq(i)
            val cpuRunningFreq = CPUInfoManager.getCpuRunningFreq(i)
            val cpuTemp = CPUInfoManager.getCpuTemp(i)
            val cpu = CPU(cpuMaxFreq, cpuMinFreq, cpuRunningFreq, cpuTemp, cpuUtils?.get(i)?: 0f)
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