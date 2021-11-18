package com.jamgu.hwstatistics.util

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by jamgu on 2021/11/18
 */
object RCommand {
    var strReadContent = ""

    @JvmStatic
    fun setEnablePrivilege(file: File?, bEnable: Boolean?) {
        file ?: return
        bEnable ?: return

        try {
            if (!file.exists()) {
                throw FileNotFoundException(file.absolutePath)
            }
            if (bEnable) {
                ShellUtils.execCommand("chmod 777 " + file.absolutePath, true)
            } else {
                ShellUtils.execCommand("chmod 444 " + file.absolutePath, true)
                ShellUtils.execCommand("chown system " + file.absolutePath, true)
            }
        } catch (ep: Exception) {
            ep.printStackTrace()
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun readFileContent(file: File?): String {
        file ?: return ""

        if (!file.exists()) {
            throw FileNotFoundException(file.absolutePath)
        }
        strReadContent = if (file.canRead()) {
            FileUtils.readFileToString(file)
        } else {
            ShellUtils.execCommand("cat " + file.absolutePath, true)?.successMsg ?: ""
        }
        return strReadContent
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readFileContentAsLineArray(path: File): Array<String?>? {
        return readFileContent(path).trim { it <= ' ' }.split("\n").toTypedArray()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readFileContentAsList(file: File?): List<*>? {
        file ?: return null
        val elements = readFileContentAsLineArray(file)
        return if (elements != null) listOf(*elements) else null
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeFileContent(file: File?, data: String?) {
        file ?: return
        data ?: return

        if (!file.exists()) {
            throw FileNotFoundException(file.absolutePath)
        }
        if (file.canWrite()) {
            FileUtils.writeStringToFile(file, data)
        } else {
            val cmdResult = ShellUtils.execCommand("echo " + data + " > " + file.absolutePath, true)
            strReadContent = cmdResult?.successMsg ?: ""
        }
    }
}