package com.lunichang.util.location;

import com.google.gson.annotations.Expose;

public class LocationData {

    @Expose()
    public String type;
    @Expose()
    public double latitude;
    @Expose()
    public double longitude;
    @Expose()
    public boolean hasAccuracy;//准确
    @Expose()
    public float accuracy;
    @Expose()
    public int is_timeout;

    @Expose()
    public float speed;    // 速度
    @Expose()
    public float bearing; // 角度
    @Expose()
    public int satellites;//卫星数
    @Expose()
    public long time;
    @Expose()
    public double altitude;//海拔

    @Expose()
    public String errMsg;

    @Expose()
    public int gpsSign;//Gps信号
    @Expose()
    public int maxSatellites;
    @Expose()
    public int enableSatellites;//可用卫星
    @Expose()
    public int foundSatellites;//找到的卫星信号数量

    public LocationData(String errMsg) {
        this.errMsg = errMsg;
    }

    public LocationData() {
    }


    public boolean equalsGps(LocationData data) {
        if (data != null)
            return data.latitude == this.latitude && data.longitude == this.longitude;

        return false;
    }
}
