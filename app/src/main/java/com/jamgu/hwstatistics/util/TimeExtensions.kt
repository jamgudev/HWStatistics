package com.jamgu.hwstatistics.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author jamgudev
 * @date 2022/12/10 14:48
 *
 * @description
 */
object TimeExtensions{
    // in mills
    const val ONE_DAY = 86400L
    const val THREE_DAYS = 3 * ONE_DAY
    const val ONE_WEEK = 7 * ONE_DAY
    const val HALF_MONTH = 15 * ONE_DAY
}

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

fun getDateOfTodayString(): String {
    return getTodaySdf().format(Date(System.currentTimeMillis()))
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

private fun getTodaySdf() = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
