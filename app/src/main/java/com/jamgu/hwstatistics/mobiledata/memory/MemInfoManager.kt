package com.jamgu.hwstatistics.mobiledata.memory

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

    /**
     *
    （1）MemTotal: 所有可用RAM大小。（即物理内存减去一些预留位和内核的二进制代码大小）
    （2）MemFree: LowFree与HighFree的总和，被系统留着未使用的内存。
    （3）Buffers: 用来给文件做缓冲大小。
    （4）Cached: 被高速缓冲存储器（cache memory）用的内存的大小（等于diskcache minus SwapCache）。
    （5）SwapCached:被高速缓冲存储器（cache memory）用的交换空间的大小。已经被交换出来的内存，
         仍然被存放在swapfile中，用来在需要的时候很快的被替换而不需要再次打开I/O端口。
    （6）Active: 在活跃使用中的缓冲或高速缓冲存储器页面文件的大小，除非非常必要，否则不会被移作他用。
    （7）Inactive: 在不经常使用中的缓冲或高速缓冲存储器页面文件的大小，可能被用于其他途径。
    （8）SwapTotal: 交换空间的总大小。
    （9）SwapFree: 未被使用交换空间的大小。
    （10）Dirty: 等待被写回到磁盘的内存大小。
    （11）Writeback: 正在被写回到磁盘的内存大小。
    （12）AnonPages：未映射页的内存大小。
    （13）Mapped: 设备和文件等映射的大小。
    （14）Slab: 内核数据结构缓存的大小，可以减少申请和释放内存带来的消耗。
    （15）SReclaimable:可收回Slab的大小。
    （16）SUnreclaim：不可收回Slab的大小（SUnreclaim+SReclaimable＝Slab）。
    （17）PageTables：管理内存分页页面的索引表的大小。
    （18）NFS_Unstable:不稳定页表的大小。
     */
    private val sMemData = arrayOf(
        "MemFree", "MemAvailable",
//        "Buffers", "Cached",
        "Active", "Inactive",
        "Dirty",
        "AnonPages", "Mapped",
    )


    fun getMemInfoFromFile(): Array<Float>? {
        return getMemInfoFromFileByKey(MEM_INFO_PATH, sMemData)?.map {
            (it.substring(it.indexOf(':') + 1, it.indexOf('k')).trim().toLong() * 1f / 1024).roundToDecimals(2)
        }?.toTypedArray()
    }

    /**
     * 相当于获取文件的MemAvailable信息
     */
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
            BufferedReader(fr, 1024).use {
                var lineTemp = it.readLine()
                while (!lineTemp.isNullOrBlank()) {
                    memInfoNeeded.forEachIndexed { _, infoStr ->
                        if (lineTemp.contains(infoStr)) {
                            memData.add(lineTemp)
                        }
                    }

                    if (memData.size == memInfoNeeded.size) break

                    lineTemp = it.readLine()
                }
            }
        }

        return memData
    }

}