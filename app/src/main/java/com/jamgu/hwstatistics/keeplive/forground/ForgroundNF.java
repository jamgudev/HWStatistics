package com.jamgu.hwstatistics.keeplive.forground;

import static com.jamgu.hwstatistics.RouterKt.AUTO_MONITOR_START_FROM_NOTIFICATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.jamgu.common.util.log.JLog;
import com.jamgu.hwstatistics.AutoMonitorActivity;
import com.jamgu.hwstatistics.R;

public class ForgroundNF {
    private static final String TAG = "ForgroundNF";
    private static final int START_ID = 101;
    private static final String CHANNEL_ID = "app_foreground_service";
    private static final String CHANNEL_NAME = "前台保活服务";

    private final Service service;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder mNotificationCompatBuilder;

    private String mChannelID = "";
    public ForgroundNF(Service service, String channelID) {
        this.service = service;
        mChannelID = channelID;
        initNotificationManager(channelID);
        initCompatBuilder(service.getBaseContext());
    }


    /**
     * 初始化NotificationCompat.Builder
     * 这个提示最好友好点，不然系统会提示一个后台运行的通知，很容易引导用户去关闭
     */
    private void initCompatBuilder(Context context) {
        if (context == null) return;
        JLog.d(TAG, "initCompatBuilder");
        mNotificationCompatBuilder = new NotificationCompat.Builder(service, mChannelID);
        //标题
        mNotificationCompatBuilder.setContentTitle(context.getString(R.string.app_name) + " " + mChannelID);
        //通知内容
        mNotificationCompatBuilder.setContentText(context.getString(R.string.working_background));
        mNotificationCompatBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
    }

    /**
     * 初始化notificationManager并创建NotificationChannel
     */
    private void initNotificationManager(String channelID) {
        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        //针对8.0+系统
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, CHANNEL_NAME + " " + channelID, NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void updateContent(String content) {
        if (mNotificationCompatBuilder != null) {
            mNotificationCompatBuilder.setContentText(content);
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
