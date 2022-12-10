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
    private val mAppUsageData: ArrayList<AppUsageRecord> = ArrayList()

    private var activeBroadcastReceiver: ActiveBroadcastReceiver? = null
    private var mLastResumeRecord: AppUsageRecord.ActivityResumeRecord? = null

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
        JLog.d(TAG, "----------------- new ---------------")
        var firstActivityResumeRecord: AppUsageRecord.ActivityResumeRecord? = null
        while (usages != null && usages.hasNextEvent()) {
            usages.getNextEvent(event)
            val packageName = event.packageName ?: continue
            val className = event.className
            val timeStamp = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    JLog.d(TAG, "DEVICE_SHUTDOWN")
                }
                UsageEvents.Event.DEVICE_STARTUP -> {
                    JLog.d(TAG, "DEVICE_STARTUP")
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    JLog.d(TAG, "SCREEN_INTERACTIVE")
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    JLog.d(TAG, "SCREEN_NON_INTERACTIVE")
                }
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val resumeRecord = AppUsageRecord.ActivityResumeRecord(packageName,
                        className, timeStamp.timeStamp2DateString())

                    if (firstActivityResumeRecord == null) {
                        firstActivityResumeRecord = resumeRecord
                    }

                    // 最新的APP使用记录与上一个相同，说明已经遍历完新事件了
                    if (isResumeRecordSameWithLast(resumeRecord)) {
                        updateLastResumeRecord(firstActivityResumeRecord)
                        return
                    }
                    // 不同，说明是新事件，更新
                    else {
                        val dataSize = mAppUsageData.size
                        if (dataSize == 0) {
                            mAppUsageData.add(resumeRecord)
                        } else {
                            // 1. 先记录上一个record的停留时间
                            val lastResumeRecord = mAppUsageData[dataSize - 1]
                            // 上一個记录是resume record，更新
                            if (lastResumeRecord is AppUsageRecord.ActivityResumeRecord) {
                                replaceLastResumeRecord2UsageRecord(lastResumeRecord)
                            }
                            // 2. 新增一条 resume record记录
                            mAppUsageData.add(resumeRecord)
                        }
                    }
                    JLog.d(TAG, "getTopActivity, packageName = $packageName，className = $className, tm = $timeStamp")
                }
                UsageEvents.Event.USER_INTERACTION -> {
                    JLog.d(TAG, "USER_INTERACTION")
                }
            }
        }
    }

    /**
     * 更新上一个 resume record 记录为 usage record 记录：补充使用时长
     */
    private fun replaceLastResumeRecord2UsageRecord(lastResumeRecord: AppUsageRecord.ActivityResumeRecord) {
        val startTime = lastResumeRecord.mTimeStamp
        val curTime = getCurrentDateString()
        val duration = curTime.timeMillsBetween(startTime)
        val usageRecord = AppUsageRecord.UsageRecord(lastResumeRecord.mPackageName,
            lastResumeRecord.mClassName, startTime,
            curTime, duration.timeStamp2SimpleDateString(), duration
        )
        // 替换记录
        mAppUsageData.remove(lastResumeRecord)
        mAppUsageData.add(usageRecord)
    }

    /**
     * 当前活动记录是否与上一个活动记录相同
     */
    private fun isResumeRecordSameWithLast(curRecord: AppUsageRecord.ActivityResumeRecord?): Boolean {
        curRecord ?: return false

//        return curRecord == getLatestSavedAppUsage()
        return curRecord == mLastResumeRecord
    }

    private fun updateLastResumeRecord(firstRecord: AppUsageRecord.ActivityResumeRecord?) {
        mLastResumeRecord = firstRecord
    }

    /**
     * 获取用户最近的APP访问记录
     */
    private fun getLatestSavedAppUsage(): String {
        val ySize = mAppUsageData.size
        if (ySize <= 0) return ""

        val lastAppUsageRecord = mAppUsageData[ySize - 1]
//        val xSize = lastAppUsageRecord.size
//        if (xSize <= 4) return ""
//
//        return lastAppUsageRecord[1]
        return ""
    }

    /**
     * 向数据中添加空行
     */
    private fun addEmptyLine() {
//        mAppUsageData.add(arrayListOf())
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