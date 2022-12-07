package com.jamgu.hwstatistics.mobiledata.network;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.List;

/**
 * CnPeng 1/22/21
 * 功用：网络工具类
 * 其他：
 */
class CpNetUtil {
    private static final CpNetUtil ourInstance = new CpNetUtil();
    private final        String    TAG         = getClass().getSimpleName();

    static CpNetUtil getInstance() {
        return ourInstance;
    }

    private CpNetUtil() {
    }

    /**
     * CnPeng:1/22/21 10:11 AM 官方文档参考：https://developer.android.google.cn/training/basics/network-ops/reading-network-state
     *
     * ConnectivityManager 管理器 | 系统连接状态：the state of connectivity in the system
     * Network             当前连接的网络对象  | ，网络切换后会变更为新的 Network 对象：one of the networks that the device is currently connected to
     *
     * LinkProperties      网络详细信息 | 如 DNS、IP 、interface name 、proxy
     * NetworkCapabilities 网络属性 | properties of a network, such as the transports (Wi-Fi, cellular, Bluetooth) and what the network is capable o
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public CpNetEnum getNetType(Context ctx) throws SecurityException {
        ConnectivityManager connectManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectManager.getActiveNetwork();

            NetworkCapabilities capabilities = connectManager.getNetworkCapabilities(network);

            if (null == capabilities || !isNetConnected(capabilities)) {
                return CpNetEnum.TYPE_NONE;
            }

            if (isWifi(capabilities)) {
                return CpNetEnum.TYPE_WIFI;
            }
        } else {
            NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();
            if (null == networkInfo) {
                return CpNetEnum.TYPE_NONE;
            }
            if (ConnectivityManager.TYPE_WIFI == networkInfo.getType()) {
                return CpNetEnum.TYPE_WIFI;
            }
        }
        return getMobileNetType(ctx);
    }

    /**
     * CnPeng:1/22/21 4:28 PM 获取移动数据类型
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private CpNetEnum getMobileNetType(Context ctx) throws SecurityException {
        TelephonyManager telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        if (null == telephonyManager) {
            return CpNetEnum.TYPE_NONE;
        }

        int netWorkType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            netWorkType = telephonyManager.getDataNetworkType();
        } else {
            netWorkType = telephonyManager.getNetworkType();
        }

        return convert2CusNetType(netWorkType);
    }

    /**
     * CnPeng:1/22/21 4:51 PM 将系统的网络 Type 转换成我们需要的标识
     *
     * GPRS : 2G(2.5) General Packet Radia Service 114kbps
     * EDGE : 2G(2.75G) Enhanced Data Rate for GSM Evolution 384kbps
     * CDMA : 2G 电信 Code Division Multiple Access 码分多址
     * 1xRTT : 2G CDMA2000 1xRTT (RTT - 无线电传输技术) 144kbps 2G的过渡,
     * IDEN : 2G Integrated Dispatch Enhanced Networks 集成数字增强型网络 （属于2G，来自维基百科）
     *
     * UMTS : 3G WCDMA 联通3G Universal Mobile Telecommunication System 完整的3G移动通信技术标准
     * EVDO_0 : 3G (EVDO 全程 CDMA2000 1xEV-DO) Evolution - Data Only (Data Optimized) 153.6kps - 2.4mbps 属于3G
     * EVDO_A : 3G 1.8mbps - 3.1mbps 属于3G过渡，3.5G
     * EVDO_B : 3G EV-DO Rev.B 14.7Mbps 下行 3.5G
     * HSPA : 3G (分HSDPA,HSUPA) High Speed Packet Access
     * HSPAP : 3G HSPAP 比 HSDPA 快些
     * HSDPA : 3.5G 高速下行分组接入 3.5G WCDMA High Speed Downlink Packet Access 14.4mbps
     * HSUPA : 3.5G High Speed Uplink Packet Access 高速上行链路分组接入 1.4 - 5.8 mbps
     * EHRPD : 3G CDMA2000向LTE 4G的中间产物 Evolved High Rate Packet Data HRPD的升级
     *
     * LTE : 4G Long Term Evolution FDD-LTE 和 TDD-LTE , 3G过渡，升级版 LTE Advanced 才是4G
     */
    private CpNetEnum convert2CusNetType(int netWorkType) {
        switch (netWorkType) {

            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return CpNetEnum.TYPE_2G;

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return CpNetEnum.TYPE_3G;

            case TelephonyManager.NETWORK_TYPE_LTE:
            case 19: // 19 对应的是 NETWORK_TYPE_LTE_CA，被标记为 hide 了，所以直接使用 19 判断
                return CpNetEnum.TYPE_4G;

            case TelephonyManager.NETWORK_TYPE_NR:
                return CpNetEnum.TYPE_5G;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                return CpNetEnum.TYPE_OTHER;
        }
    }

    /**
     * CnPeng:1/22/21 4:14 PM 判断是不是 Wifi
     */
    private boolean isWifi(NetworkCapabilities capabilities) {
        boolean hasWifiTrans = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
//        Log.d(TAG, "连接到 Wifi网络：" + hasWifiTrans + "| -------------------------");

        return hasWifiTrans;
    }

    /**
     * CnPeng:1/25/21 8:38 AM 是不是蜂窝网络（即移动数据网络）
     * 注意：这个不准确，打开 Wifi 开关，但未连接到任意网络时，此处会返回 true
     */
    private boolean isCellular(NetworkCapabilities capabilities) {
        boolean isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        Log.d(TAG, "连接到的是移动数据网络：" + isCellular);
        return isCellular;
    }

    /**
     * CnPeng:1/25/21 8:36 AM 是否可以进行网络访问
     */
    @TargetApi(23)
    private boolean isNetValidated(NetworkCapabilities capabilities) {
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * CnPeng:1/22/21 5:18 PM 是否已经连接到网络(连接上但不代表可以访问网络)
     */
    private boolean isNetConnected(NetworkCapabilities capabilities) {
        boolean hasCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
//        Log.d(TAG, "是否已经连接到网络：" + hasCapability);
        return hasCapability;
    }

    /**
     * CnPeng:1/22/21 4:15 PM 获取 IP 地址等信息
     */
    private void getLinkProperties(ConnectivityManager connectManager, Network curNetObj) {
        LinkProperties linkProperties = connectManager.getLinkProperties(curNetObj);
        List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
        if (null != linkAddresses && !linkAddresses.isEmpty()) {
            // 包含 IPV4 和 IPV6 两种地址
            for (LinkAddress linkAddress : linkAddresses) {
                String hostAddress = linkAddress.getAddress().getHostAddress();
                Log.d(TAG, "主机地址：" + hostAddress);
            }
        }
    }
}