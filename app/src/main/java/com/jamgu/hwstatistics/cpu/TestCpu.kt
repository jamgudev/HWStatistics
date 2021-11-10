package com.jamgu.hwstatistics.cpu

import android.text.TextUtils
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.RandomAccessFile

/**
 * Created by jamgu on 2021/10/21
 */

class TempCpu {
    companion object {
        private const val TAG = "TestCpu"
        const val PROC_STAT_PATH = "/proc/stat"
    }
    var mProcStatFile: RandomAccessFile = RandomAccessFile("/proc/stat", "r")
    //var mAppStatFile: RandomAccessFile = RandomAccessFile("/proc/" + android.os.Process.myPid() + "/stat", "r")
    var mLastCpuTime: Long? = null
    //var mLastAppCpuTime: Long? = null

    /**
     * 8.0以下获取cpu的方式
     *
     * @return
     */
    val cpuData: Float
        get() {
            val cpuTime: Long
            val appTime: Long
            var value = 0.0f
            try {
                mProcStatFile.seek(0L)
                val procStatString = mProcStatFile.readLine()
                val procStats = procStatString.split(" ".toRegex()).toTypedArray()

                cpuTime = procStats[2].toLong() + procStats[3].toLong() + procStats[4].toLong() + procStats[5].toLong() + procStats[6].toLong() + procStats[7].toLong() + procStats[8].toLong()
                if (mLastCpuTime == null) {
                    mLastCpuTime = cpuTime
                    return value
                }
//            value = (appTime - mLastAppCpuTime!!).toFloat() / (cpuTime - mLastCpuTime!!).toFloat() * 100f
                mLastCpuTime = cpuTime
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return value
        }

    fun getCpuUsageBeforeO(cpuCount: Int): FloatArray {
        RandomAccessFile("PROC_STAT_PATH", "r").use {
            val readLine = it.readLine()
            val anotherLine = it.readLine()
            Log.d(TAG, readLine)
//            for (i in 0 until cpuCount)
        }

        return FloatArray(cpuCount)
    }

    /**
     * 8.0以上获取cpu的方式
     *
     * @return
     */
    val cpuDataForO: Float
        get() {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec("top -n 1")
                InputStreamReader(process.inputStream).use { ir ->
                    val reader = BufferedReader(ir)
                    var line: String
                    var cpuIndex = -1
                    while (reader.readLine().also { line = it } != null) {
                        line = line.trim { it <= ' ' }
                        if (TextUtils.isEmpty(line)) {
                            continue
                        }
                        val tempIndex = getCPUIndex(line)
                        if (tempIndex != -1) {
                            cpuIndex = tempIndex
                            continue
                        }
                        if (line.startsWith(android.os.Process.myPid().toString())) {

                            if (cpuIndex == -1) {
                                continue
                            }
                            val param = line.split("\\s+".toRegex()).toTypedArray()

                            for ((index,item) in param.withIndex()){
                                println("index $index : $item")
                            }
                            if (param.size <= cpuIndex) {
                                continue
                            }
                            var cpu = param[cpuIndex]
                            if (cpu.endsWith("%")) {
                                cpu = cpu.substring(0, cpu.lastIndexOf("%"))
                            }
                            return cpu.toFloat() / Runtime.getRuntime().availableProcessors()
                        }
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                process?.destroy()
            }
            return 0F
        }

    fun getCPUIndex(line: String): Int {
        if (line.contains("CPU")) {
            val titles = line.split("\\s+".toRegex()).toTypedArray()
            for (i in titles.indices) {
                if (titles[i].contains("CPU")) {
                    return i
                }
            }
        }
        return -1
    }

}



///**
// * 8.0以下获取cpu的方式
// *
// * @return
// */
//val cpuData: Float
//    get() {
//        val cpuTime: Long
//        val appTime: Long
//        var value = 0.0f
//        try {
//            mProcStatFile.seek(0L)
//            mAppStatFile.seek(0L)
//            val procStatString = mProcStatFile.readLine()
//            val appStatString = mAppStatFile.readLine()
//            val procStats = procStatString.split(" ".toRegex()).toTypedArray()
//            val appStats = appStatString.split(" ".toRegex()).toTypedArray()
//
//            cpuTime = procStats[2].toLong() + procStats[3].toLong() + procStats[4].toLong() + procStats[5].toLong() + procStats[6].toLong() + procStats[7].toLong() + procStats[8].toLong()
//            appTime = appStats[13].toLong() + appStats[14].toLong()
//            if (mLastCpuTime == null && mLastAppCpuTime == null) {
//                mLastCpuTime = cpuTime
//                mLastAppCpuTime = appTime
//                return value
//            }
//            value = (appTime - mLastAppCpuTime!!).toFloat() / (cpuTime - mLastCpuTime!!).toFloat() * 100f
//            mLastCpuTime = cpuTime
//            mLastAppCpuTime = appTime
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return value
//    }
