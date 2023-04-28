package com.jamgu.hwstatistics.page

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import androidx.core.content.getSystemService
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.Common
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.common.util.statusbar.StatusBarUtil
import com.jamgu.common.widget.toast.JToast
import com.jamgu.hwstatistics.BaseApplication
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.databinding.ActivityAutoMonitorBinding
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.hwstatistics.net.upload.DataUploader
import com.jamgu.hwstatistics.page.InitActivity.Companion.MONITOR_INIT
import com.jamgu.hwstatistics.power.StatisticAdapter
import com.jamgu.hwstatistics.util.timeStamp2SimpleDateString
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters
import com.jamgu.settingpie.model.LayoutProp
import com.jamgu.settingpie.model.SetConstants.DEFAULT_DIVIDER_COLOR
import com.jamgu.settingpie.model.SetItemBuilder
import com.jamgu.settingpie.model.SetListBuilder
import com.jamgu.settingpie.model.ViewType.VIEW_TYPE_NORMAL
import com.jamgu.settingpie.model.ViewType.VIEW_TYPE_TEXT_TITLE
import java.util.concurrent.atomic.AtomicBoolean


@KRouter(value = [AUTO_MONITOR_PAGE], booleanParams = [AUTO_MONITOR_START_FROM_NOTIFICATION, AUTO_MONITOR_START_FROM_BOOT,
    AUTO_MONITOR_START_FROM_KILLED, AUTO_MONITOR_START_FROM_INIT, AUTO_MONITOR_START_FROM_AUTO_START])
class AutoMonitorActivity : ViewBindingActivity<ActivityAutoMonitorBinding>() {

    companion object {
        private const val TAG = "AutoMonitorActivity"
    }

//    private val mAppUsageDataLoader: AppUsageDataLoader = AppUsageDataLoader(this).apply {
//        setOnSessionListener(object : AppUsageDataLoader.IOnUserSessionListener {
//            override fun onSessionBegin() {
//            }
//
//            override fun onSessionEnd(session: UsageRecord.SingleSessionRecord) {
//            }
//        })
//    }
    private var mStartTime: Long? = null
    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()
    private var mShowTime: Boolean = false
    private var mKeepLiveServiceOpen: Boolean = false
    private var isInit = AtomicBoolean(false)
    private var mEnableAutoStart = false

    override fun isBackPressedNeedConfirm(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        StatusBarUtil.setStatusBarColor(this, R.color.colorPrimary)
        super.onCreate(savedInstanceState)
//        mAppUsageDataLoader.onCreate()

        // 加入任务栈
        (applicationContext as? BaseApplication)?.addThisActivityToRunningActivities(this.javaClass)

        val isStartFromNormalNotification =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_NOTIFICATION) ?: false

