package com.jamgu.hwstatistics.power.permission

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.widget.toast.JToast
import com.jamgu.hwstatistics.power.mobiledata.bluetooth.BLEManager
import com.jamgu.hwstatistics.power.mobiledata.network.NetWorkManager
import com.permissionx.guolindev.PermissionX

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2023/3/19 14:04
 *
 * @description
 */
class PermissionRequester(private val mContext: Context) {

    fun needPermissions(): ArrayList<String> {
        return ArrayList<String>().apply {
            addAll(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
            addAll(NetWorkManager.permission())
            addAll(BLEManager.permission())
        }
    }

    fun requestedPermission(): Boolean {
        val permissions = needPermissions()
        val notGrantedPermission = permissions.filterNot { PermissionX.isGranted(mContext, it) }
        return if (notGrantedPermission.isEmpty()) {
            true
        } else {
            requestPermission(notGrantedPermission)
            false
        }
    }

    fun isPermissionAllGranted(): Boolean {
        return needPermissions().filterNot { PermissionX.isGranted(mContext, it) }.isEmpty()
    }

    private fun requestPermission(notGrantedPermission: List<String>) {
        if (mContext is FragmentActivity) {
            PermissionX.init(mContext)
                .permissions(notGrantedPermission)
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(
                        deniedList, "获取网络类型需要申请读取手机状态权限",
                        "好的", "拒绝"
                    )
                }
                .onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(
                        deniedList, "You need to allow necessary permissions in Settings manually",
                        "OK", "Cancel"
                    )
                }
                .request { allGranted, _, deniedList ->
                    if (allGranted) {
//                    start()
                    } else {
                        ThreadPool.runUITask {
                            Toast.makeText(
                                mContext,
                                "These permissions are denied: $deniedList",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                }
        } else {
            JToast.showToast(mContext, "context is not type of activity")
        }
    }

}