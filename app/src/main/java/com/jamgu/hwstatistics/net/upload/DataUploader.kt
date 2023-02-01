package com.jamgu.hwstatistics.net.upload

import android.content.Context
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.net.Network
import com.jamgu.hwstatistics.net.RspModel
import com.jamgu.hwstatistics.util.timeStamp2DateStringWithMills
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2023/1/3 11:05 上午
 *
 * @description 上传文件到服务器
 */
object DataUploader {

    private const val TAG = "DataUploader"
    const val BASE_URL = "http://1.12.242.152:9001"
    const val USER_PREFIX = ""

    private fun upload(context: Context, file: File) {
        val parentPath = file.parentFile?.absolutePath ?: return
        try {
            val canUpload = if (parentPath.contains(DataSaver.ACTIVE_DIR)) {
                val parentDirName = parentPath.split("/").last()
                val sessionDates = parentDirName.split("_")
                val nowDate = System.currentTimeMillis().timeStamp2DateStringWithMills()
                // session 文件必须完整才能上传
                sessionDates.size == 2 && sessionDates[1] < nowDate
            } else {
                true
            }

            if (canUpload) {
                val user = PreferenceUtil.getCachePreference(context, 0).getString(USER_PREFIX, "") ?: ""
                val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Builder()
                    .addFormDataPart("username", user)
                    .addFormDataPart("path", file.absolutePath)
                    .addFormDataPart("file", "", requestBody)
                    .setType(MultipartBody.FORM)
                    .build()
                Network.remote().upload(multipartBody.parts)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(object: Observer<RspModel> {
                        override fun onNext(rspModel: RspModel) {
                            JLog.d(TAG, "result = $rspModel, code = ${rspModel.getCode()} msg = ${rspModel.getMsg()}")
                        }

                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onError(e: Throwable) {
                            DataSaver.addTestTracker(context, "filepath = ${file.absolutePath}, error when uploading, e = ${e.stackTrace}")
                        }

                        override fun onComplete() {
                        }
                    })
            } else {
                DataSaver.addTestTracker(context, "filepath = ${file.absolutePath} can not be uploaded.")
            }
        } catch (e: Exception) {
            DataSaver.addTestTracker(context, "filepath = ${file.absolutePath}, error happened when uploading")
        }
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