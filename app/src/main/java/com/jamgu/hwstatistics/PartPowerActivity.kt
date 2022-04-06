package com.jamgu.hwstatistics

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.databinding.ActivityPartPowerBinding
import com.jamgu.hwstatistics.util.ExcelUtil
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.settingpie.base.SettingViewBinder
import com.jamgu.settingpie.model.SetItemBuilder
import com.jamgu.settingpie.model.SetListBuilder
import com.jamgu.settingpie.model.ViewType.VIEW_TYPE_CUSTOM

private const val TAG = "PartPowerActivity"

/**
 * 输出各部件所占功耗百分比
 */
@KRouter([PART_POWER_PAGE])
class PartPowerActivity : ViewBindingActivity<ActivityPartPowerBinding>() {

    private var hasRealPc = false

    private var folderLauncher: ActivityResultLauncher<Intent>? = null

    override fun getViewBinding(): ActivityPartPowerBinding = ActivityPartPowerBinding.inflate(layoutInflater)

    override fun initWidget() {
        SetListBuilder(mBinding.vRecycler).addItem {
            SetItemBuilder()
                    .viewType(VIEW_TYPE_CUSTOM)
                    .viewBinder(SettingViewBinder(R.layout.part_power_item_checkbox_realpc) { holder, _ ->
                        val vCbRealPc = holder.itemView.findViewById<CheckBox>(R.id.vCbRealPc)
                        vCbRealPc.setOnCheckedChangeListener { view, isChecked ->
                            hasRealPc = isChecked
                        }
                    })
        }.addItem {
            SetItemBuilder()
                    .viewType(VIEW_TYPE_CUSTOM)
                    .viewBinder(SettingViewBinder(R.layout.part_power_item_btn_openfile) { holder, _ ->
                        val vBtnOpenFile = holder.itemView.findViewById<AppCompatButton>(R.id.vBtnOpenFile)
                        vBtnOpenFile.setOnClickListener {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            intent.type = "application/*"

                            folderLauncher?.launch(intent)
                        }
                    })
        }.build()
    }

    override fun initData() {
        folderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri: Uri = it.data?.data ?: return@registerForActivityResult
            JLog.i(TAG, "onActivityResult: " + "filePath：" + uri.path)
            // you can modify readExcelList, then write to excel.
            ThreadPool.runOnNonUIThread {
                PCRatioExporter.verifyAndExport(this, uri, hasRealPc)
            }
        }
    }
}