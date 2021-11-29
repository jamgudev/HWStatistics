package com.jamgu.hwstatistics.memory

import android.app.ActivityManager
import android.content.Context
import com.jamgu.hwstatistics.util.roundToDecimals
import java.io.BufferedReader
import java.io.FileReader

/**
 * Created by jamgu on 2021/11/29
 */
object MemInfoManager {

    private const val TAG = "MemInfoManager"
    private const val MEM_INFO_PATH = "/proc/meminfo"


    fun getMemInfoFromFile(): Array<Float>? {
        return getMemInfoFromFileByKey(MEM_INFO_PATH, arrayOf("MemFree", "Cached"))?.map {
            (it.substring(it.indexOf(':') + 1, it.indexOf('k')).trim().toLong() * 1f / 1024).roundToDecimals(2)
        }?.toTypedArray()
    }

    fun getCurrentFreeMemory(context: Context?): Float {
        if (context == null) return 0f

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0f

        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)

        return (memoryInfo.availMem * 1f / 1024 / 1024).roundToDecimals(2)
    }

    private fun getMemInfoFromFileByKey(path: String, memInfoNeeded: Array<String>): ArrayList<String>? {
        if (path.trim().isEmpty() || memInfoNeeded.isEmpty()) return null
        val memData = ArrayList<String>(memInfoNeeded.size)

        FileReader(path).use { fr ->
            BufferedReader(fr, 8192).use {
                val lineTemp = it.readLine()
                while (!lineTemp.isNullOrBlank()) {
                    memInfoNeeded.forEachIndexed { _, infoStr ->
                        if (lineTemp.contains(infoStr)) {
                            memData.add(lineTemp)
                        }
                    }

                    if (memData.size == memInfoNeeded.size) break
                }
            }
        }

        return memData
    }

}