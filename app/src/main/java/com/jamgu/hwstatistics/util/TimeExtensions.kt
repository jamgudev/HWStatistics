package com.jamgu.hwstatistics.appusage

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/12/10 14:48
 *
 * @description
 */
fun Long.timeStamp2DateStringWithMills(): String {
    return getSdfWithMills().format(Date(this))
}

fun Long.timeStamp2DateString(): String {
    return getSdf().format(Date(this))
}

@SuppressLint("SimpleDateFormat")
fun Long.timeStamp2SimpleDateString(): String {
    return getSimpleSdf().format(Date(this))
}

fun String.timeMillsBetween(fromTimeStr: String): Long {
    if (fromTimeStr.isEmpty()) return 0L

    val fromDate = getSdfWithMills().parse(fromTimeStr) ?: return 0L
    val toDate = getSdfWithMills().parse(this) ?: return 0L

    return toDate.time - fromDate.time
}

fun getCurrentDateString(): String {
    return System.currentTimeMillis().timeStamp2DateStringWithMills()
}

private fun getSdfWithMills() = SimpleDateFormat("yyyyMMdd(HH_mm_ss_SSS)", Locale.CHINA).apply {
//    timeZone = TimeZone.getTimeZone("GMT+00:00")
}

private fun getSdf() = SimpleDateFormat("yyyyMMdd(HH_mm_ss)", Locale.CHINA).apply {
//    timeZone = TimeZone.getTimeZone("GMT+00:00")
}

private fun getSimpleSdf() = SimpleDateFormat("HH_mm_ss_SSS", Locale.CHINA).apply {
    timeZone = TimeZone.getTimeZone("GMT+00:00")
}
