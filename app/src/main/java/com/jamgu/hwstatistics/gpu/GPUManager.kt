package com.jamgu.hwstatistics.gpu

import android.util.Log
import com.jamgu.hwstatistics.util.IOHelper
import com.jamgu.hwstatistics.util.roundToDecimals
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by jamgu on 2021/11/16
 */
object GPUManager {

    private const val TAG = "GPUManager"
    private const val UTIL_FILE_PATH = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"

    fun getGpuUtilization(): Float {
        val gpu3DUtils = IOHelper.getGpu3DUtils()

        val utilSplits = gpu3DUtils.split(" ")

        if (utilSplits.size == 2) {
            return utilSplits[0].toFloat()
        }

        return 0.0f
    }

    fun getGpuCurFreq(): Float {
        val gpu3DFreq = IOHelper.getGpu3DCurFreq()

        if (!gpu3DFreq.isNullOrEmpty()) {
            return (gpu3DFreq.toFloat() / 100000f).roundToDecimals(2)
        }

        return 0.0f
    }

    fun getGpuCurFreq2(): Float {
//        val randomAccessFile = RandomAccessFile(UTIL_FILE_PATH, "r")
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            os.use {
                os.write("cat $UTIL_FILE_PATH".toByteArray())
                os.flush()
            }
            process.waitFor()
            BufferedReader(InputStreamReader(process.inputStream)).use {
                val readLine = it.readLine()
                Log.d(TAG, readLine)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
//        randomAccessFile.seek(0)
//        val readLine = randomAccessFile.readLine()
//        Log.d(TAG, readLine)
        return 0f
    }

    fun getMaxCpuFreq(): String? {
        var result: String? = ""
        val cmd: ProcessBuilder
        try {
            val args = arrayOf(
                "su",
                "cat $UTIL_FILE_PATH"
            )
            cmd = ProcessBuilder(*args)
            val process = cmd.start()
            val io: InputStream = process.inputStream
            BufferedReader(InputStreamReader(io)).use {
                val readLine = it.readLine()
                Log.d(TAG, readLine)
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            result = null
        }
        return result?.trim { it <= ' ' }
    }

}