package com.jamgu.hwstatistics.appusage.broadcast

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.BaseApplication
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.hwstatistics.page.AUTO_MONITOR_START_FROM_BOOT
import com.jamgu.hwstatistics.page.AutoMonitorActivity
import com.jamgu.hwstatistics.page.TRANSITION_PAGE
import com.jamgu.hwstatistics.page.TransitionActivity
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters

/**
 * @author jamgudev
 * @date 2022/11/20 8:45 下午
 *
 * @description 手机开屏、息屏、开机、关机广播接收器
 */
class PhoneCycleBroadcastReceiver @JvmOverloads constructor(private val listener: IOnScreenStateChanged? = null) : BroadcastReceiver() {

    /**
     * 关机重启时获取开机通知，系统会调用该无参构造方法
     */
//    constructor(): this(null)

    companion object {
        private const val TAG = "ActiveBroadcastReceiver"
    }

    fun registerReceiver(context: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        // 8.0 后，只能通过动态注册
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED)
            intentFilter.addAction(Intent.ACTION_SHUTDOWN)
        }
        context.registerReceiver(this, intentFilter)
    }

    fun unRegisterReceiver(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                context?.let {
                    listener?.onPhoneBootComplete()
                    JLog.d(TAG, "ACTION_BOOT_COMPLETED")
                    val applicationContext = context.applicationContext
                    if (applicationContext is BaseApplication) {
                        val inBackStack = applicationContext.isActivityInBackStack(AutoMonitorActivity::class.java)
                        if (inBackStack) {
                            DataSaver.addDebugTracker(TAG, "手机重启，但Activity已经在后台，不弹通知")
                            return
                        }
                    }
                    DataSaver.addDebugTracker(TAG, "手机重启拉起通知")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startCallActivityVersionHigh(
                            context,
                            R.string.click_to_start, TransitionActivity::class.java
                        )
                    } else {
                        startActivityVersionLow(context)
                    }
                }
            }
            Intent.ACTION_SHUTDOWN -> {
                JLog.d(TAG, "ACTION_SHUTDOWN")
                listener?.onPhoneShutdown()
            }
            Intent.ACTION_SCREEN_ON -> {
                listener?.onScreenOn()
            }
            Intent.ACTION_SCREEN_OFF -> {
                listener?.onScreenOff()
            }
            Intent.ACTION_USER_PRESENT -> {
                listener?.onUserPresent()
            }
        }
    }

    private fun startCallActivityVersionHigh(context: Context?, contentTextID: Int, pdActivity: Class<out Activity?>?) {
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

    /**
     * 8.0 以下实现
     */
    private fun startActivityVersionLow(context: Context) {
        val params = KRouterUriBuilder(TRANSITION_PAGE)
            .build()
        KRouters.open(context, params)
    }

    open class SimpleStateChanged: IOnScreenStateChanged {
        override fun onPhoneBootComplete() {
        }

        override fun onPhoneShutdown() {
        }

        override fun onScreenOn() {
        }

        override fun onScreenOff() {
        }

        override fun onUserPresent() {
        }

    }

    interface IOnScreenStateChanged {

        fun onPhoneBootComplete()

        fun onPhoneShutdown()

        fun onScreenOn()

        fun onScreenOff()

        fun onUserPresent()

    }

}