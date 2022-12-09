package com.jamgu.hwstatistics.keeplive.utils;

import static com.jamgu.hwstatistics.RouterKt.AUTO_MONITOR_START_FROM_NOTIFICATION;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.jamgu.common.util.log.JLog;
import com.jamgu.hwstatistics.R;
import com.jamgu.hwstatistics.TransitionActivity;

import java.util.List;

/**
 *
 */
public class KeepLiveUtils {
    /**
     * 判断是否在电池优化的白名单中
     *
     * @param context
     * @return
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        boolean isIgnore = false;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isIgnore = pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return isIgnore;
    }

    /**
     * 将应用添加到电池优化白名单中
     *
     * @param context
     */
    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (isIgnoringBatteryOptimizations(context)) {
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String TAG = "KeepLiveUtils";

    public static boolean isBackgroundProcess(Context context) {

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        boolean isBackground = true;
        String processName = "empty";
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                processName = appProcess.processName;
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                    isBackground = true;
                } else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        || appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    isBackground = false;
                } else {
                    isBackground = true;
                }
            }
        }

        JLog.d(TAG, "是否在后台：" + isBackground + ", processName: " + processName);

        return isBackground;
    }

    public static void startCallActivityVersionHigh(Context context, int contentTextID, Class<? extends Activity> pdActivity) {
        if (context == null) return;

        if (contentTextID <= 0) {
            contentTextID = R.string.click_to_start;
        }

        Intent intent = new Intent(context, pdActivity);
        intent.putExtra(AUTO_MONITOR_START_FROM_NOTIFICATION, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent
                .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        String channelID = "app_call_notification";
        String channelName = "日志APP拉起通知";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int level = NotificationManager.IMPORTANCE_HIGH;
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelID, channelName, level);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentText(context.getString(contentTextID))
                .setContentTitle(context.getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVibrate(new long[]{0, 1000L, 1000L, 1000L})
                .setContentIntent(pendingIntent)    // 点击时的intent
                .setDeleteIntent(pendingIntent)     // 被用户清除时的intent
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(100, builder.build());
    }

}
