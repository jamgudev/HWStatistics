package com.jamgu.hwstatistics.util

import java.lang.StringBuilder
import java.text.DecimalFormat

/**
 * Created by jamgu on 2021/11/04
 */

fun Float.roundToDecimals(decimalPlaces: Int): Float {
    val decimalSb = StringBuilder("0.")
    for (i in 0 until decimalPlaces) {
        decimalSb.append("#")
    }
    val format = DecimalFormat(decimalSb.toString())
    return format.format(this).toFloat()
}