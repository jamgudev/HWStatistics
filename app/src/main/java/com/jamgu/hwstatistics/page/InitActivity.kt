package com.jamgu.hwstatistics.page

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.databinding.ActivityInitLayoutBinding
import com.jamgu.hwstatistics.keeplive.utils.KeepLiveUtils
import com.jamgu.hwstatistics.keeplive.utils.PhoneUtils
import com.jamgu.hwstatistics.upload.DataSaver
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters

/**
 * @author jamgudev
 * @date 2022/12/6 9:17 下午
 *
 * @description
 */
@KRouter([INIT_PAGE])
class InitActivity : ViewBindingActivity<ActivityInitLayoutBinding>() {

    companion object {
        private const val TAG = "InitActivity"
        private const val BATTERY_INIT = "battery_init"
        private const val USAGE_STATE_PERMISSION = "usage_state_permission"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataSaver.addTestTracker(this, "$TAG, onCreate")
    }

    override fun initWidget() {
        super.initWidget()

        val preference = PreferenceUtil.getCachePreference(this, 0)
        val isBatteryInit = preference.getBoolean(BATTERY_INIT, false)
        val isUsageStatePermissionSet = preference.getBoolean(USAGE_STATE_PERMISSION, false)

        if (isBatteryInit) {
            mBinding.vBtnBatterySave.isEnabled = false
        }

        if (isUsageStatePermissionSet) {
            mBinding.vBtnUsageState.isEnabled = false
        }

        mBinding.vBtnBatterySave.setOnClickListener {
            if (!isBatteryInit || !KeepLiveUtils.isIgnoringBatteryOptimizations(this)) {
                KeepLiveUtils.requestIgnoreBatteryOptimizations(this)
                PhoneUtils.setReStartAction(this)
                preference.edit().putBoolean(BATTERY_INIT, true).apply()
            }
        }

        mBinding.vBtnUsageState.setOnClickListener {
            if (!isUsageStatePermissionSet) {
                // 打开获取应用信息页面
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivityForResult(intent, 0)
                preference.edit().putBoolean(USAGE_STATE_PERMISSION, true).apply()
            }
        }

        mBinding.vBtnMonitorStart.setOnClickListener {
            KRouters.open(
                this, KRouterUriBuilder().appendAuthority(AUTO_MONITOR_PAGE)
                    .with(AUTO_MONITOR_START_FROM_INIT, true).build()
            )
        }
    }

    override fun getViewBinding(): ActivityInitLayoutBinding = ActivityInitLayoutBinding.inflate(layoutInflater)
}