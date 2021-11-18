package com.jamgu.hwstatistics.cpu

import android.content.Context
import android.content.Context.HARDWARE_PROPERTIES_SERVICE
import android.os.HardwarePropertiesManager
import android.util.Log
import com.jamgu.hwstatistics.util.cpu.CpuData
import com.jamgu.hwstatistics.util.cpu.CpuUtilisationReader
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileReader

/**
 * Created by jamgu on 2021/10/18
 */
object CPUInfoManager {

    private const val TAG = "CPUInfoManager"

    private const val FILE_NOT_FOUND = -1

    private const val CPU_PATH_DIR = "/sys/devices/system/cpu"

    /**
     * cpu#idx 下对应的文件
     */
    private const val CPU_RUNNING_FREQ = "/cpufreq/scaling_cur_freq"

    private const val CPU_MAX_FREQ = "/cpufreq/cpuinfo_max_freq"

    private const val CPU_MIN_FREQ = "/cpufreq/cpuinfo_min_freq"

//    private const val CPU_TEMP = "/cpufreq/cpu_temp"
    private const val CPU_TEMP = "/sys/class/thermal/thermal_zone#/temp"

    private const val CPU_UTILIZATION_PATH = "/proc/stat"

    private var cpuUtilReader: CpuUtilisationReader? = null

    init {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            cpuUtilReader = CpuUtilisationReader()
        }
    }

    /**
     * required running in sub thread.
     */
    fun getCpuCoresNumb(): Int {
        return try {
            val file = File(CPU_PATH_DIR)
            if (file.exists()) {
                file.listFiles(CPU_FILTER)?.size ?: FILE_NOT_FOUND
            } else FILE_NOT_FOUND
        } catch (e: SecurityException) {
            FILE_NOT_FOUND
        } catch (e: NullPointerException) {
            FILE_NOT_FOUND
        }
    }

    fun getCpuRunningFreq(cpuIdx: Int): Float {
        var freq = -1f
        try {
            val freqStr = readFile("$CPU_PATH_DIR/cpu$cpuIdx$CPU_RUNNING_FREQ")
            freq = String.format("%.1f", freqStr.toInt() / 1000.0).toFloat()
        } catch (e: NumberFormatException) {
            Log.d(TAG, e.message.toString())
        }
        return freq
    }

    fun getCpuMaxFreq(cpuIdx: Int): Float {
        var freq = -1f
        try {
            val freqStr = readFile("$CPU_PATH_DIR/cpu$cpuIdx$CPU_MAX_FREQ")
            freq = String.format("%.1f", freqStr.toInt() / 1000.0).toFloat()
        } catch (e: NumberFormatException) {
            Log.d(TAG, e.message.toString())
        }
        return freq
    }

    fun getCpuMinFreq(cpuIdx: Int): Float {
        var freq = -1f
        try {
            val freqStr = readFile("$CPU_PATH_DIR/cpu$cpuIdx$CPU_MIN_FREQ")
            freq = String.format("%.1f", freqStr.toInt() / 1000.0).toFloat()
        } catch (e: NumberFormatException) {
            Log.d(TAG, e.message.toString())
        }
        return freq
    }

    fun getCpuTemp(cpuIdx: Int): Float {
        var temp = -1f
        try {
            val freqStr = readFile(CPU_TEMP.replace("#", cpuIdx.toString(), true))
            temp = String.format("%.1f", freqStr.toInt() / 1000.0).toFloat()
            Log.d(TAG, "cpu#$cpuIdx's temp --->>> $temp")
        } catch (e: NumberFormatException) {
            Log.d(TAG, e.message.toString())
        }
        return temp
    }


    /**
     * api level <= #android.os.Build.VERSION_CODES.N
     */
    fun getCpuUtilizationFromFile(): CpuData? {
        return cpuUtilReader?.let {
            it.update()
            it.cpuInfo
        }
    }

    /**
     * required android 8.0, need system-wise permissions.
     */
    fun getCpuUtilization(context: Context?, cpuCount: Int): FloatArray {
        val cpuUsages = ArrayList<Float>(cpuCount)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val hwPropertiesManager =
                context?.getSystemService(HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
            val cpuUsagesInfo = hwPropertiesManager?.cpuUsages
            cpuUsagesInfo?.forEach {
                cpuUsages.add(it.active * 1.0f / it.total)
            }
        }
        return cpuUsages.toFloatArray()
    }

    private fun readFile(path: String): String {
        var fileString = ""
        try {
            FileReader(path).use { fr ->
                BufferedReader(fr).use { br ->
                    fileString = br.readLine()
                }
            }
        } catch (e: FileNotFoundException) {
            Log.d(TAG, e.message.toString())
        }
        return fileString
    }

    private val CPU_FILTER = FileFilter {
        val name = it.name
        val regex = """cpu[0-9]""".toRegex()
        regex.containsMatchIn(name)
    }

}