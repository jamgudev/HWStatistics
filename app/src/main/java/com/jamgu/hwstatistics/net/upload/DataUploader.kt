package com.jamgu.hwstatistics.net.upload

import android.content.Context
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.net.Network
import com.jamgu.hwstatistics.net.RspModel
import com.jamgu.hwstatistics.power.mobiledata.network.NetWorkManager
import com.jamgu.hwstatistics.util.TimeExtensions
import com.jamgu.hwstatistics.util.getCurrentDateString
import com.jamgu.hwstatistics.util.timeMillsBetween
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
        JLog.d("upload", "upload file = ${file.path}")
        val isScreenOff = PreferenceUtil.getCachePreference(context, 0).getBoolean((DataSaver.TAG_SCREEN_OFF), false)
        if (!isScreenOff) {
            JLog.d(TAG, "uploading when screen off, file = ${file.path}")
            DataSaver.addDebugTracker(context, "uploading when screen off, file = ${file.absolutePath}")
            return
        }
        try {
            val filePath = file.absolutePath
            val suffixPath = filePath.substring(filePath.indexOf(DataSaver.CACHE_ROOT_DIR) - 1)
            val user = PreferenceUtil.getCachePreference(context, 0).getString(USER_PREFIX, "jamgu") ?: "jamgu"
            val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .addFormDataPart("username", user)
                .addFormDataPart("path", suffixPath)
                .addFormDataPart("file", "", requestBody)
                .setType(MultipartBody.FORM)
                .build()

            if (!isNetWorkEnable(context)) {
                DataSaver.addInfoTracker(context, "network error, upload failed, file = $filePath")
                return
            }

            Network.remote().upload(multipartBody.parts)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(object: Observer<RspModel> {
                    override fun onNext(rspModel: RspModel) {
                        if (rspModel.getCode() == 0) {
                            if (!checkIfNeedDeleteAfterUpload(file)) {
                                // 文件不需要删去
                                DataSaver.addInfoTracker(context, "does not need to be deleted, filepath = $filePath")
                            } else {
                                // 看看它的父目录还是否有文件，没有的话，删掉父目录
                                val dirFile = file.parentFile ?: return
                                if (dirFile.isDirectory && dirFile.list().isNullOrEmpty()) {
                                    if (!dirFile.delete()) {
                                        DataSaver.addInfoTracker(context, "parent file delete failed, filepath = ${dirFile.absolutePath}")
                                    }
                                }
                            }
                        } else {
                            DataSaver.addInfoTracker(context, "upload file failed, code = ${rspModel.getCode()}, " +
                                    "msg = ${rspModel.getMsg()}, filepath = $filePath")
                            JLog.d(TAG, "upload file failed, code = ${rspModel.getCode()}, msg = ${rspModel.getMsg()}, filepath = $filePath")
                        }
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onError(e: Throwable) {
                        JLog.d(TAG, "filepath = ${file.absolutePath}, error when uploading, e = ${e.stackTrace}")
                        DataSaver.addInfoTracker(context, "filepath = ${file.absolutePath}, error when uploading, e = ${e.stackTrace}")
                    }

                    override fun onComplete() {
                    }
                })
        } catch (e: Exception) {
            DataSaver.addInfoTracker(context, "filepath = ${file.absolutePath}, error happened when uploading")
        }
    }

    /**
     * @return 判断该文件是否满足删去的规则，是的话返回该文件是否成功删除
     */
    private fun checkIfNeedDeleteAfterUpload(file: File): Boolean {
        return if (file.absolutePath.contains(DataSaver.CACHE_ROOT_DIR)) {
            if (file.exists() && !file.isDirectory) {
                // session 数据文件，保留一周，一周后清除
                val sessionFileEndTime = getSessionFileEndTime(file)
                if (sessionFileEndTime.isNotEmpty()) {
                    val duration = getCurrentDateString().timeMillsBetween(sessionFileEndTime)
                    if (duration >= TimeExtensions.ONE_WEEK) {
                        return file.delete()
                    }
                }
                false
            } else false
        } else false
    }

    private fun innerRecursivelyUpload(context: Context, file: File, timeStamp: String) {
        if (!file.exists()) return

        val childFiles = file.listFiles()
//        JLog.d(TAG, "file ${file.path}, listFiles${childFiles == null}' size = ${childFiles?.size ?: 0}")
        childFiles?.forEach { child ->
            val directory = child.isDirectory
            JLog.d(TAG, "recursivelyUpload, directory path = ${child.path}, $timeStamp")
            if (directory) {
//                JLog.d(TAG, "recursivelyUpload, directory path = ${child.path}, $timeStamp")
                if (child.list()?.isEmpty() == true) {
                    child.delete()
                    DataSaver.addInfoTracker(context, "filepath = ${file.absolutePath}, error happened when uploading")
                } else {
                    innerRecursivelyUpload(context, child, timeStamp)
                }
            } else {
//                JLog.d(TAG, "recursivelyUpload, child ----- path = ${child.path}, $timeStamp")
                val sessionEndTime = getSessionFileEndTime(child)
                val canUpload = if (sessionEndTime.isNotEmpty()) {
                    // session文件目录是否完整，完整才能上传
                    sessionEndTime < timeStamp
                } else {
                    true
                }
                if (canUpload) {
                    upload(context, child)
                }
            }
        }
    }

    private fun isSessionFile(file: File): Boolean {
        val filePath = file.path ?: return false
        return filePath.contains(DataSaver.SESSION_FILE_INFIX)
    }

    /**
     * @return 返回session文件的截止时间，如果该文件不是session文件，返回 ""
     */
    private fun getSessionFileEndTime(file: File): String {
        val filePath = file.path ?: return ""
        return if (isSessionFile(file)) {
            val childPathSplits = filePath.split(DataSaver.SESSION_FILE_INFIX)
            if (childPathSplits.size == 2 && childPathSplits[1].isNotEmpty()) {
                childPathSplits[1]
            } else ""
        } else ""
    }

    /**
     * 递归上传[file]目录下所有文件
     * @param timeStamp session文件，只会上传记录时间完成在timeStamp之前的文件
     */
    fun recursivelyUpload(context: Context, file: File, timeStamp: String) {
        ThreadPool.runIOTask { innerRecursivelyUpload(context, file, timeStamp) }
    }

    private fun isNetWorkEnable(context: Context): Boolean {
        val networkType = NetWorkManager.getNetworkType(context)
        return networkType >= 0
    }

}