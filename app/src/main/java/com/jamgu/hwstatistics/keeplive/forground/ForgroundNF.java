package com.jamgu.hwstatistics.keeplive.forground;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.jamgu.hwstatistics.R;

public class ForgroundNF {
    private static final int START_ID = 101;
    private static final String CHANNEL_ID = "app_foreground_service";
    private static final String CHANNEL_NAME = "前台保活服务";

    private final Service service;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder mNotificationCompatBuilder;

    public ForgroundNF(Service service) {
        this.service = service;
        initNotificationManager();
        initCompatBuilder(service.getBaseContext());
    }


    /**
     * 初始化NotificationCompat.Builder
     * 这个提示最好友好点，不然系统会提示一个后台运行的通知，很容易引导用户去关闭
     */
    private void initCompatBuilder(Context context) {
        if (context == null) return;
        mNotificationCompatBuilder = new NotificationCompat.Builder(service, CHANNEL_ID);
        //标题
        mNotificationCompatBuilder.setContentTitle(context.getString(R.string.app_name));
        //通知内容
        mNotificationCompatBuilder.setContentText(context.getString(R.string.working_background));
        mNotificationCompatBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
    }

    /**
     * 初始化notificationManager并创建NotificationChannel
     */
    private void initNotificationManager() {
        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        //针对8.0+系统
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void setContentIntent(PendingIntent intent) {
        if (mNotificationCompatBuilder != null) {
            mNotificationCompatBuilder.setContentIntent(intent);
            mNotificationCompatBuilder.setAutoCancel(true);
        }
    }

    public void startForegroundNotification() {
        service.startForeground(START_ID, mNotificationCompatBuilder.build());
    }

    public void stopForegroundNotification() {
        if (notificationManager != null)
            notificationManager.cancelAll();
        if (service != null)
            service.stopForeground(true);
    }

}
