package com.jamgu.hwstatistics.upload

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.core.content.FileProvider
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.appusage.UsageRecord
import com.jamgu.hwstatistics.power.IOnDataEnough
import com.jamgu.hwstatistics.util.ExcelUtil
import com.jamgu.hwstatistics.util.getDateOfTodayString
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

    private const val TAG = "DataSaver"
    private const val FILE_PROVIDER_AUTHORITY = "com.jamgu.hwstatistics"
    private const val CACHE_ROOT_DIR = "HWStatistics"
    private const val ACTIVE_DIR = "active"
    private const val CHARGE_RECORD_DIR = "charge_record"
    private const val TEST_RECORD_DIR = "test_record"
    private const val APP_USAGE_FILE = "app_usage"
    private const val POWER_USAGE_FILE = "power_usage"
    private const val CHARGE_USAGE_FILE = "charge_usage"
    private const val TEST_FILE = "test_only"
    private const val EXCEL_SUFFIX = ".xlsx"

    // 保存测试数据
    private val mTestData: MutableList<UsageRecord> = Collections.synchronizedList(ArrayList(30))

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
    fun saveAppUsageDataASync(
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
                } else {
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
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, appUsageFile)
                saveData2DestFile(context, usageAnyData, appUsageUri)
            }

            if (!powerData.isNullOrEmpty()) {
                val powerUsageFile =
                    File("${dirFile.path}/${POWER_USAGE_FILE}_$timeMillis$EXCEL_SUFFIX")
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

            val dirFile = File(getChargeDataCachePath())
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }

            if (chargeData.isNotEmpty()) {
                val timeMillis = System.currentTimeMillis()
                val chargeRecordFile = File("${dirFile.path}/${CHARGE_USAGE_FILE}_${timeMillis}$EXCEL_SUFFIX")
                val chargeUsageUri =
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, chargeRecordFile)
                ExcelUtil.writeExcelNew(context, chargeAnyData, chargeUsageUri)
            }
        }
    }

    /**
     * 保存测试数据
     */
    fun saveTestData(context: Context, testData: ArrayList<UsageRecord>?) {
        testData ?: return
        ThreadPool.runIOTask {
            val testAnyData = ArrayList<ArrayList<Any>>()

            testData.forEach { chargeRecord ->
                val singleData = chargeRecord.toAnyData() ?: return@forEach
                testAnyData.add(singleData)
            }

            val dirFile = File(getTestDataCachePath())
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }

            if (testData.isNotEmpty()) {
                val timeMillis = System.currentTimeMillis()
                val testRecordFile = File("${dirFile.path}/${TEST_FILE}_${timeMillis}$EXCEL_SUFFIX")
                val testUsageUri =
                    FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, testRecordFile)
                ExcelUtil.writeExcelNew(context, testAnyData, testUsageUri)
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

    private fun getTestDataCachePath() = "${getCacheRootPath()}/$TEST_RECORD_DIR"

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

    fun addTestTracker(context: Context, track: String) {
        mTestData.add(UsageRecord.TestRecord(LinkedHashMap<String, String>().apply {
            this["tracker"] = track
        }))

        checkIfSaveTestData2File(context, false)
    }

    /**
     * 添加一条测试记录，每次更新检查是否需要将数据缓存到文件。
     */
    fun addTestTracker(context: Context, records: LinkedHashMap<String, String>) {
        mTestData.add(UsageRecord.TestRecord(records))

        checkIfSaveTestData2File(context, false)
    }

    /**
     * 检查是否将程序测试数据保存到文件中
     */
    private fun checkIfSaveTestData2File(context: Context, destroyed: Boolean) {
        if (destroyed || mTestData.size >= IOnDataEnough.ThreshLength.THRESH_FOR_TEST.length) {
            val testData = ArrayList(mTestData)
            mTestData.clear()
            saveTestData(context, testData)
        }
    }

    @JvmStatic
    fun flushTestData(context: Context) {
        checkIfSaveTestData2File(context, true)
    }

}