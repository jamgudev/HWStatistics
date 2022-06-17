package com.jamgu.hwstatistics.network;

public enum CpNetEnum {
    //CnPeng:1/22/21 4:26 PM  无网络
    TYPE_NONE(0),
    //CnPeng:1/22/21 4:26 PM  其他网络类型
    TYPE_OTHER(6),
    TYPE_WIFI(1),
    TYPE_2G(2),
    TYPE_3G(3),
    TYPE_4G(4),
    TYPE_5G(5);

    private final int value;

    CpNetEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}