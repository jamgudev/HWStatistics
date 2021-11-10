package com.jamgu.hwstatistics.keepalive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jamgu.hwstatistics.MainActivity
import com.jamgu.hwstatistics.R

/**
 * Created by jamgu on 2021/10/14
 */
class ForegroundCoreService: Service() {

    override fun onBind(p0: Intent?): IBinder? = null

    private val mForegroundNotification: ForegroundNotification by lazy {
        ForegroundNotification(this)
    }

    override fun onCreate() {
        super.onCreate()
        mForegroundNotification.startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            // 服务被系统kill之后重启进来的
            // START_NOT_STICKY: 被杀后自动重启，保持启动状态，不保持Intent，
            // 重新调用onStartCommand，无新Intent则为空Intent—杀死重启后，
            // 不继续执行先前任务，能接受新任务
            return START_NOT_STICKY
        }
        mForegroundNotification.startForegroundNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mForegroundNotification.stopForegroundNotification()
        super.onDestroy()
    }

}

/**
 * 初始化前台通知，停止前台通知
 */
class ForegroundNotification(private val service: ForegroundCoreService) : ContextWrapper(service) {
    companion object {
        private const val START_ID = 101
        private const val CHANNEL_ID = "app_foreground_service"
        private const val CHANNEL_NAME = "前台保活服务"
    }
    private var mNotificationManager: NotificationManager? = null

    private var mCompatBuilder: NotificationCompat.Builder?=null

    private val compatBuilder: NotificationCompat.Builder?
        get() {
            if (mCompatBuilder == null) {
                val notificationIntent = Intent(this, MainActivity::class.java)
                notificationIntent.action = Intent.ACTION_MAIN
                notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                //动作意图
                val pendingIntent = PendingIntent.getActivity(
                    this, (Math.random() * 10 + 10).toInt(),
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )
                val notificationBuilder: NotificationCompat.Builder =
                    NotificationCompat.Builder(this, CHANNEL_ID)
                //标题
                notificationBuilder.setContentTitle("keep alive")
                //通知内容
                notificationBuilder.setContentText("keeping alive...")
                //状态栏显示的小图标
                notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
                //通知内容打开的意图
                notificationBuilder.setContentIntent(pendingIntent)
                mCompatBuilder = notificationBuilder
            }
            return mCompatBuilder
        }

    init {
        createNotificationChannel()
    }

    //创建通知渠道
    private fun createNotificationChannel() {
        mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //针对8.0+系统
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setShowBadge(false)
            mNotificationManager?.createNotificationChannel(channel)
        }
    }

    //开启前台通知
    fun startForegroundNotification() {
        service.startForeground(START_ID, compatBuilder?.build())
    }

    //停止前台服务并清除通知
    fun stopForegroundNotification() {
        mNotificationManager?.cancelAll()
        service.stopForeground(true)
    }
}
