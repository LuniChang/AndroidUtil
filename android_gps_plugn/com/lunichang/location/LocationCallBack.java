package com.lunichang.util.location;

public interface LocationCallBack {
    public void onGetLocationSuccess(LocationData data);
    public void onGetLocationError(LocationData data);
    public void onGetLocationTimeOut(LocationData data);
    public void hadNoLocationPermission();
}
