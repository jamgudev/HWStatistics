package com.jamgu.hwstatistics.net.upload

import android.content.Context
import com.jamgu.common.Common
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.net.Network
import com.jamgu.hwstatistics.net.RspModel
import com.jamgu.hwstatistics.power.mobiledata.network.NetWorkManager
import com.jamgu.hwstatistics.util.TimeExtensions.ONE_DAY
import com.jamgu.hwstatistics.util.getCurrentDateString
import com.jamgu.hwstatistics.util.timeMillsBetween
import io.reactivex.Observer
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
    const val USER_NAME = "user_name"
    const val UPLOADED_SUFFIX = "@"

    private fun upload(context: Context, file: File) {
        JLog.d("upload", "upload file = ${file.path}")
        val isScreenOff = PreferenceUtil.getCachePreference(context, 0).getBoolean((DataSaver.TAG_SCREEN_OFF), false)
        if (!isScreenOff) {
            JLog.d(TAG, "uploading when screen off, file = ${file.path}")
            DataSaver.addDebugTracker(context, "uploading when screen off, file = ${file.absolutePath}")
            return
        }
        try {
            val filePath = file.path
            val suffixPath = filePath.substring(filePath.indexOf(DataSaver.CACHE_ROOT_DIR) - 1)
            val user = PreferenceUtil.getCachePreference(context, 0).getString(USER_NAME, "jamgu") ?: "jamgu"
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
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(object: Observer<RspModel> {
                    override fun onNext(rspModel: RspModel) {
                        try {
                            if (rspModel.getCode() == 0) {
                                // 给已经上传的文件改名
                                val xlsxSuffix = filePath.indexOf(DataSaver.EXCEL_SUFFIX)
                                if (xlsxSuffix <= 0) {
                                    DataSaver.addDebugTracker(context, "file's xlsx suffix index <= 0, file = $filePath")
                                    return
                                }
                                val tempFilePath = filePath.substring(0, xlsxSuffix)
                                val uploadedFile = File("${tempFilePath}$UPLOADED_SUFFIX${DataSaver.EXCEL_SUFFIX}")
                                val renameResult = file.renameTo(uploadedFile)
                                DataSaver.addDebugTracker(context, "upload file success, renameResult = $renameResult, " +
                                        "filepath = $tempFilePath")
                            } else {
                                DataSaver.addInfoTracker(context, "upload file failed, code = ${rspModel.getCode()}, " +
                                        "msg = ${rspModel.getMsg()}, filepath = $filePath")
                            }
                            JLog.i(TAG, " code = ${rspModel.getCode()}, msg = ${rspModel.getMsg()}, filepath = $filePath\"")
                        } catch (e: Exception) {
                            JLog.e(TAG, "onNext: filepath = ${file.absolutePath}, error happened, e = ${e.stackTraceToString()}")
                            DataSaver.addInfoTracker(context, "onNext: filepath = ${file.absolutePath}, " +
                                    "error happened, e = ${e.stackTraceToString()}")
                        }
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onError(e: Throwable) {
                        JLog.e(TAG, "filepath = ${file.absolutePath}, error when uploading, e = ${e.stackTraceToString()}")
                        DataSaver.addInfoTracker(context, "filepath = ${file.absolutePath}, " +
                                "error when uploading, e = ${e.stackTraceToString()}")
                    }

                    override fun onComplete() {
                    }
                })
        } catch (e: Exception) {
            JLog.e(TAG, "filepath = ${file.absolutePath}, error happened when uploading, e = ${e.stackTraceToString()}")
            DataSaver.addInfoTracker(context, "filepath = ${file.absolutePath}, " +
                    "error happened when uploading, e = ${e.stackTraceToString()}")
        }
    }

    /**
     * @return 判断该文件是否满足删去的规则，是的话返回该文件是否成功删除
     */
    private fun checkIfNeedDelete(file: File): Boolean {
        var delete = false
        if (file.absolutePath.contains(DataSaver.CACHE_ROOT_DIR)) {
            if (file.exists() && file.isDirectory) {
                // session 数据文件，本地保留一周，一周后清除
                if (isSessionTempDir(file)) {
                    val fileName = file.name
                    val split = fileName.split("#")
                    // 清除1天前的temp文件
                    try {
                        if (split.size == 2 && getCurrentDateString().timeMillsBetween(split[0]) >= ONE_DAY) {
                            file.deleteRecursively()
                            delete = true
                        }
                        DataSaver.addDebugTracker(Common.getInstance().getApplicationContext(),
                            "checkIfNeedDelete, delete = $delete, file = ${file.path}")
                    } catch (e: Exception) {
                        JLog.d(TAG, "checkIfNeedDelete, e = ${e.stackTraceToString()}")
                    }
                }
            }
        }

        return delete
    }

    private fun isSessionTempDir(file: File): Boolean {
        return file.exists() && file.isDirectory && file.name.contains(DataSaver.SESSION_TEMP_SUFFIX)
    }

    private fun innerRecursivelyUpload(context: Context, file: File, timeStamp: String) {
        if (!file.exists()) return

        val childFiles = file.listFiles()
//        JLog.d(TAG, "file ${file.path}, listFiles${childFiles == null}' size = ${childFiles?.size ?: 0}")
        childFiles?.forEach { child ->
            val directory = child.isDirectory
//            JLog.d(TAG, "recursivelyUpload, directory path = ${child.path}, $timeStamp")
            if (directory) {
//                JLog.d(TAG, "recursivelyUpload, directory path = ${child.path}, $timeStamp")
                if (child.list()?.isEmpty() == true) {
                    child.delete()
                    DataSaver.addInfoTracker(context, "filepath = ${child.absolutePath}, error happened when uploading")
                } else {
                    val sessionEndTime = getSessionDirEndTime(child)
                    JLog.d(TAG, "innerRecursivelyUpload, filepath = ${child.path}, sessionEndTime = $sessionEndTime")
                    val canUpload = if (sessionEndTime.isNotEmpty()) {
                        // session文件目录是否完整，完整才能上传
                        sessionEndTime < timeStamp
                    } else if (!checkIfNeedDelete(child)){
                        true
                    } else !isSessionTempDir(child)
                    if (canUpload) {
                        innerRecursivelyUpload(context, child, timeStamp)
                    }
                }
            } else {
//                JLog.d(TAG, "recursivelyUpload, child ----- path = ${child.path}, $timeStamp")
                // 检查该文件是否已经上传
                if (child.name.contains(UPLOADED_SUFFIX)) {
                    return@forEach
                }
                upload(context, child)
            }
        }
    }

    private fun isSessionDirectory(file: File): Boolean {
        val filePath = file.path ?: return false
        return filePath.contains(DataSaver.SESSION_DIR_INFIX) && file.isDirectory
    }

    /**
     * @return 返回session文件的截止时间，如果该文件不是session文件，返回 ""
     */
    private fun getSessionDirEndTime(file: File): String {
        val filePath = file.path ?: return ""
        return if (isSessionDirectory(file)) {
            val childPathSplits = filePath.split(DataSaver.SESSION_DIR_INFIX)
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