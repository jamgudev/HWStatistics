package com.jamgu.hwstatistics.cpu

import android.util.Log
import com.jamgu.hwstatistics.cpu.model.CpuData
import com.jamgu.hwstatistics.util.readFile
import java.io.File
import java.io.FileFilter

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

    private var cpuUtilReader: CpuUtilisationReader = CpuUtilisationReader()

    init {
    }

    /**
     * 获取cpu核数
     * does not need root.
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

    /**
     * 获取cpu当前运行频率
     * does not need root.
     */
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

    /**
     * 获取cpu最大频率
     * does not need root.
     */
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

    /**
     * 获取cpu最小频率
     * does not need root
     */
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

    /**
     * 获取cpu温度
     * does not need root
     */
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
     * api level < #android.os.Build.VERSION_CODES.O, just read /proc/stat directly
     *
     * after O, root required, otherwise return 0.
     */
    fun getCpuUtilization(): CpuData? {
        return cpuUtilReader.let {
            it.update()
            it.cpuInfo
        }
    }


    private val CPU_FILTER = FileFilter {
        val name = it.name
        val regex = """cpu[0-9]""".toRegex()
        regex.containsMatchIn(name)
    }

}