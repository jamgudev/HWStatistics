package com.jamgu.hwstatistics.keeplive.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import com.jamgu.common.util.log.JLog;

import java.util.List;

/**
 *
 */
public class KeepLiveUtils {
    /**判断是否在电池优化的白名单中
     * @param context
     * @return
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        boolean isIgnore = false;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isIgnore = pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return isIgnore;
    }

    /**将应用添加到电池优化白名单中
     * @param context
     */
    public static void requestIgnoreBatteryOptimizations(Context context) {
        if(isIgnoringBatteryOptimizations(context)){
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:"+context.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static final String TAG = "Utils";

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

}
