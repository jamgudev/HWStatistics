package com.jamgu.hwstatistics.power.mobiledata.cpu.model

import com.jamgu.hwstatistics.util.roundToDecimals

/**
 * Created by jamgu on 2021/10/18
 */
class CPU(
    val maxFreq: Float,
    val minFreq: Float,
    val curFreq: Float,
    val temp: Float,
    val utilization: Float,
) {
    override fun toString(): String {
        return "[max: $maxFreq, min: $minFreq, cur: $curFreq, temp: $temp, utils: ${utilization.roundToDecimals(2)}]"
    }
}