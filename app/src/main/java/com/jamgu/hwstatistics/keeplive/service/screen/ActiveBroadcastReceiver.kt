package com.jamgu.hwstatistics.keeplive.service.screen

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.*
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/11/20 8:45 下午
 *
 * @description 手机开屏、息屏、开机、关机广播接收器
 */
class ActiveBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ActiveBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                context?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startActivityVersionHigh(context)
                    } else {
                        startActivityVersionLow(context)
                    }
                }
            }
            Intent.ACTION_SHUTDOWN -> {
                JLog.d(TAG, "shutdown")
            }
            Intent.ACTION_SCREEN_ON -> {
                JLog.d(TAG, "screen on")
            }
            Intent.ACTION_SCREEN_OFF -> {
                JLog.d(TAG, "screen off")
            }
            Intent.ACTION_USER_PRESENT -> {
                JLog.d(TAG, "user present")
            }
        }
    }

    /**
     * 8.0 以下实现
     */
    private fun startActivityVersionLow(context: Context) {
        val params = KRouterUriBuilder(TRANSITION_PAGE)
            .build()
        KRouters.open(context, params)
    }

    /***
     * 高版本的实现
     * @param context
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startActivityVersionHigh(context: Context) {
        val intent = Intent(context, TransitionActivity::class.java).apply {
            putExtra(AUTO_MONITOR_START_FROM_NOTIFICATION, true)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelID = "app_reboot_service"
        val channelNAME = "开机启动服务"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val level = NotificationManager.IMPORTANCE_HIGH
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(channelID, channelNAME, level)
            manager?.createNotificationChannel(channel);
        }
        val builder = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentText(context.getString(R.string.click_to_start))
            .setContentTitle(context.getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVibrate(arrayOf(0, 1000L, 1000L, 1000L).toLongArray())
            .setContentIntent(pendingIntent)    // 点击时的intent
            .setDeleteIntent(pendingIntent)     // 被用户清除时的intent
            .setAutoCancel(true)
        val notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(100, builder.build())
    }
}