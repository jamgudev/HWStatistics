package com.jamgu.hwstatistics.net.upload

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.core.content.FileProvider
import com.jamgu.common.Common
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.appusage.UsageRecord
import com.jamgu.hwstatistics.power.IOnDataEnough
import com.jamgu.hwstatistics.util.ExcelUtil
import com.jamgu.hwstatistics.util.getDateOfTodayString
import com.jamgu.hwstatistics.util.timeStamp2DateString
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * @author jamgudev
 * @date 2022/11/14 8:06 下午
 *
 * @description 保存数据至缓存目录
 */
object DataSaver {

    const val ACTIVE_DIR = "active"
    const val CACHE_ROOT_DIR = "HWStatistics"
    const val DEBUG_RECORD_DIR = "debug_record"
    const val INFO_RECORD_DIR = "info_record"
    const val SESSION_TEMP_SUFFIX = "#temp"
    const val SESSION_DIR_INFIX = "$$"
    const val SESSION_FILE_PREFIX = "session"
    const val TAG_SCREEN_OFF = "tag_screen_off"
    private const val TAG = "DataSaver"
    private const val FILE_PROVIDER_AUTHORITY = "com.jamgu.hwstatistics"
    private const val CHARGE_RECORD_DIR = "charge_record"
    private const val APP_USAGE_FILE = "${SESSION_FILE_PREFIX}_app_usage"
    private const val POWER_USAGE_FILE_PREFIX = "${SESSION_FILE_PREFIX}_power_usage"
    private const val CHARGE_USAGE_FILE_PREFIX = "charge_usage"
    private const val DEBUG_FILE_PREFIX = "DEBUG"
    private const val INFO_FILE_PREFIX = "INFO"
    const val EXCEL_SUFFIX = ".xlsx"

    // 保存测试数据
    private val mDebugData: MutableList<UsageRecord> = Collections.synchronizedList(ArrayList(30))

