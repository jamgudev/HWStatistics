package com.jamgu.hwstatistics.net.upload

import android.content.Context
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.net.Network
import com.jamgu.hwstatistics.net.RspModel
import com.jamgu.hwstatistics.power.mobiledata.network.NetWorkManager
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * @author jamgudev
 * @date 2023/1/3 11:05 上午
 *
 * @description 上传文件到服务器
 */
object DataUploader {

    private const val TAG = "DataUploader"
    const val USER_PREFIX = ""

    private fun upload(context: Context, file: File) {
        val isScreenOff = PreferenceUtil.getCachePreference(context, 0).getBoolean((DataSaver.TAG_SCREEN_OFF), false)
        if (!isScreenOff) {
            JLog.d(TAG, "uploading when screen off.")
            DataSaver.addTestTracker(context, "uploading when screen off, file = ${file.absolutePath}")
            return
        }
        try {
            val filePath = file.absolutePath
            val suffixPath = filePath.substring(filePath.indexOf(DataSaver.CACHE_ROOT_DIR) - 1)
            val user = PreferenceUtil.getCachePreference(context, 0).getString(USER_PREFIX, "ott") ?: "ott"
            val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .addFormDataPart("username", user)
                .addFormDataPart("path", suffixPath)
                .addFormDataPart("file", "", requestBody)
                .setType(MultipartBody.FORM)
                .build()

            if (!isNetWorkEnable(context)) {
                DataSaver.addTestTracker(context, "network error, upload failed, file = $filePath")
                return
            }

            Network.remote().upload(multipartBody.parts)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(object: Observer<RspModel> {
                    override fun onNext(rspModel: RspModel) {
                        if (rspModel.getCode() == 0) {
                            if (!checkIfNeedDelete(file)) {
                                // 文件未成功删去
                                DataSaver.addTestTracker(context, "delete file failed, filepath = $filePath")
                            } else {
                                // 看看它的父目录还是否有文件，没有的话，删掉父目录
                                val dirFile = file.parentFile ?: return
                                if (dirFile.isDirectory && dirFile.listFiles().isNullOrEmpty()) {
                                    if (!dirFile.delete()) {
                                        DataSaver.addTestTracker(context, "parent file delete failed, filepath = ${dirFile.absolutePath}")
                                    }
                                }
                            }
                        } else {
                            DataSaver.addTestTracker(context, "upload file failed, code = ${rspModel.getCode()}, " +
                                    "msg = ${rspModel.getMsg()}, filepath = $filePath")
                            JLog.d(TAG, "upload file failed, code = ${rspModel.getCode()}, msg = ${rspModel.getMsg()}, filepath = $filePath")
                        }
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onError(e: Throwable) {
                        JLog.d(TAG, "onError")
                        DataSaver.addTestTracker(context, "filepath = ${file.absolutePath}, error when uploading, e = ${e.stackTrace}")
                    }

                    override fun onComplete() {
                    }
                })
        } catch (e: Exception) {
            DataSaver.addTestTracker(context, "filepath = ${file.absolutePath}, error happened when uploading")
        }
    }

    /**
     * @return 判断该文件是否满足删去的规则，是的话返回该文件是否成功删除
     */
    private fun checkIfNeedDelete(file: File): Boolean {
        return if (file.absolutePath.contains(DataSaver.CACHE_ROOT_DIR)) {
            if (file.exists() && !file.isDirectory) {
                file.delete()
            } else false
        } else false
    }

    /**
     * 递归上传[file]目录下所有文件
     * @param timeStamp session文件，只会上传记录时间完成在timeStamp之前的文件
     */
    fun recursivelyUpload(context: Context, file: File, timeStamp: String) {
        if (!file.exists()) return

        val childFiles = file.listFiles()
        childFiles?.forEach { child ->
            if (child.isDirectory) {
                if (child.listFiles().isNullOrEmpty()) {
                    child.delete()
                } else {
                    val childPath = child.absolutePath ?: return
                    // session文件目录是否完整，完整才能上传
                    val canUpload = if (childPath.contains(DataSaver.FILE_INFIX)) {
                        val childPathSplits = childPath.split(DataSaver.FILE_INFIX)
                        // session 文件必须完整才能上传
                        JLog.d(TAG, "path = $childPath, ${childPathSplits[1]}, $timeStamp")
                        childPathSplits.size == 2 && childPathSplits[1].isNotEmpty() && childPathSplits[1] < timeStamp
                    } else {
                        true
                    }
                    if (canUpload) {
                        recursivelyUpload(context, child, timeStamp)
                    }
                }
            } else {
                upload(context, child)
            }
        }
    }

    private fun isNetWorkEnable(context: Context): Boolean {
        return NetWorkManager.getNetworkType(context) >= 0
    }

}