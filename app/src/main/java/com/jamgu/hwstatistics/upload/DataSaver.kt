package com.jamgu.hwstatistics.upload

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.core.content.FileProvider
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.appusage.UsageRecord
import com.jamgu.hwstatistics.appusage.getDateOfTodayString
import com.jamgu.hwstatistics.util.ExcelUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/11/14 8:06 下午
 *
 * @description 保存数据至缓存目录
 */
object DataSaver {

    private const val TAG = "DataSaver"
    private const val CACHE_ROOT_DIR = "HWStatistics"
    private const val ACTIVE_DIR = "active"
    private const val APP_USAGE_FILE = "app_usage_file"
    private const val POWER_USAGE_FILE = "power_usage_file"
    private const val EXCEL_SUFFIX = ".xlsx"

    /**
     * 保存功耗模型数据
     */
    fun savePowerData(context: Context, data: ArrayList<ArrayList<Any>>) {
        ThreadPool.runOnNonUIThread {
            val dirPath = getCacheRootPath()
            val dirFile = File(dirPath)
            if (!dirFile.exists()) {
                val mkdir = dirFile.mkdir()
                JLog.d(TAG, "dir mkdir = $mkdir")
            }

            val timeMillis = getCurrentTimeFormatted()
            val filePath = "${dirFile.path}/$timeMillis.xlsx"
            val destFile = File(filePath)
            val uri = FileProvider.getUriForFile(context, "com.jamgu.hwstatistics", destFile)
            JLog.d(TAG, "uri = $uri")
            ExcelUtil.writeExcelNew(context, data, uri)
        }
    }

    /**
     * 保存用户行为数据
     */
    fun saveAppUsageDataASync(
        context: Context,
        usageData: ArrayList<UsageRecord>?,
        powerData: ArrayList<ArrayList<Any>>?,
        startTime: String,
        endTime: String? = "",
        isSessionFinish: Boolean
    ) {
        ThreadPool.runIOTask {
            val usageAnyData = ArrayList<ArrayList<Any>>()
            usageData?.forEach { usageRecord ->
                val singleData = ArrayList<Any>()
                when (usageRecord) {
                    is UsageRecord.AppUsageRecord -> {
                        singleData.add(usageRecord.mUsageName)
                        singleData.add(usageRecord.mDetailUsage ?: "")
                        singleData.add(usageRecord.mStartTime)
                        singleData.add(usageRecord.mEndTime)
                        singleData.add(usageRecord.mDuration)
                        singleData.add(usageRecord.mDurationLong)
                    }
                    is UsageRecord.PhoneLifeCycleRecord -> {
                        singleData.add(usageRecord.mLifeCycleName)
                        singleData.add(usageRecord.mOccTime)
                    }
                    is UsageRecord.SingleSessionRecord -> {
                        singleData.add(usageRecord.mUsageName)
                        singleData.add(usageRecord.mScreenOnTime)
                        singleData.add(
                            usageRecord.mUserPresentTime.ifEmpty { context.getString(R.string.usage_un_present) }
                        )
                        singleData.add(usageRecord.mScreenOfTime)
                        singleData.add(usageRecord.mScreenSession)
                        singleData.add(usageRecord.mPresentSession)
                        singleData.add(usageRecord.mActivitySession)
                    }
                    is UsageRecord.EmptyUsageRecord -> {
                        singleData.add("")
                    }
                    is UsageRecord.ActivityResumeRecord -> {
                        singleData.add(usageRecord.mPackageName)
                        singleData.add(usageRecord.mClassName)
                        singleData.add(usageRecord.mTimeStamp)
                        singleData.add(context.getString(R.string.err_activity_usage_record_failed))
                    }
                    is UsageRecord.TextTitleRecord -> {
                        usageRecord.titles.forEach { title ->
                            singleData.add(title)
                        }
                    }
                    is UsageRecord.SingleAppUsageRecord -> {
                        singleData.add(usageRecord.mAppName)
                        singleData.add(usageRecord.mDuration)
                        singleData.add(usageRecord.mDurationLong)
                    }
                    else -> {}
                }
                usageAnyData.add(singleData)
            }

            val nameSplits = startTime.split("(")
            // date of today
            val subDirName = if (nameSplits.isNotEmpty()) {
                nameSplits[0]
            } else {
                getDateOfTodayString()
            }
            val dirName = "${startTime}_"
            var dirFile = File("${getActiveCachePath()}/$subDirName/$dirName")
            if (isSessionFinish) {
                val finishFileName = "${dirFile.path}$endTime"
                val finishFile = File(finishFileName)
                // 之前已经上传了部分power data，rename
                if (dirFile.exists()) {
                    if (!dirFile.renameTo(finishFile)) {
                        JLog.e(TAG, "file = ${dirFile.path} rename to path{$finishFileName} failed.")
                        return@runIOTask
                    } else {
                        // rename 成功，更改目录File
                        dirFile = finishFile
                    }
                }
                else {
                    // session时长低于power data分段保存的阈值，直接保存
                    dirFile = finishFile
                    if (!dirFile.exists()) {
                        dirFile.mkdirs()
                    }
                }
            } else {
                // 创建临时保存目录
                if (!dirFile.exists()) {
                    dirFile.mkdirs()
                }
            }

            JLog.d(TAG, "dirFile.path = ${dirFile.path}")
            val timeMillis = System.currentTimeMillis()
            if (usageAnyData.isNotEmpty()) {
                val appUsageFile = File("${dirFile.path}/${APP_USAGE_FILE}_$timeMillis$EXCEL_SUFFIX")
                val appUsageUri =
                    FileProvider.getUriForFile(context, "com.jamgu.hwstatistics", appUsageFile)
                saveData2DestFile(context, usageAnyData, appUsageUri)
            }

            if (!powerData.isNullOrEmpty()) {
                val powerUsageFile =
                    File("${dirFile.path}/${POWER_USAGE_FILE}_$timeMillis$EXCEL_SUFFIX")
                val powerUsageUri =
                    FileProvider.getUriForFile(context, "com.jamgu.hwstatistics", powerUsageFile)
                saveData2DestFile(context, powerData, powerUsageUri)
            }
        }
    }


    private fun saveData2DestFile(
        context: Context,
        data: ArrayList<ArrayList<Any>>,
        destFileUri: Uri
    ) {
        ExcelUtil.writeExcelNew(context, data, destFileUri)
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        return sdf.format(Date(System.currentTimeMillis())).toString()
    }

    private fun getCacheRootPath() = "${getSDPath()}/$CACHE_ROOT_DIR"

    private fun getActiveCachePath() = "${getCacheRootPath()}/$ACTIVE_DIR"

    private fun getSDPath(): String {
        val sdDir: File?
        val sdCardExist =
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED // 判断sd卡是否存在
        sdDir = if (sdCardExist) {
            Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS) // 获取跟目录
        } else {
            Environment.getDataDirectory()
        }
        return sdDir.toString()
    }

}