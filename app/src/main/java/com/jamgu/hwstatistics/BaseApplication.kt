package com.jamgu.hwstatistics

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jamgu.common.Common
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.hwstatistics.page.AUTO_MONITOR_START_FROM_BOOT
import com.jamgu.hwstatistics.page.AutoMonitorActivity
import com.jamgu.hwstatistics.page.TransitionActivity
import com.jamgu.hwstatistics.util.getCurrentDateString
import com.jamgu.hwstatistics.util.timeMillsBetween
import com.jamgu.hwstatistics.util.timeStamp2DateStringWithMills
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
        private const val TAG_IS_INIT = "is_init"
    }

    private val mRunningActivities = ArrayList<Class<out Activity>>()

    override fun onCreate() {
        super.onCreate()

        Common.getInstance().init(this)
        Thread.setDefaultUncaughtExceptionHandler(this)
        DataSaver.addInfoTracker(TAG, "onCreate")

        val isInit = PreferenceUtil.getCachePreference(this, 0).getBoolean(TAG_IS_INIT, true)
        val inBackStack = isActivityInBackStack(AutoMonitorActivity::class.java)
        ThreadPool.runUITask({
            if (!isInit && !inBackStack && !KeepAliveService.isStarted()) {
                DataSaver.addDebugTracker(TAG, "自启动拉起通知")
                showCallNotification(this,  R.string.click_to_start, TransitionActivity::class.java)
            }
            if (isInit) {
                PreferenceUtil.getCachePreference(this, 0).edit().putBoolean(TAG_IS_INIT, false).apply()
            }
        }, 4000)
    }

    private fun showCallNotification(context: Context?, contentTextID: Int, pdActivity: Class<out Activity?>?) {
        var contextText = contentTextID
        if (context == null) return
        if (contextText <= 0) {
            contextText = R.string.click_to_start
        }
        val intent = Intent(context, pdActivity)
        intent.putExtra(AUTO_MONITOR_START_FROM_BOOT, true)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        @SuppressLint("UnspecifiedImmutableFlag") val pendingIntent = PendingIntent
            .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val channelID = "app_call_notification"
        val channelName = "日志APP拉起通知"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val level = NotificationManager.IMPORTANCE_HIGH
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelID, channelName, level)
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentText(context.getString(contextText))
            .setContentTitle(context.getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVibrate(longArrayOf(0, 1000L, 1000L, 1000L))
            .setContentIntent(pendingIntent) // 点击时的intent
            .setDeleteIntent(pendingIntent) // 被用户清除时的intent
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(100, builder.build())
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val destroyTime = KeepAliveService.getActivityDestroyTime()
        val millsBetween = getCurrentDateString().timeMillsBetween(destroyTime).timeStamp2DateStringWithMills()
        DataSaver.addDebugTracker(TAG, "onTrimMemory level = $level, activity destroyed time passed:" +
                " $millsBetween, is loader running = ${KeepAliveService.isStarted()}")

        if (KeepAliveService.checkIfNeedNotifyUser()) {
            KeepAliveService.start(this)
        }
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