    // 保存致命的报错数据
    private val mInfoData: MutableList<UsageRecord> = Collections.synchronizedList(ArrayList(4))

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
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, destFile)
            JLog.d(TAG, "uri = $uri")
            ExcelUtil.writeExcelNew(context, data, uri)
        }
    }

    /**
     * 保存用户行为数据
     */
    fun saveAppUsageDataSync(
        context: Context,
        usageData: ArrayList<UsageRecord>?,
        powerData: ArrayList<ArrayList<Any>>?,
        startTime: String,
        endTime: String? = "",
        isSessionFinish: Boolean
    ) {
        if (startTime.isEmpty()) {
            return
        }

        ThreadPool.runIOTask {
            val usageAnyData = ArrayList<ArrayList<Any>>()
            usageData?.forEach { usageRecord ->
                val singleData = usageRecord.toAnyData() ?: return@forEach
                usageAnyData.add(singleData)
            }

            val nameSplits = startTime.split("(")
            // date of today
            val subDirName = if (nameSplits.isNotEmpty()) {
                nameSplits[0]
            } else {
                getDateOfTodayString()
            }

            var dirFile: File
            val tempPath = "${getActiveCachePath()}/$subDirName/$startTime$SESSION_TEMP_SUFFIX"
            if (isSessionFinish) {
                val tempFile = File(tempPath)
                // 之前已经上传了部分power data，rename
                if (tempFile.exists()) {
                    val suffixIndex = tempPath.lastIndexOf(SESSION_TEMP_SUFFIX)
                    if (suffixIndex < 0) {
                        JLog.e(TAG, "file = $tempPath, suffixIndex < 0, continue")
                        addDebugTracker(TAG, "file = $tempPath, suffixIndex < 0, continue")
                        return@runIOTask
                    }
                    // 把临时文件后缀去掉
                    val finishPath = tempPath.substring(0, suffixIndex)
                    val finishFileName = "${finishPath}$SESSION_DIR_INFIX$endTime"
                    val finishFile = File(finishFileName)
                    if (!tempFile.renameTo(finishFile)) {
                        JLog.e(TAG, "file = ${tempFile.path} rename to path{$finishFileName} failed.")
                        addInfoTracker(TAG, "file = ${tempFile.path} rename to path{$finishFileName} failed.")
                        return@runIOTask
                    } else {
                        // rename 成功，更改目录File
                        dirFile = finishFile
                    }
                } else {
                    // session时长低于power data分段保存的阈值，直接保存
                    dirFile = File("${getActiveCachePath()}/$subDirName/$startTime$SESSION_DIR_INFIX$endTime")
                    if (!dirFile.exists()) {
                        dirFile.mkdirs()
                    }
                }
            } else {
                // 创建临时保存目录
                dirFile = File(tempPath)
                if (!dirFile.exists()) {
                    dirFile.mkdirs()
                }
            }

            JLog.d(TAG, "saveAppUsageDataSync, dirFile.path = ${dirFile.path}")
            val timeMillis = System.currentTimeMillis().timeStamp2DateString()
            if (usageAnyData.isNotEmpty()) {
                val appUsageFile = File("${dirFile.path}/${APP_USAGE_FILE}_$timeMillis$EXCEL_SUFFIX")
                val appUsageUri =
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, appUsageFile)
                saveData2DestFile(context, usageAnyData, appUsageUri)
            }

            if (!powerData.isNullOrEmpty()) {
                val powerUsageFile =
                    File("${dirFile.path}/${POWER_USAGE_FILE_PREFIX}_$timeMillis$EXCEL_SUFFIX")
                val powerUsageUri =
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, powerUsageFile)
                saveData2DestFile(context, powerData, powerUsageUri)
            }
        }
    }

    /**
     * 保存手机充电记录
     */
    fun savePhoneChargeDataASync(context: Context, chargeData: ArrayList<UsageRecord>?) {
        chargeData ?: return
        ThreadPool.runIOTask {
            val chargeAnyData = ArrayList<ArrayList<Any>>()

            chargeData.forEach { chargeRecord ->
                val singleData = chargeRecord.toAnyData() ?: return@forEach
                chargeAnyData.add(singleData)
            }

            val dirFile = File("${getChargeDataCachePath()}/${getDateOfTodayString()}")
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }

            if (chargeData.isNotEmpty()) {
                val timeMillis = System.currentTimeMillis().timeStamp2DateString()
                val chargeRecordFile = File("${dirFile.path}/${CHARGE_USAGE_FILE_PREFIX}_${timeMillis}$EXCEL_SUFFIX")
                val chargeUsageUri =
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, chargeRecordFile)
                ExcelUtil.writeExcelNew(context, chargeAnyData, chargeUsageUri)
            }
        }
    }

    /**
     * 保存测试数据
     */
    fun saveDebugData(testData: ArrayList<UsageRecord>?) {
        testData ?: return
        ThreadPool.runIOTask {
            val testAnyData = ArrayList<ArrayList<Any>>()

            testData.forEach { chargeRecord ->
                val singleData = chargeRecord.toAnyData() ?: return@forEach
                testAnyData.add(singleData)
            }

            val dirFile = File("${getTestDataCachePath()}/${getDateOfTodayString()}")
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }

            val context = Common.getInstance().getApplicationContext()
            if (testData.isNotEmpty()) {
                val timeMillis = System.currentTimeMillis().timeStamp2DateString()
                val testRecordFile = File("${dirFile.path}/${DEBUG_FILE_PREFIX}_${timeMillis}$EXCEL_SUFFIX")
                val testUsageUri =
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, testRecordFile)
                ExcelUtil.writeExcelNew(context, testAnyData, testUsageUri)
            }
        }
    }

    /**
     * 保存测试数据
     */
    fun saveInfoData(errorData: ArrayList<UsageRecord>?) {
        errorData ?: return
        ThreadPool.runIOTask {
            val errorAnyData = ArrayList<ArrayList<Any>>()

            errorData.forEach { chargeRecord ->
                val singleData = chargeRecord.toAnyData() ?: return@forEach
                errorAnyData.add(singleData)
            }

            val context = Common.getInstance().getApplicationContext()
            val dirFile = File("${getINFODataCachePath()}/${getDateOfTodayString()}")
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }

            if (errorData.isNotEmpty()) {
                val dateStr = System.currentTimeMillis().timeStamp2DateString()
                val errorRecordFile = File("${dirFile.path}/${INFO_FILE_PREFIX}_${dateStr}$EXCEL_SUFFIX")
                val errorUsageUri =
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, errorRecordFile)
                ExcelUtil.writeExcelNew(context, errorAnyData, errorUsageUri)
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

    fun getCacheRootPath() = "${getSDPath()}/$CACHE_ROOT_DIR"

    private fun getActiveCachePath() = "${getCacheRootPath()}/$ACTIVE_DIR"

    private fun getChargeDataCachePath() = "${getCacheRootPath()}/$CHARGE_RECORD_DIR"

    fun getTestDataCachePath() = "${getCacheRootPath()}/$DEBUG_RECORD_DIR"

    fun getINFODataCachePath() = "${getCacheRootPath()}/$INFO_RECORD_DIR"

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

    fun addDebugTracker(tag: String, track: String) {
        mDebugData.add(UsageRecord.TestRecord(LinkedHashMap<String, String>().apply {
            this[tag] = track
        }))

        checkIfSaveDebugData2File(false)
    }

    fun addInfoTracker(tag: String, track: String) {
        mInfoData.add(UsageRecord.TestRecord(LinkedHashMap<String, String>().apply {
            this[tag] = track
        }))

        checkIfSaveInfoData2File()
    }

    /**
     * 添加一条测试记录，每次更新检查是否需要将数据缓存到文件。
     */
    fun addDebugTracker(records: LinkedHashMap<String, String>) {
        mDebugData.add(UsageRecord.TestRecord(records))

        checkIfSaveDebugData2File(false)
    }

    /**
     * 检查是否将程序测试数据保存到文件中
     */
    private fun checkIfSaveDebugData2File(flushImmediately: Boolean) {
        if (flushImmediately || mDebugData.size >= IOnDataEnough.ThreshLength.THRESH_FOR_TRACKER.length) {
            val testData = ArrayList(mDebugData)
            mDebugData.clear()
            saveDebugData(testData)
        }
    }

    /**
     * 检查是否将程序测试数据保存到文件中
     */
    private fun checkIfSaveInfoData2File() {
        if (mInfoData.size >= IOnDataEnough.ThreshLength.THRESH_FOR_ERROR.length) {
            val testData = ArrayList(mInfoData)
            saveInfoData(testData)
            mInfoData.clear()
        }
    }

    @JvmStatic
    fun flushTestData() {
        checkIfSaveDebugData2File(true)
    }

}