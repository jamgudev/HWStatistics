package com.jamgu.hwstatistics.appusage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.timer.VATimer
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.StatisticsLoader
import com.jamgu.hwstatistics.keeplive.service.screen.ActiveBroadcastReceiver
import com.jamgu.hwstatistics.upload.DataSaver
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/12/7 7:43 下午
 *
 * @description 用户打开app信息收集器
 */
class AppUsageDataLoader(private val mContext: Context) :
    ActiveBroadcastReceiver.IOnScreenStateChanged {

    @Volatile
    private var mScreenOn: AtomicBoolean = AtomicBoolean(false)

    // 用来存放用户打开的应用信息
    // --- 时间 --- 活动名 --- 详细包名 --- 开始访问时间 --- 结束访问时间 --- 访问时长
    private val mAppUsageData: ArrayList<UsageRecord> = ArrayList()

    private var activeBroadcastReceiver: ActiveBroadcastReceiver? = null

    // 上一个事件轮询的最后一个 activity resumes 事件
    private var mLastResumeRecord: UsageRecord.ActivityResumeRecord? = null

    // 屏幕亮起事件
    private var mScreenOnRecord: UsageRecord.PhoneLifeCycleRecord? = null

    // 用户解锁事件
    private var mUserPresentRecord: UsageRecord.PhoneLifeCycleRecord? = null

    private var mSessionListener: IOnUserSessionListener? = null

    private lateinit var mPowerDataLoader: StatisticsLoader

    private var mTimer: VATimer = VATimer()

    companion object {
        private const val TAG = "AppUsageDataLoader"
        const val TEXT_SESSION_SUMMARIZE = "Session Summarize"
    }

    init {
        // 注册开机、关机、解锁广播
        if (activeBroadcastReceiver == null) {
            activeBroadcastReceiver = ActiveBroadcastReceiver(this)
        }
    }

    fun start() {
        mTimer.run({
            queryCurrentUsedApp()
        }, 1000)
    }

    fun isStarted(): Boolean {
        return mTimer.isStarted()
    }

    fun setOnSessionListener(listener: IOnUserSessionListener?) {
        mSessionListener = listener
    }

    fun getUsageData(): ArrayList<UsageRecord> {
        return mAppUsageData
    }

    private fun clearUsageData() {
        return mAppUsageData.clear()
    }

    /**
     * 获取手机顶层Activity
     */
    private fun queryCurrentUsedApp() {
        if (!mScreenOn.get()) {
            return
        }
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1500
        val sUsageStatsManager =
            mContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val usages = sUsageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        JLog.d(TAG, "----------------- new ---------------")
        while (usages != null && usages.hasNextEvent()) {
            usages.getNextEvent(event)
            val packageName = event.packageName ?: continue
            val className = event.className
            val timeStamp = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val resumeRecord = UsageRecord.ActivityResumeRecord(
                        packageName,
                        className, timeStamp.timeStamp2DateStringWithMills()
                    )

                    // 当前事件是否是新事件
                    if (!isNewResumeRecord(resumeRecord)) {
                        return
                    }
                    // 说明是新事件，更新
                    else {
                        val dataSize = mAppUsageData.size
                        if (dataSize == 0) {
                            mAppUsageData.add(resumeRecord)
                        } else {
                            // 1. 先记录上一个record的停留时间
                            replaceLastResumeRecord2UsageRecord()
                            // 2. 新增一条 resume record记录
                            mAppUsageData.add(resumeRecord)
                        }

                        updateLatestResumeRecord(resumeRecord)
                    }
                    JLog.d(
                        TAG,
                        "getTopActivity, packageName = $packageName，className = $className, tm = $timeStamp"
                    )
                }
            }
        }
    }

    /**
     * 更新上一个 resume record 记录为 usage record 记录：补充使用时长
     */
    private fun replaceLastResumeRecord2UsageRecord() {
        val dataSize = mAppUsageData.size
        if (dataSize <= 0) return

        val lastResumeRecord = mAppUsageData[dataSize - 1]
        if (lastResumeRecord is UsageRecord.ActivityResumeRecord) {
            val startTime = lastResumeRecord.mTimeStamp
            val curTime = getCurrentDateString()
            val duration = curTime.timeMillsBetween(startTime)
            val usageRecord = UsageRecord.AppUsageRecord(
                lastResumeRecord.mPackageName,
                lastResumeRecord.mClassName, startTime,
                curTime, duration.timeStamp2SimpleDateString(), duration
            )
            // 替换记录
            mAppUsageData.remove(lastResumeRecord)
            mAppUsageData.add(usageRecord)
        }
    }

    /**
     * 当前活动记录是否与上一个活动记录相同
     */
    private fun isNewResumeRecord(curRecord: UsageRecord.ActivityResumeRecord?): Boolean {
        curRecord ?: return false

        return curRecord.mTimeStamp > (mLastResumeRecord?.mTimeStamp ?: "0")
    }

    private fun updateLatestResumeRecord(firstRecord: UsageRecord.ActivityResumeRecord?) {
        mLastResumeRecord = firstRecord
    }

    /**
     * 记录一次用户解锁
     */
    private fun addUserPresentRecord() {
        replaceLastResumeRecord2UsageRecord()

        val usageName = mContext.getString(R.string.usage_user_present)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
        mAppUsageData.add(cycleRecord)
        mUserPresentRecord = cycleRecord
    }

    /**
     * 记录一次屏幕亮起
     */
    private fun addOnScreenOnRecord() {
        val usageName = mContext.getString(R.string.usage_screen_on)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
        mAppUsageData.add(cycleRecord)
        mScreenOnRecord = cycleRecord
    }

    /**
     * 记录一次屏幕熄灭
     */
    private fun addOnScreenOffRecord(): UsageRecord.SingleSessionRecord {
        // 先记录上一个record的停留时间
        replaceLastResumeRecord2UsageRecord()

        val usageName = mContext.getString(R.string.usage_screen_off)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
        mAppUsageData.add(cycleRecord)

        // 记录一次session
        return addSessionRecord(cycleRecord)
    }

    /**
     * 记录一次手机关机，也算一次屏幕熄灭事件，记录session
     */
    private fun addOnShutdownRecord(): UsageRecord.SingleSessionRecord {
        replaceLastResumeRecord2UsageRecord()

        val usageName = mContext.getString(R.string.usage_shut_down)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
        mAppUsageData.add(cycleRecord)

        return addSessionRecord(cycleRecord)
    }

    /**
     * 经历完屏幕亮起和熄灭后，记录一次session
     */
    private fun addSessionRecord(screenOfRecord: UsageRecord.PhoneLifeCycleRecord): UsageRecord.SingleSessionRecord {
        addEmptyLine()
        addTextTitle(arrayOf(TEXT_SESSION_SUMMARIZE))

        summarizeActivityUsageRecords()
        val screenOfTime = screenOfRecord.mOccTime
        val screenOnTime = mScreenOnRecord?.mOccTime ?: ""
        val presentTime = mUserPresentRecord?.mOccTime ?: ""
        val screenSession = screenOfTime.timeMillsBetween(screenOnTime)
        val presentSession = screenOfTime.timeMillsBetween(presentTime)
        val sessionName = mContext.getString(R.string.usage_session)
        val sessionRecord = UsageRecord.SingleSessionRecord(
            sessionName, screenOnTime, presentTime,
            screenOfTime, screenSession, presentSession
        )
        mAppUsageData.add(sessionRecord)

        saveData2File("${screenOnTime}_${screenOfTime}")
        return sessionRecord
    }

    /**
     * 一次session完成后，总结一次activity使用记录
     */
    private fun summarizeActivityUsageRecords() {
        val appUsageRecordMap = HashMap<String, UsageRecord.SingleAppUsageRecord>()
        mAppUsageData.forEach { appUsageRecord ->
            if (appUsageRecord is UsageRecord.AppUsageRecord) {
                val packageName = appUsageRecord.mUsageName
                if (appUsageRecordMap.contains(packageName)) {
                    val singleUsageRecord = appUsageRecordMap[packageName] ?: return@forEach
                    singleUsageRecord.mDurationLong += appUsageRecord.mDurationLong
                    singleUsageRecord.mDuration = singleUsageRecord.mDurationLong
                        .timeStamp2SimpleDateString()
                } else {
                    appUsageRecordMap[packageName] = UsageRecord.SingleAppUsageRecord(
                        packageName,
                        appUsageRecord.mDuration, appUsageRecord.mDurationLong
                    )
                }
            }
        }

        appUsageRecordMap.values.forEach { singleAppRecord ->
            mAppUsageData.add(singleAppRecord)
        }
    }

    /**
     * 向数据中添加空行
     */
    private fun addEmptyLine() {
        mAppUsageData.add(UsageRecord.EmptyUsageRecord())
    }

    private fun addTextTitle(titles: Array<String>) {
        mAppUsageData.add(UsageRecord.TextTitleRecord(titles))
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
        mPowerDataLoader = StatisticsLoader(mContext).initOnCreate {}
    }

    fun onDestroy() {
        mContext.unregisterReceiver(activeBroadcastReceiver)
        mPowerDataLoader.release()
        resetAfterDataSaved()
    }

    /**
     * 一次session过后，重置属性
     */
    private fun resetAfterDataSaved() {
        mScreenOnRecord = null
        mUserPresentRecord = null
        mLastResumeRecord = null
        mScreenOn.set(false)
        clearUsageData()
        mPowerDataLoader.clearData()
    }

    override fun onPhoneBootComplete() {
        // do noting
    }

    override fun onPhoneShutdown() {
        if (!mScreenOn.get()) {
            return
        }

        val shutdownRecord = addOnShutdownRecord()
        mSessionListener?.onSessionEnd(shutdownRecord)
    }

    override fun onScreenOn() {
        mScreenOn.set(true)
        if (mPowerDataLoader.isStarted()) {
            mPowerDataLoader.stop()
        }
        mPowerDataLoader.startNonMainThread()
        addOnScreenOnRecord()
        mSessionListener?.onSessionBegin()
    }

    override fun onScreenOff() {
        if (!mScreenOn.get()) {
            return
        }
        val screenOffRecord = addOnScreenOffRecord()
        mSessionListener?.onSessionEnd(screenOffRecord)
    }

    override fun onUserPresent() {
        if (!mScreenOn.get()) {
            return
        }
        addUserPresentRecord()
    }

    /**
     * 将数据保存到缓存目录
     */
    private fun saveData2File(fileName: String) {
        if (mPowerDataLoader.isStarted()) {
            mPowerDataLoader.stop()
        }
        val powerDataWithTitle = ArrayList(mPowerDataLoader.getDataWithTitle())
        val appUsageData = ArrayList(mAppUsageData)
        DataSaver.saveAppUsageDataSync(mContext, appUsageData, powerDataWithTitle, fileName)
        resetAfterDataSaved()
    }

    /**
     * 用户一次交互Session监听
     */
    interface IOnUserSessionListener {
        fun onSessionBegin()
        fun onSessionEnd(session: UsageRecord.SingleSessionRecord)
    }
}