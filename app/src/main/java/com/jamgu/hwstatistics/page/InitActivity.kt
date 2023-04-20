package com.jamgu.hwstatistics.page

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.common.widget.toast.JToast
import com.jamgu.hwstatistics.databinding.ActivityInitLayoutBinding
import com.jamgu.hwstatistics.keeplive.utils.KeepLiveUtils
import com.jamgu.hwstatistics.keeplive.utils.PhoneUtils
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.hwstatistics.net.upload.DataUploader
import com.jamgu.hwstatistics.power.permission.PermissionRequester
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
        private const val FILE_ACCESS_PERMISSION = "file_access_permission"
        const val MONITOR_INIT = "monitor_init"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataSaver.addDebugTracker(TAG, "onCreate")
    }

    override fun initWidget() {
        super.initWidget()

        val preference = PreferenceUtil.getCachePreference(this, 0)
        val isBatteryInit = preference.getBoolean(BATTERY_INIT, false)
        val isUsageStatePermissionSet = preference.getBoolean(USAGE_STATE_PERMISSION, false)

        var isBatteryBtmClick = false
        var isUsageBtnClick = false
        var isFileAccessBtnClick = false

        if (isBatteryInit) {
            mBinding.vBtnBatterySave.isEnabled = false
            isBatteryBtmClick = true
        }

        if (isUsageStatePermissionSet) {
            mBinding.vBtnUsageState.isEnabled = false
            isUsageBtnClick = true
        }

        if (PermissionRequester(this).isPermissionAllGranted()) {
            mBinding.vBtnUserPermissionRequest.isEnabled = false
        }

        mBinding.vBtnBatterySave.setOnClickListener {
            if (!isBatteryInit || !KeepLiveUtils.isIgnoringBatteryOptimizations(this)) {
                KeepLiveUtils.requestIgnoreBatteryOptimizations(this)
                PhoneUtils.setReStartAction(this)
                preference.edit().putBoolean(BATTERY_INIT, true).apply()
                isBatteryBtmClick = true
            }
        }

        val mMonitorActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            preference.edit().putBoolean(USAGE_STATE_PERMISSION, true).apply()
        }
        mBinding.vBtnUsageState.setOnClickListener {
            if (!isUsageStatePermissionSet) {
                // 打开获取应用信息页面
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                mMonitorActivityLauncher.launch(intent)
                isUsageBtnClick = true
            }
        }

        mBinding.vBtnUserPermissionRequest.setOnClickListener {
            PermissionRequester(this@InitActivity).requestedPermission()
        }

        val mFileAccessBtn = mBinding.vBtnFileAccess
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isFileAccessEnable = PreferenceUtil.getCachePreference(this, 0)
                .getBoolean(FILE_ACCESS_PERMISSION, false) ?: false
            if (!isFileAccessEnable) {
                isFileAccessBtnClick = true
                mFileAccessBtn.visibility = View.GONE
            }
            mFileAccessBtn.visibility = View.VISIBLE
            val fileAccessActivityLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()) { }
            mFileAccessBtn.setOnClickListener {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + this.packageName)
                fileAccessActivityLauncher.launch(intent)
                isFileAccessBtnClick = true
                PreferenceUtil.getCachePreference(this, 0).edit()
                    .putBoolean(FILE_ACCESS_PERMISSION, isFileAccessBtnClick).apply()
            }
        } else {
            isFileAccessBtnClick = true
            PreferenceUtil.getCachePreference(this, 0).edit()
                .putBoolean(FILE_ACCESS_PERMISSION, isFileAccessBtnClick).apply()
            mFileAccessBtn.visibility = View.GONE
        }

        mBinding.vBtnRegister.setOnClickListener {
            KRouters.open(this@InitActivity, REGISTER_PAGE)
        }

        mBinding.vBtnMonitorStart.setOnClickListener {
            val username = PreferenceUtil.getCachePreference(this, 0)
                .getString(DataUploader.USER_NAME, "") ?: ""
            val isUserRegister = username.isNotEmpty()
            var tips = ""
            if (!isBatteryBtmClick) {
                tips = mBinding.vBtnBatterySave.text.toString()
            } else if (!isFileAccessBtnClick) {
                tips = mBinding.vBtnFileAccess.text.toString()
            } else if (!isUserRegister) {
                tips = mBinding.vBtnRegister.text.toString()
            } else if (!isUsageBtnClick) {
                tips = mBinding.vBtnUsageState.text.toString()
            } else if (!PermissionRequester(this).isPermissionAllGranted()) {
                tips = mBinding.vBtnUserPermissionRequest.text.toString()
            }

            if (tips.isNotEmpty()) {
                JToast.showToast(this, "$tips 相关的权限未授予或者步骤未完成噢！")
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putBoolean(AUTO_MONITOR_START_FROM_INIT, true)
            }
            KRouters.open(
                this, KRouterUriBuilder().appendAuthority(AUTO_MONITOR_PAGE)
                    .build(), bundle
            )

            preference.edit().putBoolean(MONITOR_INIT, true).apply()
            finish()
        }
    }

    override fun getViewBinding(): ActivityInitLayoutBinding = ActivityInitLayoutBinding.inflate(layoutInflater)
}