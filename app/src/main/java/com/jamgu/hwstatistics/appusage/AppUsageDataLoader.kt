package com.jamgu.hwstatistics.appusage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.keeplive.service.screen.ActiveBroadcastReceiver

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/12/7 7:43 下午
 *
 * @description 用户打开app信息收集器
 */
class AppUsageDataLoader(private val mContext: Context): ActiveBroadcastReceiver.IOnScreenStateChanged {

    // 用来存放用户打开的应用信息
    // --- 时间 --- 活动名 --- 详细包名 --- 开始访问时间 --- 结束访问时间 --- 访问时长
    private val mAppUsageData: ArrayList<ArrayList<String>> = ArrayList()

    private var activeBroadcastReceiver: ActiveBroadcastReceiver? = null
    private var mLastAppRecordName: String = ""

    companion object {
        private const val TAG = "AppUsageDataLoader"
    }

    init {
        // 注册开机、关机、解锁广播
        if (activeBroadcastReceiver == null) {
            activeBroadcastReceiver = ActiveBroadcastReceiver(this)
        }
    }

    /**
     * 获取手机顶层Activity
     */
    fun getCurrentUsedApp() {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000
        val sUsageStatsManager = mContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var result = ""

        val usages = sUsageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        while (usages != null && usages.hasNextEvent()) {
            usages.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val packageName = event.packageName ?: continue
                // 最新的APP使用记录与上一个相同，跳过
                if (isRecordSameWithLast(packageName)) {
                    return
                } else {
                    // 不同，记录上一个APP的使用时长，并更新下一个APP的开始使用时间
                    // TODO FINISH THIS

                }
                val className = event.className
            }
//            JLog.d(TAG, "getTopActivity, result = $result，tm = $endTime")
        }
    }

    /**
     * 当前活动记录是否与上一个活动记录相同
     */
    private fun isRecordSameWithLast(curRecord: String?): Boolean {
        if (curRecord.isNullOrEmpty()) return false

//        return curRecord == getLatestSavedAppUsage()
        return curRecord == mLastAppRecordName
    }

    /**
     * 获取用户最近的APP访问记录
     */
    private fun getLatestSavedAppUsage(): String {
        val ySize = mAppUsageData.size
        if (ySize <= 0) return ""

        val lastAppUsageRecord = mAppUsageData[ySize - 1]
        val xSize = lastAppUsageRecord.size
        if (xSize <= 4) return ""

        return lastAppUsageRecord[1]
    }

    /**
     * 向数据中添加空行
     */
    private fun addEmptyLine() {
        mAppUsageData.add(arrayListOf())
    }

    fun onCreate() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        // 8.0 后，只能通过动态注册
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED)
            intentFilter.addAction(Intent.ACTION_SHUTDOWN)
        }
        mContext.registerReceiver(activeBroadcastReceiver, intentFilter)
    }

    fun onDestroy() {
        mContext.unregisterReceiver(activeBroadcastReceiver)
    }

    override fun onPhoneBootComplete() {

    }

    override fun onPhoneShutdown() {
    }

    override fun onScreenOn() {
    }

    override fun onScreenOff() {
    }

    override fun onUserPresent() {
    }

}