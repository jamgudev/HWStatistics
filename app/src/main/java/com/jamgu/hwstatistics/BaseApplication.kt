package com.jamgu.hwstatistics

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.jamgu.common.Common
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.hwstatistics.net.upload.DataSaver
import kotlin.system.exitProcess


/**
 * @author jamgudev
 * @date 2022/12/1 9:45 下午
 *
 * @description
 */
class BaseApplication: Application(), Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "BaseApplication"
    }

    private var mKeepLiveServiceOpen: Boolean = false
    private val mRunningActivities = ArrayList<Class<out Activity>>()

    override fun onCreate() {
        super.onCreate()

        Common.getInstance().init(this)
        Thread.setDefaultUncaughtExceptionHandler(this)
        DataSaver.addInfoTracker(TAG, "onCreate")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        JLog.d(TAG, "onTrimMemory level = $level")
    }

    fun addThisActivityToRunningActivities(cls: Class<out Activity>) {
        if (!mRunningActivities.contains(cls)) mRunningActivities.add(cls)
    }

    fun removeThisActivityFromRunningActivities(cls: Class<out Activity>) {
        if (mRunningActivities.contains(cls)) mRunningActivities.remove(cls)
    }

    fun isActivityInBackStack(cls: Class<out Activity>): Boolean {
        return mRunningActivities.contains(cls)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        JLog.e(TAG, "uncaughtException happens[${t.name}]: error{${e.printStackTrace()}}")
        DataSaver.addInfoTracker(TAG, "uncaughtException happens in thread[${t.name}]: error{${e.stackTraceToString()}}")
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        // 重启自己
//        applicationContext.startActivity(intent)
        // 杀掉之前的进程
//        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

}