package com.jamgu.hwstatistics.upload

import android.content.Context
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.yutils.http.YHttp
import com.yutils.http.contract.YHttpListener
import java.io.File

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2023/1/3 11:05 上午
 *
 * @description 上传文件到服务器
 */
object DataUploader {

    private const val TAG = "DataUploader"
    const val BASE_URL = ""
    const val USER_PREFIX = ""

    private fun upload(context: Context, file: File) {
        val user = PreferenceUtil.getCachePreference(context, 0).getString(USER_PREFIX, "") ?: ""
        val params = HashMap<String, String>().apply {
            put("", user)
        }
        YHttp.create().upload(BASE_URL, params, listOf(file), object : YHttpListener {
            override fun success(bytes: ByteArray?, value: String?) {
                JLog.d(TAG, "success = url = $value")
            }

            override fun fail(value: String?) {
            }
        })
    }

    fun recursiveUpload(context: Context, file: File) {
        if (!file.exists()) return

        val childFiles = file.listFiles()
        childFiles?.forEach { child ->
            if (child.isDirectory) {
                recursiveUpload(context, child)
            } else {
                upload(context, child)
            }
        }
    }

}