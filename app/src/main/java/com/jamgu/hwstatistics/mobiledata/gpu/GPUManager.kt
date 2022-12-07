package com.jamgu.hwstatistics.mobiledata.gpu

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
    private const val GPU_CUR_FREQ = "/sys/class/kgsl/kgsl-3d0/gpuclk"
    private const val UTIL_FILE_PATH2 = "/sys/devices/soc/5000000.qcom,kgsl-3d0/kgsl/kgsl-3d0/gpu_busy_percentage"

    fun getGpuUtilization(): Float {
//        val text = readFile(UTIL_FILE_PATH)
//        val statFile = RandomAccessFile(UTIL_FILE_PATH2, "r")
//        statFile.seek(0)
//        val text = statFile.readLine()
//        Log.d(TAG, text)

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

    // todo 明天试试游戏环境下，拿gpu利用率数据会不会准确些，如果误差难以估算，是不是可以这样：先算出io消耗的平均功率，最后再减掉这部分
    fun getGpuCurFreq2(): Float {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            os.use {
//                os.write("tail -n 1 $UTIL_FILE_PATH".toByteArray())
                os.write("cat $UTIL_FILE_PATH".toByteArray())
                os.flush()
            }
            process.waitFor()
            InputStreamReader(process.inputStream).use {
                BufferedReader(it).use { br ->
                    val readLine = br.readLine()
                    Log.d(TAG, readLine)
                }
            }
            process.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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