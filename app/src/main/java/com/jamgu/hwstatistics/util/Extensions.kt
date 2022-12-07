package com.jamgu.hwstatistics.util

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
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

fun readFile(path: String, tag: String = "Constants"): String {
    val file = File(path)
    var fileString = ""
    if (!file.exists()) {
        return fileString
    }

    if (!file.canRead()) {
        val readable = file.setReadable(true)
        Log.d(tag, "File[path:$path] is not readable!, readable = $readable")
        return ""
    }


    try {
        FileReader(path).use { fr ->
            BufferedReader(fr).use { br ->
                fileString = br.readLine()
            }
        }
    } catch (e: FileNotFoundException) {
        Log.d(tag, e.message.toString())
    }
    return fileString
}