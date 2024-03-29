package com.jamgu.hwstatistics.appusage

import com.jamgu.common.Common
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.util.timeStamp2DateStringWithMills

/**
 * @author jamgudev
 * @date 2022/12/10 14:33
 *
 * @description 用戶使用行為記錄
 */
sealed class UsageRecord {

    abstract fun toAnyData(): ArrayList<Any>?

    /**
     * 用户使用不同app的行为记录，一个app使用完后，记录会被替换为 [AppUsageRecord]
     * @param mPackageName app 包名
     * @param mClassName   app 下不同的类名
     * @param mTimeStamp   使用行为发生的时间节点
     */
    data class ActivityResumeRecord(
        val mPackageName: String,
        val mClassName: String,
        val mTimeStamp: String
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                add(mPackageName)
                add(mClassName)
                add(mTimeStamp)
                add(
                    Common.getInstance().getApplicationContext()
                        .getString(R.string.err_activity_usage_record_failed)
                )
            }
        }
    }

    data class ActivityPauseRecord(
        val mPackageName: String,
        val mClassName: String,
        val mTimeStamp: String
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any>? {
            return null
        }
    }

    /**
     * 用户使用手机的整体记录
     * @param mUsageName 用户行为名称
     * @param mDetailUsage 行为开始时间
     * @param mEndTime   行为结束时间
     * @param mDuration  持续时长
     */
    data class AppUsageRecord @JvmOverloads constructor(
        val mUsageName: String, val mDetailUsage: String? = null,
        val mStartTime: String, val mEndTime: String,
        val mDuration: String, val mDurationLong: Long = 0
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                add(mUsageName)
                add(mDetailUsage ?: "")
                add(mStartTime)
                add(mEndTime)
                add(mDuration)
                add(mDurationLong)
            }
        }
    }

    /**
     * 一个Session内，用户在某个App的总使用时长
     */
    data class SingleAppUsageRecord(
        val mAppName: String,
        var mDuration: String, var mDurationLong: Long
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                add(mAppName)
                add(mDuration)
                add(mDurationLong)
            }
        }
    }

    /**
     * 用户使用手机生命周期记录：手机启动 -> 屏幕亮起 -> 用户解锁 -> 屏幕熄灭 -> 手机关机。
     * 一次Session由屏幕亮起开始，到屏幕熄灭结束
     * @param mLifeCycleName 生命周期名称
     * @param mOccTime       发生时间
     */
    data class PhoneLifeCycleRecord(val mLifeCycleName: String, val mOccTime: String) :
        UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                add(mLifeCycleName)
                add(mOccTime)
            }
        }
    }

    /**
     * 手机充电和取消充电记录
     * 充电和取消充电记录是跨Session的，可以同时出现在同一个SessionRecord中，也可以出现在不同的SessionRecord。
     * 这取决于用户使用习惯，因此单独存储。
     */
    data class PhoneChargeRecord(
        val mEventName: String,
        val mOccTime: String,
        val curBatteryState: String
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                add(mEventName)
                add(mOccTime)
                add(curBatteryState)
            }
        }
    }

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
        val mUserPresentTime: String = "", val mScreenOfTime: String,
        val mScreenSession: Long, val mPresentSession: Long = 0,
        val mActivitySession: Long = 0
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                add(mUsageName)
                add(mScreenOnTime)
                add(mUserPresentTime.ifEmpty {
                    Common.getInstance().getApplicationContext()
                        .getString(R.string.usage_un_present)
                })
                add(mScreenOfTime)
                add(mScreenSession)
                add(mPresentSession)
                add(mActivitySession)
            }
        }
    }

    /**
     * 空的使用记录，用来表示空行
     */
    data class EmptyUsageRecord(val line: String = "") : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                add("")
            }
        }
    }

    /**
     * 用来表示标题
     */
    data class TextTitleRecord(val titles: Array<String>) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                titles.forEach { title ->
                    add(title)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TextTitleRecord

            if (!titles.contentEquals(other.titles)) return false

            return true
        }

        override fun hashCode(): Int {
            return titles.contentHashCode()
        }
    }

    /**
     * 测试记录
     */
    data class TestRecord(
        val records: LinkedHashMap<String, String>,
        val mRecordTime: String = System.currentTimeMillis().timeStamp2DateStringWithMills()
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                this.add(mRecordTime)
                records.forEach { entry ->
                    this.add(entry.key)
                    this.add(entry.value)
                }
            }
        }
    }

    /**
     * 用来记录 App 发生的错误
     */
    data class ErrorRecord(
        val records: LinkedHashMap<String, String>,
        val mRecordTime: String = System.currentTimeMillis().timeStamp2DateStringWithMills()
    ) : UsageRecord() {
        override fun toAnyData(): ArrayList<Any> {
            return ArrayList<Any>().apply {
                this.add(mRecordTime)
                records.forEach { entry ->
                    this.add(entry.key)
                    this.add(entry.value)
                }
            }
        }
    }

}