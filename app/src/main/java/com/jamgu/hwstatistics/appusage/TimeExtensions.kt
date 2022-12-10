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
fun Long.timeStamp2DateString(): String {
    return getSdf().format(Date(this))
}

@SuppressLint("SimpleDateFormat")
fun Long.timeStamp2SimpleDateString(): String {
    return getSimpleSdf().format(Date(this))
}

fun String.timeMillsBetween(fromTimeStr: String): Long {
    val fromDate = getSdf().parse(fromTimeStr) ?: return 0L
    val toDate = getSdf().parse(this) ?: return 0L

    val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("GMT+00:00")
    }
    return toDate.time - fromDate.time
}

fun getCurrentDateString(): String {
    return System.currentTimeMillis().timeStamp2DateString()
}

private fun getSdf() = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA).apply {
    timeZone = TimeZone.getTimeZone("GMT+00:00")
}

private fun getSimpleSdf() = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
    timeZone = TimeZone.getTimeZone("GMT+00:00")
}
