package com.jamgu.hwstatistics.appusage

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/12/10 14:33
 *
 * @description 用戶使用行為記錄
 */
sealed class AppUsageRecord {

    /**
     * 用户使用不同app的行为记录
     * @param mPackageName app 包名
     * @param mClassName   app 下不同的类名
     * @param mTimeStamp   使用行为发生的时间节点
     */
    data class ActivityResumeRecord(val mPackageName: String, val mClassName: String, val mTimeStamp: String): AppUsageRecord()

    /**
     * 用户使用手机的整体记录
     * @param mUsageName 用户行为名称
     * @param mStartTime 行为开始时间
     * @param mEndTime   行为结束时间
     * @param mDuration  持续时长
     */
    data class UsageRecord @JvmOverloads constructor(val mUsageName: String, val detailUsage: String? = null,
                                                     val mStartTime: String, val mEndTime: String,
                                                     val mDuration: String, val mDurationLong: Long): AppUsageRecord()

}