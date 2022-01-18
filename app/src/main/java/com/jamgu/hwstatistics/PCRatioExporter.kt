package com.jamgu.hwstatistics

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.jamgu.hwstatistics.util.ExcelUtil
import com.jamgu.hwstatistics.util.roundToDecimals
import com.jamgu.hwstatistics.util.thread.ThreadPool
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

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
                    0.2917f, 0.2917f, 0.2917f, 0.1547f, 0.0631f,
                    0f, 0f, 0.2917f, 0f, 0.0297f,
                    0.0118f, 0.0098f, 0.0181f, 0.0236f, 0.0155f,
                    0.0175f, 0.0182f, 0.0167f, 0.0556f, 0.0215f,
                    0.0615f, 0.0412f, 0.0297f, 0.0666f, 0.0617f,
                    0.0563f, 0.0568f, 0.2917f, -0.0026f, 0.0146f,
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
                // 数据格式有限制，最后一列为功耗设备测出的功耗
                // 计算百分比时会略过这一列的数据，但这一列数据得有
                val dataList = ExcelUtil.readExcelNew(context, filePathUri, "$fileName.xlsx")
                    ?: return@runOnNonUIThread
                Log.d(TAG, "dataList = $dataList, size = ${dataList.size}")

                if (dataList.size < 2) {
                    return@runOnNonUIThread
                }
//                featureNormalize(dataList)

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
                        // 忽略最后一列数据
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

                    // 最后把总功耗加上
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

        /**
         * 数据标准化
         */
        private fun featureNormalize(dataList: List<MutableMap<Int, Any>>?) {
            dataList ?: return

            val xAxis = dataList.size
            if (xAxis < 2) return

            val yAxis = dataList[1].size
            if (yAxis <= 0) return

            val means = ArrayList<Float>()
            val sigmas = ArrayList<Float>()

            for (j in 0 until yAxis) {
                // 均值
                var mu = 0f
                // 标准差
                var sigma = 0f
                var total = 0f
                // 一列中最大的值
                var maxVal = 0f
                val yAxisDataArray = ArrayList<Float>()
                // 跳过表头
                for (i in 1 until xAxis) {
                    val yVal = dataList[i][j] as? String ?: throw Throwable("not String, type: ${dataList[i][j]?.javaClass}")
                    val fYVal = yVal.toFloat()
                    yAxisDataArray.add(fYVal)

                    maxVal = maxVal.coerceAtLeast(fYVal)
                    total += fYVal
                }

                mu = total / (yAxisDataArray.size)
                yAxisDataArray.forEach {
                    sigma += (it - mu).pow(2)
                }
//                Log.d(TAG, "sigma = $sigma")
                sigma = sqrt(sigma / yAxisDataArray.size)
//                Log.d(TAG, "sqrt sigma = $sigma")

                // 一列数值都相同，可能存在标注差为 0 的情况
                if (sigma == 0f) {
                    mu = 0f
                    sigma = maxVal
                }
                Log.d(TAG, "sqrt sigma = $sigma")
                means.add(mu)
                sigmas.add(sigma)

                // 标准化数据
                for (i in 1 until xAxis) {
                    val yVal = dataList[i][j] as? String ?: return
                    if (sigma != 0f) {
                        dataList[i][j] = ((yVal.toFloat() - mu) / sigma).toString()
                        Log.d(TAG, "($i, $j), normalized = ${dataList[i][j]}")
                    } else {
                        Log.d(TAG, "($i, $j), no need to normalize = ${dataList[i][j]}")
                    }
                }
            }


            means.forEach {
                Log.d(TAG, "mus = $it")
            }

            sigmas.forEach {
                Log.d(TAG, "sigmas = $it")
            }
        }
    }
}