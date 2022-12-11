package com.jamgu.hwstatistics.appusage

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/12/10 14:33
 *
 * @description 用戶使用行為記錄
 */
sealed class AppUsageRecord {

    /**
     * 用户使用不同app的行为记录，一个app使用完后，记录会被替换为 [UsageRecord]
     * @param mPackageName app 包名
     * @param mClassName   app 下不同的类名
     * @param mTimeStamp   使用行为发生的时间节点
     */
    data class ActivityResumeRecord(
        val mPackageName: String,
        val mClassName: String,
        val mTimeStamp: String
    ) : AppUsageRecord()

    /**
     * 用户使用手机的整体记录
     * @param mUsageName 用户行为名称
     * @param mDetailUsage 行为开始时间
     * @param mEndTime   行为结束时间
     * @param mDuration  持续时长
     */
    data class UsageRecord @JvmOverloads constructor(
        val mUsageName: String, val mDetailUsage: String? = null,
        val mStartTime: String, val mEndTime: String,
        val mDuration: String, val mDurationLong: Long = 0
    ) : AppUsageRecord()

    /**
     * 手机生命周期记录：手机启动 -> 屏幕亮起 -> 用户解锁 -> 屏幕熄灭 -> 手机关机
     * @param mLifeCycleName 生命周期名称
     * @param mOccTime       发生时间
     */
    data class PhoneLifeCycleRecord(val mLifeCycleName: String, val mOccTime: String) :
        AppUsageRecord()

    /**
     * 用戶与手机从手机亮起到手机屏幕熄灭的一次session record
     * @param mScreenOnTime 屏幕亮起时间
     * @param mScreenOfTime 屏幕熄灭时间
     * @param mUserPresentTime 用户解锁时间，可能为空
     * @param mScreenSession   屏幕亮起到熄灭过程持续时间
     * @param mPresentSession  用户解锁到屏幕熄灭过程的持续时间，用户未解锁时为0
     */
    data class SingleSessionRecord(
        val mUsageName: String, val mScreenOnTime: String,
        val mUserPresentTime: String? = "", val mScreenOfTime: String,
        val mScreenSession: Long, val mPresentSession: Long = 0L
    ) : AppUsageRecord()

    /**
     * 空的使用记录，用来表示空行
     */
    data class EmptyUsageRecord(val line: String = "") : AppUsageRecord()

}