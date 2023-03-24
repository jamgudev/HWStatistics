package com.jamgu.hwstatistics.keeplive.service;

import static com.jamgu.hwstatistics.page.PageRouterKt.AUTO_MONITOR_START_FROM_KILLED;
import static com.jamgu.hwstatistics.page.PageRouterKt.AUTO_MONITOR_START_FROM_NOTIFICATION;
import static com.jamgu.hwstatistics.util.TimeExtensionsKt.timeStamp2DateString;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jamgu.common.util.log.JLog;
import com.jamgu.hwstatistics.BaseApplication;
import com.jamgu.hwstatistics.R;
import com.jamgu.hwstatistics.appusage.AppUsageDataLoader;
import com.jamgu.hwstatistics.keeplive.forground.ForegroundNF;
import com.jamgu.hwstatistics.net.upload.DataSaver;
import com.jamgu.hwstatistics.page.AutoMonitorActivity;
import com.jamgu.hwstatistics.page.TransitionActivity;

/**
 * 创建一个JobService用于提高应用优先级
 * 这种前台保活方式，目前一定需要一个明确的通知显示，
 * 这个提示最好友好点，不然系统会提示一个后台运行的通知，很容易引导用户去关闭。
 * 如果是音乐类型的，可以直接使用音乐的服务MusicPlayerService挂接通知即可做的一定程度的保活
 */
public class KeepAliveService extends JobService {
    private static final String TAG = KeepAliveService.class.getSimpleName();
    private static final int JOB_ID = 1;

    private JobScheduler mJobScheduler;
    private ComponentName JOB_PG;
    private ForegroundNF mForegroundNF;
    private static Boolean isLoaderStarted = false;
    private AppUsageDataLoader mUsageLoader = null;

    private Handler mJobHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "pull alive.");
            Context applicationContext = getApplicationContext();

            if (mUsageLoader != null) {
                isLoaderStarted = mUsageLoader.isStarted();
            }

            // 判断Activity是否存活
            if (applicationContext instanceof BaseApplication) {
                boolean inBackStack = ((BaseApplication) applicationContext)
                        .isActivityInBackStack(AutoMonitorActivity.class);
                DataSaver.INSTANCE.addDebugTracker(TAG,
                        "inBackStack = " + inBackStack + ", isStart = " + isLoaderStarted);
                String currentContent = mForegroundNF.getCurrentContent();
                String rebootText = getString(R.string.app_being_killed_reboot);
                if (!inBackStack && !isLoaderStarted) {
                    DataSaver.INSTANCE.addInfoTracker(TAG, "检测到Activity已不在栈内");
                    if (rebootText.equals(currentContent)) {
                        return true;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(AUTO_MONITOR_START_FROM_KILLED, true);
                    initForeGroundNF(TransitionActivity.class, bundle);
                    mForegroundNF.updateContent(rebootText);
                    mForegroundNF.startForegroundNotification();
                } else if (rebootText.equals(currentContent)){
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(AUTO_MONITOR_START_FROM_NOTIFICATION, true);
                    initForeGroundNF(AutoMonitorActivity.class, bundle);
                    mForegroundNF.updateContent(getString(R.string.working_background));
                    mForegroundNF.startForegroundNotification();
                }
            }
            if (msg != null && msg.obj instanceof JobParameters) {
                jobFinished((JobParameters) msg.obj, true);
            }
            return true;
        }
    });

    public static Boolean isStarted() {
        return isLoaderStarted;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JOB_PG = new ComponentName(getPackageName(), KeepAliveService.class.getName());
        Bundle bundle = new Bundle();
        bundle.putBoolean(AUTO_MONITOR_START_FROM_NOTIFICATION, true);
        initForeGroundNF(AutoMonitorActivity.class, bundle);
        DataSaver.INSTANCE.addInfoTracker(TAG, "onCreate");
        if (mUsageLoader == null) {
            mUsageLoader = new AppUsageDataLoader(getApplicationContext());
            mUsageLoader.onCreate();
        }
        if (!mUsageLoader.isStarted()) {
            mUsageLoader.start();
        }
    }

    private void initForeGroundNF(Class<? extends Activity> activityClass, Bundle bundle) {
//        if (mForgroundNF != null) {
//            mForgroundNF.stopForegroundNotification();
//        }

        mForegroundNF = new ForegroundNF(this, timeStamp2DateString(System.currentTimeMillis()));
        Intent intent = new Intent(this, activityClass);
        if (bundle != null && !bundle.isEmpty()) {
            intent.putExtras(bundle);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        mForegroundNF.setContentIntent(pendingIntent);
    }

    /**
     * 外部只需要调用这句话就可以启动一个保活状态
     *
     * @param context   启动服务的上下文
     *                  <p>
     *                  AliveStrategy.BATTERYOPTIMIZATION：启动电量优化保活
     *                  <p>
     *                  AliveStrategy.RESTARTACTION：启动自启动白名单保活
     *                  <p>
     *                  AliveStrategy.ALL:启动电量优化，启动白名单保活
     *                  <p>
     *                  AliveStrategy.NONE：不启动上面两个任何一个
     *                  <p>
     *                  AliveStrategy.JOB_SERVICE：只使用JobService进行保活
     */
    public static void init(Context context) {
        Intent intent = new Intent(context, KeepAliveService.class);
        context.startService(intent);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");
        DataSaver.INSTANCE.addInfoTracker(TAG, "onStartJob");
        mJobHandler.sendMessage(Message.obtain(mJobHandler, 1, params));
        return true;
    }

    /**
     * 系统回调使用，说明触发了job条件
     *
     * @param params params
     * @return boolean
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob");
        mJobHandler.sendEmptyMessage(1);
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        JobInfo job = initJob();
        mJobScheduler.cancel(JOB_ID);
        mJobScheduler.schedule(job);
        JLog.d(TAG, "onStartCommand");
        startNotificationForeground();
        boolean isStarted = false;
        if (mUsageLoader != null) {
            isStarted = mUsageLoader.isStarted();
        }
        DataSaver.INSTANCE.addInfoTracker(TAG,
                "onStartCommand " + "mUsageLoader = " + mUsageLoader + ", isStarted = " + isStarted);

        if (mUsageLoader == null) {
            mUsageLoader = new AppUsageDataLoader(getApplicationContext());
            mUsageLoader.onCreate();
        }

        if (!mUsageLoader.isStarted()) {
            mUsageLoader.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 大于18可以使用一个取消Notification的服务
     */
    private void startNotificationForeground() {
        mForegroundNF.startForegroundNotification();
    }

    /**
     * 初始化Job任务
     *
     * @return JobInfo
     */
    @SuppressLint("ObsoleteSdkInt")
    private JobInfo initJob() {
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, JOB_PG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
            builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);  //执行的最长延时时间
            builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS,
                    JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
        } else {
            builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
        }
        builder.setPersisted(false);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
        builder.setRequiresCharging(false);
        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUsageLoader != null) {
            mUsageLoader.onDestroy();
            mUsageLoader = null;
        }
        if (mForegroundNF != null) {
            mForegroundNF.stopForegroundNotification();
        }
        mJobHandler = null;
        JLog.d(TAG, "onDestroy");
        DataSaver.INSTANCE.addInfoTracker(TAG, "onDestroy.");
    }

}
