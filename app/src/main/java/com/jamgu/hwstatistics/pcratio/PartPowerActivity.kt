package com.jamgu.hwstatistics.pcratio

import android.content.Intent
import android.net.Uri
import android.widget.CheckBox
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import com.jamgu.common.event.EventCenter
import com.jamgu.common.event.IEventHandler
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.PART_POWER_PAGE
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.databinding.ActivityPartPowerBinding
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.settingpie.base.SettingViewBinder
import com.jamgu.settingpie.model.SetItemBuilder
import com.jamgu.settingpie.model.SetListBuilder
import com.jamgu.settingpie.model.ViewType.VIEW_TYPE_CUSTOM

private const val TAG = "PartPowerActivity"
private const val EVENT_PARAM_READY = "EVENT_PARAM_READY"

/**
 * 输出各部件所占功耗百分比
 */
@KRouter([PART_POWER_PAGE])
class PartPowerActivity : ViewBindingActivity<ActivityPartPowerBinding>(), IEventHandler {

    private val mPartPowerExporter: PCRatioExporter = PCRatioExporter(false)

    private var isParamReady: Boolean = false

    private var testFolderLauncher: ActivityResultLauncher<Intent>? = null
    private var paramFolderLauncher: ActivityResultLauncher<Intent>? = null

    override fun getViewBinding(): ActivityPartPowerBinding = ActivityPartPowerBinding.inflate(layoutInflater)

    override fun initWidget() {
        SetListBuilder(mBinding.vRecycler).addItem {
            SetItemBuilder()
                    .viewType(VIEW_TYPE_CUSTOM)
                    .viewBinder(SettingViewBinder(R.layout.part_power_get_theta) { holder, _ ->
                        holder.itemView.findViewById<AppCompatButton>(R.id.vBtnExtractParam).setOnClickListener {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            intent.type = "application/*"
                            paramFolderLauncher?.launch(intent)
                        }
                    })
        }.addItem {
            SetItemBuilder()
                    .viewType(VIEW_TYPE_CUSTOM)
                    .viewBinder(SettingViewBinder(R.layout.part_power_item_checkbox_realpc) { holder, _ ->
                        val vCbRealPc = holder.itemView.findViewById<CheckBox>(R.id.vCbRealPc)
                        vCbRealPc.setOnCheckedChangeListener { _, isChecked ->
                            mPartPowerExporter.hasRealPc = isChecked
                        }
                    })
        }.addItem {
            SetItemBuilder()
                    .viewType(VIEW_TYPE_CUSTOM)
                    .viewBinder(SettingViewBinder(R.layout.part_power_item_checkbox_realpc) { holder, _ ->
                        val vCbRealPc = holder.itemView.findViewById<CheckBox>(R.id.vCbRealPc)
                        vCbRealPc.text = "输出为功耗百分比"
                        vCbRealPc.setOnCheckedChangeListener { _, isChecked ->
                            mPartPowerExporter.setOutPutFormat(
                                if (isChecked) OUTPUT_FORMAT_PERCENTAGE else OUTPUT_FORMAT_RAW
                            )
                        }
                    })
        }.addItem {
            SetItemBuilder()
                    .viewType(VIEW_TYPE_CUSTOM)
                    .viewBinder(SettingViewBinder(R.layout.part_power_item_btn_openfile) { holder, _ ->
                        val vBtnOpenFile = holder.itemView.findViewById<AppCompatButton>(R.id.vBtnOpenFile)
                        vBtnOpenFile.setOnClickListener {
                            if (!isParamReady) {
                                showToast("请先选择参数文件，进行了参数初始化后再操作")
                                return@setOnClickListener
                            }
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            intent.type = "application/*"

                            testFolderLauncher?.launch(intent)
                        }
                    })
        }.build()
    }

    override fun initData() {
        testFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri: Uri = it.data?.data ?: return@registerForActivityResult
            JLog.i(TAG, "onActivityResult: " + "filePath：" + uri.path)
            // you can modify readExcelList, then write to excel.
            ThreadPool.runOnNonUIThread {
                mPartPowerExporter.verifyAndExport(this, uri)
            }
        }
        paramFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri: Uri = it.data?.data ?: return@registerForActivityResult
            JLog.i(TAG, "onActivityResult: " + "filePath：" + uri.path)
            // you can modify readExcelList, then write to excel.
            ThreadPool.runOnNonUIThread {
                try {
                    val params = DataExtractor.getParamData(this, uri)
                    if (params != null) {
                        mPartPowerExporter.initParams(params)
                        showToast("params loaded.")
                        EventCenter.getInstance().postEvent(EVENT_PARAM_READY, true)
                    }
                } catch (e: Exception) {
                    EventCenter.getInstance().postEvent(EVENT_PARAM_READY, false)
                }
            }
        }
        EventCenter.getInstance().regEvent(EVENT_PARAM_READY, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventCenter.getInstance().unRegEvent(EVENT_PARAM_READY, this)
    }

    override fun onEvent(data: Any?) {
        isParamReady = data as? Boolean ?: false
    }
}