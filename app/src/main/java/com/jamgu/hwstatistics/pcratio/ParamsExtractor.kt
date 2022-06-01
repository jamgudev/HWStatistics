package com.jamgu.hwstatistics.pcratio

import android.content.Context
import android.net.Uri
import com.jamgu.hwstatistics.util.ExcelUtil

/**
 * Created by jamgu on 2022/05/28
 */

private const val TAG = "DataExtractor"

object DataExtractor {
    /**
     * 获取模型参数，sigma，mu 数组
     * 耗时方法，需放到子线程中
     */
    fun getParamData(context: Context?, uri: Uri?): ArrayList<MutableList<Float>>? {
        if (context == null || uri == null) return null

        val path = uri.path
        if (path.isNullOrEmpty()) {
            return null
        }

        val fileName = path.subSequence(
            path.lastIndexOf("/") + 1,
            path.lastIndexOf(".")
        )

        val rawData = ExcelUtil.readExcelNewLineByLine(context, uri,
            "$fileName.xlsx") ?: return null

        val data = ArrayList<MutableList<Float>>()
        rawData.forEach { raw ->
            data.add(raw.values.map { it.toString().toFloat() }.toMutableList())
        }

        return data
    }
}