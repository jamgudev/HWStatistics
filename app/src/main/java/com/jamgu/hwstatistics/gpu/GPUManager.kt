package com.jamgu.hwstatistics.gpu

import com.jamgu.hwstatistics.util.IOHelper
import com.jamgu.hwstatistics.util.roundToDecimals

/**
 * Created by jamgu on 2021/11/16
 */
object GPUManager {

    private const val TAG = "GPUManager"

    fun getGpuUtilization(): Float {
        val gpu3DUtils = IOHelper.getGpu3DUtils()

        val utilSplits = gpu3DUtils.split(" ")

        if (utilSplits.size == 2) {
            return utilSplits[0].toFloat()
        }

        return 0.0f
    }

    fun getGpuCurFreq(): Float {
        val gpu3DFreq = IOHelper.getGpu3DCurFreq()

        if (!gpu3DFreq.isNullOrEmpty()) {
            return (gpu3DFreq.toFloat() / 100000f).roundToDecimals(2)
        }

        return 0.0f
    }



}