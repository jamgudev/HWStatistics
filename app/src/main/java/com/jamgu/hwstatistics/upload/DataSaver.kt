package com.jamgu.hwstatistics.upload

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.core.content.FileProvider
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
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
    const val DIR_NAME = "HWStatistics"

    fun save(context: Context, data: ArrayList<ArrayList<Any>>) {
        val sdPath = getSDPath()
        val dirPath = "$sdPath/$DIR_NAME"
        val dirFile = File(dirPath)
        if (!dirFile.exists()) {
            val mkdir = dirFile.mkdir()
            JLog.d(TAG, "dir mkdir = $mkdir")
        }

        val timeMillis = getCurrentTimeFormatted()
        val filePath = "${dirFile.path}/$timeMillis.xlsx"
        val destFile = File(filePath)
        val uri = FileProvider.getUriForFile(context, "com.jamgu.hwstatistics", destFile)
        ThreadPool.runOnNonUIThread {
            JLog.d(TAG, "uri = $uri")
            ExcelUtil.writeExcelNew(context, data, uri)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        return sdf.format(Date(System.currentTimeMillis())).toString()
    }

    fun getSDPath(): String {
        var sdDir: File? = null
        val sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS) // 获取跟目录
        } else {
            sdDir = Environment.getDataDirectory()
        }
        return sdDir.toString()
    }

}