        val startFromBoot =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_BOOT) ?: false

        val startFromAutoStart =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_AUTO_START) ?: false

        val startFromKilled =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_KILLED) ?: false

        val trace = if (isStartFromNormalNotification) {
            intent.extras?.putBoolean(AUTO_MONITOR_START_FROM_NOTIFICATION, false)
            "正常从前台服务重启"
        } else if (startFromBoot) {
            intent.extras?.putBoolean(AUTO_MONITOR_START_FROM_BOOT, false)
            "开机通过通知重启"
        } else if (startFromAutoStart) {
            intent.extras?.putBoolean(AUTO_MONITOR_START_FROM_AUTO_START, false)
            "通过自启动通知重启"
        } else if (startFromKilled) {
            intent.extras?.putBoolean(AUTO_MONITOR_START_FROM_KILLED, false)
            "异常从前台服务重启"
        } else {
            "点击icon重启"
        }
        mEnableAutoStart = startFromBoot || startFromKilled || isStartFromNormalNotification
        DataSaver.addDebugTracker(TAG, "onCreate, $trace")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onResume() {
        super.onResume()
        val preference = PreferenceUtil.getCachePreference(this, 0)
        val isInit = preference.getBoolean(MONITOR_INIT, false)
        if (!isInit) {
            ThreadPool.runUITask({
                KRouters.open(this, KRouterUriBuilder().appendAuthority(INIT_PAGE).build())
            }, 400)
        } else {
            // 没启动且不是由初始化页面过来的：简而言之，初始化后回到activity，只要没启动都会启动
//            val fromInit = intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_INIT, false) ?: false
//            if (!mAppUsageDataLoader.isStarted()) {
//                if (!fromInit) {
//                    mAppUsageDataLoader.start()
//                    mStartTime = System.currentTimeMillis()
//                }
//            }

            checkResumeEnterPoint()

            mStartTime?.let { startTime ->
                val duration = System.currentTimeMillis() - startTime
                mData.add("Session Duration:" + duration.timeStamp2SimpleDateString())
                mAdapter.notifyItemInserted(mData.size - 1)
            }

            // 保活前台服务
            if (!mKeepLiveServiceOpen) {
                KeepAliveService.start(Common.getInstance().getApplicationContext())
                mKeepLiveServiceOpen = true
                mStartTime = System.currentTimeMillis()
            }

            ThreadPool.runUITask ({
                if (mKeepLiveServiceOpen) {
                    mBinding.vStart.text = getString(R.string.already_started)
                    mBinding.vStart.isEnabled = false
                }
            }, 100L)

            this.isInit.set(true)
        }
        initSettings()
    }

    private fun initSettings() {
        SetListBuilder(mBinding.vSettingRecycler)
            .arrowOfTheme(false)
            .paddingPair(12, 10)
            .decorationOfTheme(2, 0, 0, null)
            .decorationOfGroup(10, DEFAULT_DIVIDER_COLOR)
            .showDecoration(true)
            .addItem {
                SetItemBuilder().viewType(VIEW_TYPE_TEXT_TITLE).mainText("设置", 18, "#FFFFFF").
                layoutProp(LayoutProp(R.color.colorPrimary) {})
            }
            .addItem {
            val username = PreferenceUtil.getCachePreference(this, 0)
                .getString(DataUploader.USER_NAME, "unknown") ?: "unknown"
            SetItemBuilder().mainText("用户名").hintText(username).layoutProp(LayoutProp("#FFFFFF") {
                val service = this@AutoMonitorActivity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                service?.setPrimaryClip(ClipData(username, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), ClipData.Item(username)))
                JToast.showToast(this, "已复制内容")
            }).viewType(VIEW_TYPE_NORMAL)
        }.addItem {
            SetItemBuilder().mainText("清空缓存").layoutProp(LayoutProp {
                ThreadPool.runIOTask {
                    DataSaver.clearUploadedCacheFile(DataSaver.getCacheRootPath())
                    JToast.showToast(this, "缓存已清空")
                }
            }).viewType(VIEW_TYPE_NORMAL).showArrow(true)
        }.addGroupItem {
            SetItemBuilder().mainText("回到初始化界面").layoutProp(LayoutProp {
                KRouters.open(this@AutoMonitorActivity, INIT_PAGE)
            }).viewType(VIEW_TYPE_NORMAL).showArrow(true)
        }.build()
    }

    private fun checkResumeEnterPoint() {
        val isStartFromNormalNotification =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_NOTIFICATION) ?: false

        val startFromBoot =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_BOOT) ?: false

        val startFromKilled =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_KILLED) ?: false

        val trace = if (isStartFromNormalNotification) {
            "正常从前台服务进入"
        } else if (startFromBoot) {
            "开机通过通知进入"
        } else if (startFromKilled) {
            "异常从前台服务进入"
        } else {
            "点击icon进入"
        }

        DataSaver.addDebugTracker(TAG, "onResume --------> $trace, isStarted = ${KeepAliveService.isStarted()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        JLog.d(TAG, "onDestroy")
        val activityManager = getSystemService<ActivityManager>()

        var currentMemoryInfo: ActivityManager.RunningAppProcessInfo? = null
        kotlin.run {
            activityManager?.runningAppProcesses?.forEach {
                if (it.pid == android.os.Process.myPid()) {
                    currentMemoryInfo = it
                    return@run
                }
            }
        }
        ActivityManager.getMyMemoryState(currentMemoryInfo)
        DataSaver.addDebugTracker(TAG, "onDestroy called, memState = ${currentMemoryInfo?.lastTrimLevel}")
        DataSaver.flushTestData()
        DataUploader.uploadFile(this, DataSaver.getINFODataCachePath())

//        mAppUsageDataLoader.onDestroy()

        (applicationContext as? BaseApplication)?.removeThisActivityFromRunningActivities(this.javaClass)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initWidget() {
        super.initWidget()

        mAdapter.setData(mData)
        mBinding.vRecycler.adapter = mAdapter
        mBinding.vRecycler.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        mBinding.vRecycler.layoutManager = LinearLayoutManager(this)

        mBinding.vShowTime.setOnClickListener {
            mShowTime = !mShowTime
            mBinding.vShowTime.text = if (mShowTime) "不显示时间戳" else "显示时间戳"
        }
        mBinding.vStart.setOnClickListener {
//            if (!mAppUsageDataLoader.isStarted()) {
//                mData.clear()
//                mAdapter.notifyDataSetChanged()
//                mAppUsageDataLoader.start()
//                mStartTime = System.currentTimeMillis()
//                mBinding.vStart.text = getString(R.string.already_started)
//                mBinding.vStart.isEnabled = false
//            }
        }
        mBinding.vDrawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                mBinding.vContentContainer.translationX = (drawerView.width * slideOffset)
            }
        })
        mBinding.vMenu.setOnClickListener {
            mBinding.vDrawer.openDrawer(Gravity.LEFT, true)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.repeatCount == 0) {
            // 抽屉如果是打开状态，先关闭抽屉
            if (mBinding.vDrawer.isDrawerOpen(mBinding.vSettingContainer)) {
                mBinding.vDrawer.closeDrawer(Gravity.LEFT)
                return false
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * OnLowMemory是Android提供的API，在系统内存不足，
     * 所有后台程序（优先级为background的进程，不是指后台运行的进程）都被杀死时，系统会调用OnLowMemory。
     */
    override fun onLowMemory() {
        super.onLowMemory()
        JLog.d(TAG, "onLowMemory")
        DataSaver.addDebugTracker(TAG, "onLowMemory called.")
    }

    /**
     *
    TRIM_MEMORY_COMPLETE：内存不足，并且该进程在后台进程列表最后一个，马上就要被清理
    TRIM_MEMORY_MODERATE：内存不足，并且该进程在后台进程列表的中部。
    TRIM_MEMORY_BACKGROUND：内存不足，并且该进程是后台进程。
    TRIM_MEMORY_UI_HIDDEN：内存不足，并且该进程的UI已经不可见了。
    以上4个是4.0增加
    TRIM_MEMORY_RUNNING_CRITICAL：内存不足(后台进程不足3个)，这个回调的下一个阶段就是 onLowMemory
    TRIM_MEMORY_RUNNING_LOW：内存不足(后台进程不足5个)，并且该进程优先级比较高，需要清理内存
    TRIM_MEMORY_RUNNING_MODERATE：内存不足(后台进程超过5个)，并且该进程优先级比较高，需要清理内存
    以上3个是4.1增加

    通过一键清理后，OnLowMemory不会被触发，而OnTrimMemory会被触发一次(有些手机如一加，不会触发), level = 80
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    /**
     * 当用户按下Home键 app处于后台，此时会调用onSaveInstanceState 方法
    当用户按下电源键时，会调用onSaveInstanceState 方法
    当Activity进行横竖屏切换的时候也会调用onSaveInstanceState 方法
    从BActivity跳转到CActivity的时候 BActivity也会调用onSaveInstanceState 方法
     */
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        JLog.d(TAG, "onSaveInstanceState")
//        saveData()
//        // 打开拉起通知
//        KeepLiveUtils.startCallActivityVersionHigh(
//            this,
//            R.string.app_being_killed_reboot, AutoMonitorActivity::class.java
//        )
//    }

    override fun getViewBinding(): ActivityAutoMonitorBinding =
        ActivityAutoMonitorBinding.inflate(layoutInflater)
}