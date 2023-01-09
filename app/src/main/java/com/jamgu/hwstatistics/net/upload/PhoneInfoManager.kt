package com.jamgu.hwstatistics.net.upload

/**
 * @author jamgudev
 * @date 2022/12/12 3:38 下午
 *
 * @description 手机信息管理器：手机品牌、系统版本信息
 */
object PhoneInfoManager {

    @JvmStatic
    fun getPhoneInfo(): PhoneInfo {
        val info = PhoneInfo()
        info.product = android.os.Build.PRODUCT
        info.cpuAbis = android.os.Build.SUPPORTED_ABIS
        info.tags = android.os.Build.TAGS
        info.model = android.os.Build.MODEL
        info.sdkInt = android.os.Build.VERSION.SDK_INT
        info.version = android.os.Build.VERSION.RELEASE
        info.device = android.os.Build.DEVICE
        info.display = android.os.Build.DISPLAY
        info.brand = android.os.Build.BRAND
        info.board = android.os.Build.BOARD
        info.manufacture = android.os.Build.MANUFACTURER
        return info
    }

    data class PhoneInfo (
        var product: String? = "",
        // cpu芯片集
        var cpuAbis: Array<String>? = null,
        var tags: String? = "",
        // 设备型号
        var model: String? = "",
        // sdk 版本
        var sdkInt: Int = 0,
        // 安卓系统版本
        var version: String? = "",
        // 设备名称
        var device: String? = "",
        var display: String? = "",
        // 品牌
        var brand: String? = "",
        var board: String? = "",
        // 厂商
        var manufacture: String? = ""
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PhoneInfo

            if (product != other.product) return false
            if (cpuAbis != null) {
                if (other.cpuAbis == null) return false
                if (!cpuAbis.contentEquals(other.cpuAbis)) return false
            } else if (other.cpuAbis != null) return false
            if (tags != other.tags) return false
            if (model != other.model) return false
            if (sdkInt != other.sdkInt) return false
            if (version != other.version) return false
            if (device != other.device) return false
            if (display != other.display) return false
            if (brand != other.brand) return false
            if (board != other.board) return false
            if (manufacture != other.manufacture) return false

            return true
        }

        override fun hashCode(): Int {
            var result = product?.hashCode() ?: 0
            result = 31 * result + (cpuAbis?.contentHashCode() ?: 0)
            result = 31 * result + (tags?.hashCode() ?: 0)
            result = 31 * result + (model?.hashCode() ?: 0)
            result = 31 * result + sdkInt
            result = 31 * result + (version?.hashCode() ?: 0)
            result = 31 * result + (device?.hashCode() ?: 0)
            result = 31 * result + (display?.hashCode() ?: 0)
            result = 31 * result + (brand?.hashCode() ?: 0)
            result = 31 * result + (board?.hashCode() ?: 0)
            result = 31 * result + (manufacture?.hashCode() ?: 0)
            return result
        }
    }
}