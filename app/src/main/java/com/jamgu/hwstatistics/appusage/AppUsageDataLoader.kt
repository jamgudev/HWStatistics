package com.jamgu.hwstatistics.appusage

import android.app.Activity
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentCallbacks2.*
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.jamgu.common.Common
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.common.util.timer.VATimer
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.appusage.broadcast.PhoneCycleBroadcastReceiver
import com.jamgu.hwstatistics.appusage.broadcast.PowerConnectReceiver
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.hwstatistics.net.upload.DataSaver.TAG_SCREEN_OFF
import com.jamgu.hwstatistics.net.upload.DataUploader
import com.jamgu.hwstatistics.net.upload.DataUploader.PA_THRESHOLD
import com.jamgu.hwstatistics.power.IOnDataEnough
import com.jamgu.hwstatistics.power.StatisticsLoader
import com.jamgu.hwstatistics.util.getCurrentDateString
import com.jamgu.hwstatistics.util.timeMillsBetween
import com.jamgu.hwstatistics.util.timeStamp2DateStringWithMills
import com.jamgu.hwstatistics.util.timeStamp2SimpleDateString
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author jamgudev
 * @date 2022/12/7 7:43 下午
 *
 * @description 用户打开app信息收集器
 */
class AppUsageDataLoader(private val mContext: FragmentActivity) :
    PhoneCycleBroadcastReceiver.IOnScreenStateChanged, PowerConnectReceiver.IOnPhoneChargeStateChanged {

    @Volatile
    private var mScreenOn: AtomicBoolean = AtomicBoolean(false)

    @Volatile
    private var mIsCharging: AtomicBoolean = AtomicBoolean(false)

    // 用来存放一次Session用户行为
    // --- 时间 --- 活动名 --- 详细包名 --- 开始访问时间 --- 结束访问时间 --- 访问时长
    private val mUserUsageData: MutableList<UsageRecord> = Collections.synchronizedList(ArrayList())

    // 用户充电记录是跨Session的，所以单独存储
    private val mChargeData: MutableList<UsageRecord> = Collections.synchronizedList(ArrayList())

    // 监听手机Session生命周期
    private var mActiveBroadcastReceiver: PhoneCycleBroadcastReceiver? = null

    // 监听手机充电状态
    private var mPowerConnectReceiver: PowerConnectReceiver? = null

    // 上一个事件轮询的最后一个 activity resume 事件
    private var mLastResumeRecord: UsageRecord.ActivityResumeRecord? = null
    private var mResumeRecordLock = Any()

    // 上一个事件轮询的最后一个 activity pause 事件
    private var mLastPauseRecord: UsageRecord.ActivityPauseRecord? = null
    private var mPauseRecordLock = Any()

    // 屏幕亮起事件
    private var mScreenOnRecord: UsageRecord.PhoneLifeCycleRecord? = null

    // 用户解锁事件
    private var mUserPresentRecord: UsageRecord.PhoneLifeCycleRecord? = null

    private var mSessionListener: IOnUserSessionListener? = null

    private lateinit var mPowerDataLoader: StatisticsLoader

    private var mTimer: VATimer = VATimer("AppUsageData").apply {
        setUncaughtExceptionHandler { t, e ->
            DataSaver.addInfoTracker(
                Common.getInstance().getApplicationContext(),
                "uncaughtException: threadName#${t.name}, e = ${e.stackTraceToString()}")
        }
    }

    companion object {
        private const val TAG = "AppUsageDataLoader"
        const val TEXT_SESSION_SUMMARIZE = "Session Summarize"
    }

    init {
        // 注册开机、关机、解锁广播
        if (mActiveBroadcastReceiver == null) {
            mActiveBroadcastReceiver = PhoneCycleBroadcastReceiver(this)
        }
        if (mPowerConnectReceiver == null) {
            mPowerConnectReceiver = PowerConnectReceiver(this)
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

    fun getUserUsageData(): ArrayList<UsageRecord> {
        return ArrayList(mUserUsageData)
    }

    private fun clearUsageData() {
        mPowerDataLoader.clearData()
        return mUserUsageData.clear()
    }

    /**
     * 获取手机顶层Activity
     */
    private fun queryCurrentUsedApp() {
        checkIfSavePhoneChargeData2File(if (mContext is Activity) mContext.isDestroyed else false)

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

                    JLog.d(
                        TAG,
                        "activity resume, packageName = $packageName，className = $className, tm = $timeStamp"
                    )

                    // 当前事件是否是新事件
                    if (!isNewResumeRecord(resumeRecord)) {
                        return
                    }
                    // 说明是新事件，更新
                    else {
                        val dataSize = mUserUsageData.size
                        if (dataSize == 0) {
                            mUserUsageData.add(resumeRecord)
                        } else {
                            // 经测试，可能存在PAUSE事件漏发的情况，
                            // 在新的RESUME事件到来之前，检查一遍上一个Activity事件是否完整
                            replaceLastResumeRecord2UsageRecord(resumeRecord.mTimeStamp)
                            // 2. 新增一条 resume record记录
                            mUserUsageData.add(resumeRecord)
                        }

                        updateLatestResumeRecord(resumeRecord)
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val pauseRecord = UsageRecord.ActivityPauseRecord(
                        packageName,
                        className, timeStamp.timeStamp2DateStringWithMills()
                    )

                    JLog.d(
                        TAG,
                        "activity pause, packageName = $packageName，className = $className, tm = $timeStamp"
                    )

                    if (!isNewPauseRecord(pauseRecord)) {
                        return
                    }
                    // 新的pause事件
                    else {
                        checkWhetherPreviousResumeIsMissing(pauseRecord)
                        updateLatestPauseRecord(pauseRecord)
                    }
                }
                else -> {
//                    JLog.d(
//                        TAG,
//                        "other event = ${event.eventType}, packageName = $packageName，className = $className, tm = $timeStamp"
//                    )
                }
            }
        }
    }

    /**
     * 新的pauseRecord到来时，如果上一个record已经是完整的ActivityUsageRecord，说明有resumeRecord遗漏了需要补充
     * 如果上一个record是ResumeRecord，说明数据正常，走ResumeRecord替换逻辑
     */
    private fun checkWhetherPreviousResumeIsMissing(pauseRecord: UsageRecord.ActivityPauseRecord) {
        val dataSize = mUserUsageData.size
        if (dataSize <= 0) return

        val lastRecord = mUserUsageData[dataSize - 1]
        if (lastRecord is UsageRecord.AppUsageRecord && lastRecord.mEndTime < pauseRecord.mTimeStamp) {
            // 如果上一个record是一个完整的Activity记录，且activity记录结束时间比当前的pauseEventTime要早
            // 说明有ActivityResume事件遗漏了，需要补充完整
            val newResumeRecord = UsageRecord.ActivityResumeRecord(
                pauseRecord.mPackageName,
                pauseRecord.mClassName, lastRecord.mEndTime
            )
            mUserUsageData.add(newResumeRecord)
            updateLatestResumeRecord(newResumeRecord)
        }

        replaceLastResumeRecord2UsageRecord(pauseRecord.mTimeStamp)
    }

    /**
     * 更新上一个 resume record 记录为 usage record 记录：补充使用时长
     */
    private fun replaceLastResumeRecord2UsageRecord(endDateString: String) {
        val dataSize = mUserUsageData.size
        if (dataSize <= 0) return

        val lastResumeRecord = mUserUsageData[dataSize - 1]
        if (lastResumeRecord is UsageRecord.ActivityResumeRecord) {
            val startTime = lastResumeRecord.mTimeStamp
            val duration = endDateString.timeMillsBetween(startTime)
            val usageRecord = UsageRecord.AppUsageRecord(
                lastResumeRecord.mPackageName,
                lastResumeRecord.mClassName, startTime,
                endDateString, duration.timeStamp2SimpleDateString(), duration
            )
            // 替换记录
            mUserUsageData.remove(lastResumeRecord)
            mUserUsageData.add(usageRecord)
        }
    }

    /**
     * 当前活动RESUME记录是否与上一个活动记录相同
     */
    private fun isNewResumeRecord(curRecord: UsageRecord.ActivityResumeRecord?): Boolean {
        curRecord ?: return false

        return synchronized(mResumeRecordLock) {
            curRecord.mTimeStamp > (mLastResumeRecord?.mTimeStamp ?: "0")
        }
    }

    private fun updateLatestResumeRecord(newRecord: UsageRecord.ActivityResumeRecord?) {
        synchronized(mResumeRecordLock) {
            mLastResumeRecord = newRecord
        }
    }

    /**
     * 当前活动PAUSE记录是否与上一个活动记录相同
     */
    private fun isNewPauseRecord(curRecord: UsageRecord.ActivityPauseRecord?): Boolean {
        curRecord ?: return false

        return synchronized(mPauseRecordLock) {
            curRecord.mTimeStamp > (mLastPauseRecord?.mTimeStamp ?: "0")
        }
    }

    private fun updateLatestPauseRecord(newRecord: UsageRecord.ActivityPauseRecord?) {
        synchronized(mPauseRecordLock) {
            mLastPauseRecord = newRecord
        }
    }

    private fun getUserPresentRecord(): UsageRecord.PhoneLifeCycleRecord {
        val usageName = mContext.getString(R.string.usage_user_present)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        return UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
    }

    /**
     * 记录一次用户解锁
     */
    private fun addUserPresentRecord() {
        // 用户解锁事件，因为消息可能存在延迟，事件到来的时间点可能会比Activity Resume事件来的晚
        // 所以固定将解锁事件放在 ScreenOn 事件下面
        val addIndex = if (mUserUsageData.isEmpty()) {
            0
        } else {
            1
        }
        val presentRecord = mUserPresentRecord ?: getUserPresentRecord()
        mUserUsageData.add(addIndex, presentRecord)
        if (mUserPresentRecord == null) {
            mUserPresentRecord = presentRecord
        }
    }

    /**
     * 记录一次屏幕亮起
     */
    private fun addOnScreenOnRecord() {
        val usageName = mContext.getString(R.string.usage_screen_on)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
        mUserUsageData.add(0, cycleRecord)
        mScreenOnRecord = cycleRecord
    }

    /**
     * 记录一次屏幕熄灭
     */
    private fun addOnScreenOffRecord(): UsageRecord.SingleSessionRecord {
        replaceLastResumeRecord2UsageRecord(getCurrentDateString())

        val usageName = mContext.getString(R.string.usage_screen_off)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
        mUserUsageData.add(cycleRecord)

        // 记录一次session
        return addSessionRecord(cycleRecord)
    }


    /**
     * 记录一次电池充电状态
     */
    private fun addOnPowerCharge(curBatteryState: Float) {
        val usageName = mContext.getString(R.string.usage_phone_charging)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneChargeRecord(usageName, occurrenceTime, curBatteryState.toString())
        mChargeData.add(cycleRecord)
    }

    /**
     * 记录一次电池取消充电状态
     */
    private fun addOnPowerCancelCharge(curBatteryState: Float) {
        val usageName = mContext.getString(R.string.usage_phone_cancel_charge)
        val occurrenceTime = System.currentTimeMillis().timeStamp2DateStringWithMills()
        val cycleRecord = UsageRecord.PhoneChargeRecord(usageName, occurrenceTime, curBatteryState.toString())
        mChargeData.add(cycleRecord)
    }

    /**
     * 记录一次手机关机，也算一次屏幕熄灭事件，记录session
     */
    private fun addOnShutdownRecord(): UsageRecord.SingleSessionRecord {
        replaceLastResumeRecord2UsageRecord(getCurrentDateString())

        val usageName = mContext.getString(R.string.usage_shut_down)
        val occurrenceTime = getCurrentDateString()
        val cycleRecord = UsageRecord.PhoneLifeCycleRecord(usageName, occurrenceTime)
        mUserUsageData.add(cycleRecord)

        return addSessionRecord(cycleRecord)
    }

    /**
     * 经历完屏幕亮起和熄灭后，记录一次session
     */
    private fun addSessionRecord(screenOfRecord: UsageRecord.PhoneLifeCycleRecord): UsageRecord.SingleSessionRecord {
//        addEmptyLine()
        addTextTitle(arrayOf(TEXT_SESSION_SUMMARIZE))

        val activitySession = summarizeActivityUsageRecords()
        val screenOfTime = screenOfRecord.mOccTime
        val screenOnTime = mScreenOnRecord?.mOccTime ?: ""
        val presentTime = mUserPresentRecord?.mOccTime ?: ""
        val screenSession = screenOfTime.timeMillsBetween(screenOnTime)
        val presentSession = screenOfTime.timeMillsBetween(presentTime)
        val sessionName = mContext.getString(R.string.usage_session)
        val sessionRecord = UsageRecord.SingleSessionRecord(
            sessionName, screenOnTime, presentTime,
            screenOfTime, screenSession, presentSession, activitySession
        )
        mUserUsageData.add(sessionRecord)

        saveUserUsageData2File(screenOnTime, screenOfTime)
        return sessionRecord
    }

    /**
     * 一次session完成后，总结一次activity使用记录
     */
    private fun summarizeActivityUsageRecords(): Long {
        val appUsageRecordMap = HashMap<String, UsageRecord.SingleAppUsageRecord>()
        mUserUsageData.forEach { appUsageRecord ->
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

        var totalActivityResumeDuration = 0L
        appUsageRecordMap.values.forEach { singleAppRecord ->
            totalActivityResumeDuration += singleAppRecord.mDurationLong
            mUserUsageData.add(singleAppRecord)
        }
        return totalActivityResumeDuration
    }

    /**
     * 向数据中添加空行
     */
    private fun addEmptyLine() {
        mUserUsageData.add(UsageRecord.EmptyUsageRecord())
    }

    private fun addTextTitle(titles: Array<String>) {
        mUserUsageData.add(UsageRecord.TextTitleRecord(titles))
    }

    fun onCreate() {
        mActiveBroadcastReceiver?.registerReceiver(mContext)
        mPowerConnectReceiver?.registerReceiver(mContext)
        mPowerDataLoader = StatisticsLoader(mContext).initOnCreate {}.apply {
            setOnDataEnoughListener(IOnDataEnough.ThreshLength.THRESH_ONE_MIN.length, object : IOnDataEnough {
                override fun onDataEnough() {
                    saveTempUserUsageData2File()
                    // 更新阈值
                    val paThreshold = PreferenceUtil.getCachePreference(mContext, 0).getInt(PA_THRESHOLD, 60).toLong()
                    if (mPowerDataLoader.getDataNumThreshold() != paThreshold) {
                        mPowerDataLoader.setOnDataEnoughListener(paThreshold, this)
                    }
                }
            })
        }
    }

    fun onDestroy() {
        val screenOnTime = mScreenOnRecord?.mOccTime ?: ""
        saveUserUsageData2File(screenOnTime, System.currentTimeMillis().timeStamp2DateStringWithMills())
        checkIfSavePhoneChargeData2File(true)

        mActiveBroadcastReceiver?.unRegisterReceiver(mContext)
        mPowerConnectReceiver?.unRegisterReceiver(mContext)
        mPowerDataLoader.release()
    }

    /**
     * 一次session过后，重置属性
     */
    private fun resetAfterDataSaved() {
        mScreenOnRecord = null
        mUserPresentRecord = null
        updateLatestPauseRecord(null)
        updateLatestResumeRecord(null)
        mScreenOn.set(false)
        clearUsageData()
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
        PreferenceUtil.getCachePreference(mContext, 0).edit().putBoolean((TAG_SCREEN_OFF), !mScreenOn.get()).apply()
        if (mPowerDataLoader.isStarted()) {
            mPowerDataLoader.stop()
        }
        mPowerDataLoader.startNonMainThread()
        addOnScreenOnRecord()
        // 可能存在present事件比screen_on事件先到，导致present事件丢失的情况
        if (mUserPresentRecord != null) {
            addUserPresentRecord()
        }
        mSessionListener?.onSessionBegin()
    }

    override fun onScreenOff() {
        // 先把之前的数据上传
        mScreenOn.set(false)
        PreferenceUtil.getCachePreference(mContext, 0).edit().putBoolean((TAG_SCREEN_OFF), !mScreenOn.get()).apply()
        if (mIsCharging.get()) {
            val nowDate = System.currentTimeMillis().timeStamp2DateStringWithMills()
            DataUploader.recursivelyUpload(mContext, File(DataSaver.getCacheRootPath()), nowDate)
        }
        val screenOffRecord = addOnScreenOffRecord()
        mSessionListener?.onSessionEnd(screenOffRecord)

    }

    override fun onChargeState(curBatteryState: Float) {
        mIsCharging.set(true)
        addOnPowerCharge(curBatteryState)
    }

    override fun onCancelChargeState(curBatteryState: Float) {
        mIsCharging.set(false)
        addOnPowerCancelCharge(curBatteryState)
    }

    override fun onUserPresent() {
        if (!mScreenOn.get()) {
            mUserPresentRecord = getUserPresentRecord()
            return
        }
        addUserPresentRecord()
    }

    /**
     * 将数据保存到缓存目录
     */
    private fun saveUserUsageData2File(startTime: String, endTime: String) {
        if (mPowerDataLoader.isStarted()) {
            mPowerDataLoader.stop()
        }
        val powerDataWithTitle = ArrayList(mPowerDataLoader.getDataWithTitle())
        val appUsageData = ArrayList(mUserUsageData)
        DataSaver.saveAppUsageDataSync(mContext, appUsageData, powerDataWithTitle, startTime, endTime, true)
        resetAfterDataSaved()
    }

    /**
     * 单次 Session 未结束，但 PowerData 缓存已经超出阈值，
     * 先刷新一次内存到文件中，释放内存。
     */
    private fun saveTempUserUsageData2File() {
        val screenOnTime = mScreenOnRecord?.mOccTime ?: ""
        val powerDataWithTitle = ArrayList(mPowerDataLoader.getDataWithTitle())
        mPowerDataLoader.clearData()
        DataSaver.saveAppUsageDataSync(mContext, null, powerDataWithTitle, screenOnTime, "", false)
    }

    /**
     * 检查是否将手机充电记录保存到文件中，满足下面两个条件之一即缓存：
     * 1. Loader is about to be destroyed
     * 2. [mChargeData] cache size exceeds the threshold
     */
    private fun checkIfSavePhoneChargeData2File(destroyed: Boolean) {
        if (destroyed || mChargeData.size >= IOnDataEnough.ThreshLength.THRESH_FOR_CHARGE.length) {
            val chargeData = ArrayList(mChargeData)
            mChargeData.clear()
            DataSaver.savePhoneChargeDataASync(mContext, chargeData)
        }
    }

    /**
     * 内存优化紧张时，进行更保守的数据采集策略
     *
     * level 1: TRIM_MEMORY_RUNNING_MODERATE：内存不足(后台进程超过5个)，立刻缓存一次PowerData数据到本地，清一次内存
     * level 2: TRIM_MEMORY_RUNNING_CRITICAL：内存不足(后台进程不足3个)，在 level 1 级别上，降低数据采样率
     *
     */
    fun onTrimMemory(level: Int) {
        JLog.d(TAG, "onTrimMemory level = $level")
        when (level) {
            TRIM_MEMORY_MODERATE -> {
                JLog.d(TAG, "TRIM_MEMORY_MODERATE")
                saveTempUserUsageData2File()
            }
            TRIM_MEMORY_COMPLETE -> {
                JLog.d(TAG, "TRIM_MEMORY_COMPLETE")
                saveTempUserUsageData2File()
//                mPowerDataLoader.startInInternal(1000)
            }
            TRIM_MEMORY_BACKGROUND -> {
                JLog.d(TAG, "TRIM_MEMORY_BACKGROUND")
            }
        }
        DataSaver.addDebugTracker(mContext, "onTrimMemory, level = $level")

        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            checkIfSavePhoneChargeData2File(true)
            saveTempUserUsageData2File()
            DataSaver.flushTestData(mContext)
        }
    }

    /**
     * 用户一次交互Session监听
     */
    interface IOnUserSessionListener {
        fun onSessionBegin()
        fun onSessionEnd(session: UsageRecord.SingleSessionRecord)
    }
}