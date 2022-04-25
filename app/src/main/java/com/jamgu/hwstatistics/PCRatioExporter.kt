package com.jamgu.hwstatistics

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.util.ExcelUtil
import com.jamgu.hwstatistics.util.roundToDecimals
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "PCRatioExporter"
private val params = floatArrayOf(
    1.4882f, 0.1428f, 0.0995f, 0.0135f, 0.0272f,
    0.0183f, 0.0178f, 0.0063f, 0.0029f, 0.0085f,
    0.0126f, 0.0267f, 0.0275f, 0.0281f, 0.0276f,
    0.0740f, 0.0210f, 0.0621f, 0f, 0.0572f,
    0.0779f, 0.0801f, 0.0242f, 0.0347f, 0.0036f,
    0.0065f, 0.0517f, 0.0148f,
)


/**
 * Created by jamgu on 2022/01/17
 *
 * Power Consumption Ratio Exporter
 * 输出 app 各部件功耗占比 为 xlsx 文件
 */
class PCRatioExporter(var hasRealPc: Boolean) {

    fun verifyAndExport(context: Context?, uri: Uri?) {
        if (context == null || uri == null) return

        val path = uri.path
        if (path.isNullOrEmpty()) {
            return
        }

        val fileName = path.subSequence(path.lastIndexOf("/") + 1,
            path.lastIndexOf("."))

        ThreadPool.runOnNonUIThread {
            val dataList = ExcelUtil.readExcelNewLineByLine(context, uri, "$fileName.xlsx")
                ?: return@runOnNonUIThread
            JLog.d(TAG, "dataList = $dataList, size = ${dataList.size}")

            if (dataList.size < 2) {
                return@runOnNonUIThread
            }

            // 把表头取出
            val headRows = getHeadRowAndDump(dataList)

            val exportList = computePCTofEachHW(dataList) ?: return@runOnNonUIThread

            // 再标准化，算出模型预测功耗
            featureNormalize(dataList)
            // 将功耗数据补上
            computeActualPC(dataList)?.let {
                exportList.forEachIndexed { idx, subList ->
                    subList.addAll(it[idx])
                }
            }

            // 最后加上表头
            headRows?.reverse()
            headRows?.forEach { headRow ->
                exportList.add(0, headRow)
            }

            // 导出文件
            val fileDir = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}"
            val filePath = "$fileDir/${fileName}_export.xlsx"
            val filePathUri = Uri.fromFile(File(filePath))
            if (exportList.size != 0) {
                ExcelUtil.writeExcelNew(context, exportList, filePathUri)
            }
        }
    }

    /**
     * 把表头从数据集中取出，并会从数据集中把表头删去
     * @param dataList 从文件读出来的数据集
     * @return ArrayList<ArrayList<Any>>? 取出表头，可能有多行
     */
    private fun getHeadRowAndDump(dataList: MutableList<MutableMap<Int, Any>>?): ArrayList<ArrayList<Any>>? {
        if (dataList == null || dataList.isEmpty()) return null

        var lastHeadRowNum = -1
        val headRows = ArrayList<ArrayList<Any>>()
        run breaking@{
            dataList.forEachIndexed { idx, map ->
                val collectedVal = map[0] as? String
                try {
                    collectedVal?.toFloat()
                    return@breaking
                } catch (e: NumberFormatException) {
                    val headRow = ArrayList<Any>()
                    val size = dataList[idx].size
                    (0 until size).forEach {
                        val headName = dataList[idx][it].toString()
                        if (headName != "avg_p" || hasRealPc) {
                            headRow.add(it, headName)
                        }
                    }
                    headRows.add(headRow)
                    lastHeadRowNum = idx
                }
            }
        }

        // 从数据集中删除表头
        for (i in 0..lastHeadRowNum) {
            dataList.removeFirst()
        }

        if (lastHeadRowNum >= 0) {
            // 补充表头
            val extraHead = arrayListOf(
                "exp_p", "p_error"
            )
            headRows[lastHeadRowNum].addAll(extraHead)
        }

        return headRows
    }

    /**
     * @param dataList 从文件读出来的数据集
     * @return ArrayList<ArrayList<Any>> 一个包含每条数据各部分硬件功耗的二维列表
     */
    private fun computePCTofEachHW(dataList: List<MutableMap<Int, Any>>?): ArrayList<ArrayList<Any>>? {
        val exportList = ArrayList<ArrayList<Any>>()
        dataList?.forEachIndexed { _, map ->
            val size = map.size

            val listInRow = ArrayList<Float>()
            // 先加基础功耗
            var estimatedPC = params[0]
            for (i in 0 until size) {
                val collectedVal = map[i] as? String
                // 忽略最后一列数据
                if (i == size - 1 && hasRealPc) break
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
            } as? ArrayList ?: return null
            listInRow.clear()

            // 最后把总功耗加上，
            // mappedArray.add(estimatedPC)

//                Log.d(TAG, "estimatedPC = $estimatedPC, listInRow = $mappedArray")
            exportList.add(mappedArray)

        }
        return exportList
    }

    /**
     * @param dataList 从文件读出来的数据集
     * @return ArrayList<FloatArray> 每行的预测功耗，真实功耗，预测误差
     */
    private fun computeActualPC(dataList: List<MutableMap<Int, Any>>?): ArrayList<ArrayList<Float>>? {
        dataList ?: return null

        val realEstimatedPC = ArrayList<ArrayList<Float>>()
        for (i in dataList.indices) {
            var estimatedPc = params[0]
            var realPc = -1f
            val size = dataList[i].size
            for (j in 0 until size) {
                val collectedVal = dataList[i][j] as? String
                if (collectedVal != null) {
                    if (j == size - 1 && hasRealPc) {
                        // 拿到真实功耗
                        realPc = collectedVal.toFloat()
                        break
                    }

                    val paramsValue = params[j + 1]
                    // 加权每个部件预测功耗
                    estimatedPc += collectedVal.toFloat().times(paramsValue)
                }
            }

            // 计算预测误差
            val error = if (realPc != -1f) {
                (realPc - estimatedPc).absoluteValue / realPc
            } else 0f
            if (hasRealPc) {
                realEstimatedPC.add(arrayListOf(realPc, estimatedPc, error))
            } else {
                realEstimatedPC.add(arrayListOf(estimatedPc))
            }
        }
        return realEstimatedPC
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
            // 最后的功耗数据不需要标准化
            if (j == yAxis - 1 && hasRealPc) {
                break
            }
            // 均值
            var mu = 0f
            // 标准差
            var sigma = 0f
            var total = 0f
            // 一列中最大的值
            var maxVal = 0f
            val yAxisDataArray = ArrayList<Float>()
            // 跳过表头
            for (i in 0 until xAxis) {
                val yVal =
                    dataList[i][j] as? String ?: throw Throwable("not String, type: ${dataList[i][j]?.javaClass}")
                val fYVal = yVal.toFloat()
                yAxisDataArray.add(fYVal)

                maxVal = maxVal.coerceAtLeast(fYVal)
                total += fYVal
            }

            mu = total / (yAxisDataArray.size)
            yAxisDataArray.forEach {
                sigma += (it - mu).pow(2)
            }
            sigma = sqrt(sigma / yAxisDataArray.size)

            // 一列数值都相同，可能存在标注差为 0 的情况
            if (sigma == 0f) {
                mu = 0f
                sigma = maxVal
            }
            means.add(mu)
            sigmas.add(sigma)

            // 标准化数据
            for (i in 0 until xAxis) {
                val yVal = dataList[i][j] as? String ?: return
                if (sigma != 0f) {
                    dataList[i][j] = ((yVal.toFloat() - mu) / sigma).toString()
                } else {
//                        JLog.d(TAG, "($i, $j), no need to normalize = ${dataList[i][j]}")
                }
            }
        }
    }
}