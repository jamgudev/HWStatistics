package com.jamgu.hwstatistics.pcratio

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.util.ExcelUtil
import java.io.File
import kotlin.math.absoluteValue

private const val TAG = "PCRatioExporter"
const val OUTPUT_FORMAT_RAW = 1
const val OUTPUT_FORMAT_PERCENTAGE = 2

/**
 * Created by jamgu on 2022/01/17
 *
 * Power Consumption Ratio Exporter
 * 输出 app 各部件功耗占比 为 xlsx 文件
 */
class PCRatioExporter(var hasRealPc: Boolean) {
    private var mParamReady = false
    private lateinit var mParams: MutableList<Float>
    private lateinit var mSigma: MutableList<Float>
    private lateinit var mMu: MutableList<Float>
    // 输出格式: 1 百分比，2 原数值，默认输出原数值，即功耗原数值
    private var mOutputFormat: Int = OUTPUT_FORMAT_RAW

    fun initParams(totalParams: ArrayList<MutableList<Float>>? /* = java.util.ArrayList<kotlin.collections.MutableList<kotlin.Float>>? */) {
        if ((totalParams == null || totalParams.size < 3)) {
            return
        }
        // 初始化参数
        mParams = totalParams[0]
        mMu = totalParams[1]
        mSigma = totalParams[2]
        mParamReady = true
    }

    fun setOutPutFormat(format: Int?) {
        mOutputFormat = format ?: OUTPUT_FORMAT_RAW
    }

    /**
     * 耗时方法，需要放到子线程中
     */
    fun verifyAndExport(context: Context?, uri: Uri?) {
        if (context == null || uri == null || !mParamReady) {
            return
        }

        val path = uri.path
        if (path.isNullOrEmpty()) {
            return
        }

        val fileName = path.subSequence(path.lastIndexOf("/") + 1,
            path.lastIndexOf("."))

        val dataList = ExcelUtil.readExcelNewLineByLine(context, uri, "$fileName.xlsx")
            ?: return
        JLog.d(TAG, "dataList = $dataList, size = ${dataList.size}")

        if (dataList.size < 2) {
            return
        }

        // 把表头取出
        val headRows = getHeadRowAndDump(dataList)

        // 标准化
        featureNormalize2(dataList)

        // 计算各部件功耗占比
        val exportList = computePCTofEachHW(dataList) ?: return

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
        val filePath = "$fileDir/${fileName}_export_${System.currentTimeMillis()}.xlsx"
        val filePathUri = Uri.fromFile(File(filePath))
        if (exportList.size != 0) {
            ExcelUtil.writeExcelNew(context, exportList, filePathUri)
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
            // 加上基础表头
            headRows[lastHeadRowNum].add(0, "basic")
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
            var estimatedPC = 0f
            for (i in 0 until size) {
                val collectedVal = map[i] as? String
                // 基础功耗
                if (i == 0) {
                    val basicPower = mParams[0] * 1
                    listInRow.add(basicPower)
                    estimatedPC += basicPower
                }

                // 忽略最后一列数据
                if (i == size - 1 && hasRealPc) break
                if (collectedVal != null) {
                    val paramsValue = mParams[i + 1]
                    // 计算单个预测功耗
                    val elementPower = collectedVal.toFloat().times(paramsValue)
                    listInRow.add(elementPower)
                    // 计算总预测功耗
                    estimatedPC += elementPower
                }
            }

            // 计算功耗百分比
            val mappedArray = listInRow.map {
                @Suppress("USELESS_CAST")
                when(mOutputFormat) {
                    OUTPUT_FORMAT_RAW -> it as Any
                    OUTPUT_FORMAT_PERCENTAGE -> (it / estimatedPC * 100) as Any
                    else -> it as Any
                }
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
            var estimatedPc = mParams[0]
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

                    val paramsValue = mParams[j + 1]
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
     * 数据标准化，均值归一化
     */
    private fun featureNormalize(dataList: List<MutableMap<Int, Any>>?) {
        dataList ?: return
        val xAxis = dataList.size
        if (xAxis < 2) return

        val yAxis = dataList[1].size
        if (yAxis <= 0) return

        val means = mMu
        val sigmas = mSigma

        for (j in 0 until yAxis) {
            // 最后的功耗数据不需要标准化
            if (j == yAxis - 1 && hasRealPc) {
                break
            }

            // 标准化数据
            for (i in 0 until xAxis) {
                val yVal = dataList[i][j] as? String ?: return
                dataList[i][j] = ((yVal.toFloat() - means[j]) / sigmas[j]).toString()
            }
        }
    }

    /**
     * 数据标准化，线型归一化
     */
    private fun featureNormalize2(dataList: List<MutableMap<Int, Any>>?) {
        dataList ?: return
        val xAxis = dataList.size
        if (xAxis < 2) return

        val yAxis = dataList[1].size
        if (yAxis <= 0) return

        val min = mMu
        val max = mSigma

        for (j in 0 until yAxis) {
            // 最后的功耗数据不需要标准化
            if (j == yAxis - 1 && hasRealPc) {
                break
            }

            // 标准化数据
            for (i in 0 until xAxis) {
                val yVal = dataList[i][j] as? String ?: return
                dataList[i][j] = ((yVal.toFloat() - min[j]) / (max[j] - min[j])).toString()
            }
        }
    }
}