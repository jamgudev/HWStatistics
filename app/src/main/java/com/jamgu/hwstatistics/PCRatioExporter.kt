package com.jamgu.hwstatistics

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.jamgu.hwstatistics.util.ExcelUtil
import com.jamgu.hwstatistics.util.roundToDecimals
import com.jamgu.hwstatistics.util.thread.ThreadPool
import java.io.File

/**
 * Created by jamgu on 2022/01/17
 *
 * Power Consumption Ratio Exporter
 * 输出 app 各部件功耗占比 为 xlsx 文件
 */
class PCRatioExporter {
    companion object {
        private const val TAG = "VerifyTest"

        fun verifyAndExport(context: Context?) {
            context ?: return

            ThreadPool.runOnNonUIThread {
                val params = floatArrayOf(
                    1.4488f, 0f, 0f, 0.1547f, 0.0631f,
                    0f, 0f, 0f, 0f, 0f, 0.0297f,
                    0.0118f, 0.0098f, 0.0181f, 0.0236f, 0.0155f,
                    0.0175f, 0.0182f, 0.0167f, 0.0556f, 0.0215f,
                    0.0615f, 0.0412f, 0.0297f, 0.0666f, 0.0617f,
                    0.0563f, 0.0568f, 0f, -0.0026f, 0.0146f,
                    0.0088f, 0.0012f, -0.006f, 0.0058f, 0.0184f
                )
//            val fileName = "verify_all"
                val fileName = "01181932_bb"
                val fileDir = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}"
                val filePath = "$fileDir/$fileName.xlsx"
                val file = File(filePath)
                if (!file.exists()) {
                    Log.e(TAG, "file not exist!")
                    return@runOnNonUIThread
                }

                val filePathUri = Uri.fromFile(File(filePath))
                Log.d(TAG, "filePathUri = $filePathUri")
                val dataList = ExcelUtil.readExcelNew(context, filePathUri, "$fileName.xlsx") ?: return@runOnNonUIThread
                Log.d(TAG, "dataList = $dataList, size = ${dataList.size}")

                if (dataList.size < 2) {
                    return@runOnNonUIThread
                }
                val exportList = ArrayList<ArrayList<Any>>()

                // 遍历数据
                dataList.forEachIndexed { idx, map ->
                    val size = map.size

                    // 第一行数据为表头，直接添加
                    if (idx == 0) {
                        val headRow = ArrayList<Any>()
                        (0 until size).forEach {
                            headRow.add(it, map[it].toString())
                        }
                        exportList.add(headRow)
                        return@forEachIndexed
                    }

                    val listInRow = ArrayList<Float>()
                    // 先加基础功耗
                    var estimatedPC = params[0]
                    for (i in 0 until size) {
                        val collectedVal = map[i] as? String
                        if (i == size - 1) break
                        if (collectedVal != null) {
                            val paramsValue = params[i + 1]
                            // 计算单个预测功耗
                            listInRow.add(collectedVal.toFloat().times(paramsValue))
                            // 计算总预测功耗
                            estimatedPC += listInRow[i]
                        }
                    }

                    // 计算功耗百分比
                    val mappedArray = listInRow.map {
                        @Suppress("USELESS_CAST")
                        (it / estimatedPC * 100).roundToDecimals(2) as Any
                    } as? ArrayList ?: return@runOnNonUIThread
                    listInRow.clear()

                    // 最后把总功耗加上(未正则化)
                    mappedArray.add(estimatedPC)

                    Log.d(TAG, "estimatedPC = $estimatedPC, listInRow = $mappedArray")
                    exportList.add(mappedArray)
                }

                // 导出文件
                if (exportList.size != 0) {
                    val exportUri = Uri.fromFile(File("$fileDir/${fileName}_export.xlsx"))
                    ExcelUtil.writeExcelNew(context, exportList, exportUri)
                }
            }
        }
    }
}