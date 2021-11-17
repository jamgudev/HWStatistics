package com.jamgu.hwstatistics.gpu

import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by jamgu on 2021/11/16
 */
object GPUManager {

    private const val TAG = "GPUManager"
    private const val UTIL_FILE_PATH = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
    private const val GPU_CURRENT_FREQ = "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq"

    private lateinit var mRandomAccessFile: RandomAccessFile
    private val mFileOpenedOk: AtomicBoolean = AtomicBoolean()


    fun getGpuUtilization(): Int {
        openFile()

        if (mFileOpenedOk.get()) {
            try {
                mRandomAccessFile.seek(0)
                var cpuLine: String
                var cpuId = -1
                cpuLine = mRandomAccessFile.readLine()
                Log.d(TAG, cpuLine)
            } catch (e: IOException) {
                Log.e(TAG, "Error parsing file: $e")
            }
        }

        return 0

    }


    private fun openFile() {
        try {
            mRandomAccessFile = RandomAccessFile(UTIL_FILE_PATH, "r")
            mFileOpenedOk.set(true)
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "$UTIL_FILE_PATH not found, ${e.message.toString()}")
        }
    }
}