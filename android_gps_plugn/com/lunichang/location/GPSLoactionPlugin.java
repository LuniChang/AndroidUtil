package com.lunichang.util.location;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

import android.location.GpsSatellite;
import android.location.GpsStatus;

import androidx.core.app.ActivityCompat;


public class GPSLoactionPlugin {


    private static GPSLoactionPlugin instance;//建议单例调用


    public static GPSLoactionPlugin getInstance(Context context) {
        if (instance == null) {
            instance = new GPSLoactionPlugin(context);
        }
        return instance;
    }

    public GPSLoactionPlugin(Context context) {
        this.context = context;
    }
//    public GPSLoactionPlugin(Context context, LocationCallBack locationCallBack) {
//        this.context = context;
//        this.locationCallBack = locationCallBack;
//    }
//
//    public LocationCallBack getLocationCallBack() {
//        return locationCallBack;
//    }
//
//    public void setLocationCallBack(LocationCallBack locationCallBack) {
//        this.locationCallBack = locationCallBack;
//    }

    private float minDistance = 0.5f;
    private static final String TAG = "GPSLoactionPlugin";
    private LocationManager locationManager = null;
    private Criteria locationOption = null;
    private GpsLocationListener locationListener = new GpsLocationListener();
    private WatchGpsStatusistener watchGpsStatusistener = new WatchGpsStatusistener();
    private GpsStatusistener gpsStatusistener = new GpsStatusistener();
    private ErrCallbackLocationListener onceErrCallbackLocationListener = new ErrCallbackLocationListener();
    private ErrCallbackLocationListener watchErrCallbackLocationListener = new ErrCallbackLocationListener();
    private TimeOutHandler timeOutHandler = new TimeOutHandler();

    private Context context;
    private boolean isOnceLocation = true;
    private long interval = 1000L;
    private long timeOutSet = 10000L;
    private boolean hadGetGps = false;
    private long curentGetGpsTime = 0;
    private volatile boolean isWatchTimeOut = false;
    private Thread locationThread;
    private long gpsSignInterval = 1000L;
    private int gpsEnableSign = 29;
    private int gpsSign = -1;

    private boolean isWork=false;

//    private LocationCallBack locationCallBack;

    private ArrayList<LocationCallBack> locationCallBacks = new ArrayList<>();


    public void removeLocationCallBack(LocationCallBack locationCallBack) {
        this.locationCallBacks.remove(locationCallBack);
    }

    public void addLocationCallBack(LocationCallBack locationCallBack) {
        if (this.locationCallBacks.indexOf(locationCallBack) == -1)
            this.locationCallBacks.add(locationCallBack);
    }

    protected class WatchTimeOutRunnable implements Runnable {
        @Override
        public void run() {
            while (isWatchTimeOut) {

                try {
                    Thread.sleep(timeOutSet);
                } catch (InterruptedException e) {
                    Log.e(TAG, "isWatchTimeOut error");
                }

                if (!hadGetGps && curentGetGpsTime + timeOutSet < System.currentTimeMillis()) {

                    Message timeOutMsg = new Message();
                    timeOutMsg.setTarget(timeOutHandler);
                    timeOutMsg.sendToTarget();
//                    runOnUi{
//                        whenTimeOut();
//                    }

                    if (isOnceLocation) {
                        isWatchTimeOut = false;
                    }

                }

            }
        }
    }

    /**
     * 事实定位
     *
     * @param interval    间隔时间  毫秒
     * @param minDistance 变化间隔 单位米
     * @param timeOutSet  超时时间 毫秒
     */
    public void getLocationAllTime(long interval, float minDistance, long timeOutSet) {
        if (checkPermission()) {
            return;
        }
        this.interval = interval;
        this.timeOutSet = timeOutSet;
        this.minDistance = minDistance;
        this.isWork=true;
        getLocationAllTime();
    }


    /**
     * 默认配置
     */
    public void getLocationAllTime() {
        if (checkPermission()) {
            return;
        }
        isOnceLocation = false;
        this.isWork=true;
        startLocation();
        startWatchTimeOut();
    }

    /**
     * 单次定位
     *
     * @param timeOutSet
     */
    public void getLocation(int timeOutSet) {
        if (checkPermission()) {
            return;
        }
        this.timeOutSet = timeOutSet;
        stopLocation();
        isOnceLocation = true;
        this.isWork=true;
        startLocation();
        startWatchTimeOut();
    }


    /**
     * gps sign enable
     *
     * @return if enable  than >=4    大于4有效卫星
     */
    public void watchGpsSign() {
        if (checkPermission()) {
            return;
        }
        try {
            this.isWork=true;
            if (locationManager == null)
                locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);


            locationManager.addGpsStatusListener(watchGpsStatusistener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, minDistance,
                    watchErrCallbackLocationListener);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            this.isWork=false;
        }

    }

    public void stopWathcGpsSign() {
        try {
            locationManager.removeGpsStatusListener(gpsStatusistener);
            locationManager.removeUpdates(watchErrCallbackLocationListener);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public void getGpsSign() {
        if (checkPermission()) {
            return;
        }
        try {
            this.isWork=true;
            if (locationManager == null)
                locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);


            locationManager.addGpsStatusListener(gpsStatusistener);


            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, minDistance,
                    onceErrCallbackLocationListener);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            this.isWork=false;
        }
    }


    private Intent serviceIntent = null;

    private void startWatchTimeOut() {
        if (this.isWatchTimeOut == true) {
            return;
        }
        this.isWatchTimeOut = true;

        this.curentGetGpsTime = System.currentTimeMillis();

        try {
            locationThread = new Thread(new WatchTimeOutRunnable());
            locationThread.start();
        } catch (Exception e) {
            Log.e(TAG, "startWatchTimeOut" + e.getMessage());
        }

    }

    public void stopLocation() {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        }
        if (locationListener != null)
            locationManager.removeUpdates(locationListener);

        isWatchTimeOut = false;
        locationThread = null;
        if (null != serviceIntent) {
            context.stopService(serviceIntent);

        }
        this.isWork=false;

    }

    protected void whenTimeOut() {
        Log.i(TAG, "gps timeout");
        LocationData data = new LocationData();
        data.is_timeout = 1;
        data.type = "timeout";

        for (int i = 0; i < locationCallBacks.size(); ++i) {
            locationCallBacks.get(i).onGetLocationTimeOut(data);
        }
//        locationCallBack.onGetLocationTimeOut(data);

        if (isOnceLocation) {
            stopLocation();
        }
    }

    protected void callbackLocation(android.location.Location location) {
        if (location != null) {
            hadGetGps = true;
            this.curentGetGpsTime = System.currentTimeMillis();
            // 获取位置信息
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

            LocationData data = new LocationData();
            // 速度
            data.speed = location.getSpeed();
            // 角度
            data.bearing = location.getBearing();
            // 星数
            data.satellites = location.getExtras().getInt("satellites", 0);
            // 时间
            data.time = location.getTime();
            data.altitude = location.getAltitude();
            data.is_timeout = 0;
            data.hasAccuracy = location.hasAccuracy();
            data.accuracy = location.getAccuracy();
            data.type = "location";
            data.latitude = latitude;
            data.longitude = longitude;


//            locationCallBack.onGetLocationSuccess(data);
            for (int i = 0; i < locationCallBacks.size(); ++i) {
                locationCallBacks.get(i).onGetLocationSuccess(data);
            }

            if (isOnceLocation) {
                stopLocation();
            }

        }
    }

    public class GpsLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(android.location.Location location) {
            callbackLocation(location);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {


//            locationCallBack.onGetLocationError(new LocationData(provider));
            for (int i = 0; i < locationCallBacks.size(); ++i) {
                locationCallBacks.get(i).onGetLocationError(new LocationData(provider));
            }
        }

    }

    private void startLocation() {
        if (checkPermission()) {
            return;
        }
        if (locationManager == null)
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationOption == null) {
            locationOption = new Criteria();
            locationOption.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
            locationOption.setAltitudeRequired(true);
            locationOption.setBearingRequired(true);
            locationOption.setSpeedRequired(true);
            locationOption.setCostAllowed(true);
            locationOption.setPowerRequirement(Criteria.POWER_LOW); // 功耗
        }

        try {


            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, minDistance, locationListener);
        } catch (Exception e) {

            Log.e(TAG, e.getMessage());
        }

        if (null == serviceIntent) {
            serviceIntent = new Intent();
            serviceIntent.setClass(context, LocationForegoundService.class);
        }
        context.startService(serviceIntent);
    }

    public class WatchGpsStatusistener implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(int event) {
            try {

                if (checkPermission()) {
                    return;
                }
                GpsStatus gpsStatus = locationManager.getGpsStatus(null);

                int maxSatellites = gpsStatus.getMaxSatellites();

                Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                int count = 0;
                int maxCount = 0;
                while (iters.hasNext() && count <= maxSatellites) {
                    GpsSatellite item = iters.next();

                    if (item.getSnr() > gpsEnableSign) {
                        ++count;
                    }
                    ++maxCount;
                }

                gpsSign = count;

                LocationData data = new LocationData();
                data.type = "gps_sign";
                if (maxSatellites != 0) {
                    data.gpsSign = gpsSign / maxSatellites;
                } else {
                    data.gpsSign = 0;
                }
                data.is_timeout = 0;


                data.maxSatellites = maxSatellites;
                data.foundSatellites = maxCount;
                data.enableSatellites = gpsSign;

//                locationCallBack.onGetLocationSuccess(data);
                for (int i = 0; i < locationCallBacks.size(); ++i) {
                    locationCallBacks.get(i).onGetLocationSuccess(data);
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
//                locationCallBack.onGetLocationError(new LocationData(e.getMessage()));
                for (int i = 0; i < locationCallBacks.size(); ++i) {
                    locationCallBacks.get(i).onGetLocationError(new LocationData(e.getMessage()));
                }

            }

        }
    }

    private boolean checkPermission() {
        boolean state = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (state) {
//            locationCallBack.hadNoLocationPermission();
            for (int i = 0; i < locationCallBacks.size(); ++i) {
                locationCallBacks.get(i).hadNoLocationPermission();
            }
        }

        return state;
    }

    public class GpsStatusistener implements GpsStatus.Listener {
        @Override
        public void onGpsStatusChanged(int event) {
            try {


                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                GpsStatus gpsStatus = locationManager.getGpsStatus(null);


                int maxSatellites = gpsStatus.getMaxSatellites();

                Iterator<GpsSatellite> iterator = gpsStatus.getSatellites().iterator();
                int count = 0;
                int maxCount = 0;
                while (iterator.hasNext() && count <= maxSatellites) {
                    GpsSatellite item = iterator.next();

                    if (item.getSnr() > gpsEnableSign) {
                        ++count;
                    }
                    ++maxCount;
                }

                gpsSign = count;


                LocationData data = new LocationData();
                data.type = "gps_sign";
                if (maxSatellites != 0) {
                    data.gpsSign = gpsSign / maxSatellites;
                } else {
                    data.gpsSign = 0;
                }
                data.is_timeout = 0;


                data.maxSatellites = maxSatellites;
                data.foundSatellites = maxCount;
                data.enableSatellites = gpsSign;


                locationManager.removeGpsStatusListener(this);
                locationManager.removeUpdates(onceErrCallbackLocationListener);

//                locationCallBack.onGetLocationSuccess(data);
                for (int i = 0; i < locationCallBacks.size(); ++i) {
                    locationCallBacks.get(i).onGetLocationSuccess(data);
                }
            } catch (Exception e) {

                Log.e(TAG, e.getMessage());
//                locationCallBack.onGetLocationError(new LocationData(e.getMessage()));
                for (int i = 0; i < locationCallBacks.size(); ++i) {
                    locationCallBacks.get(i).onGetLocationError(new LocationData(e.getMessage()));
                }
            }

        }
    }


    public class ErrCallbackLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(android.location.Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
//            locationCallBack.onGetLocationError(new LocationData(provider));
            for (int i = 0; i < locationCallBacks.size(); ++i) {
                locationCallBacks.get(i).onGetLocationError(new LocationData(provider));
            }
        }

    }


    class TimeOutHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            try {
                whenTimeOut();

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    public boolean isWork() {
        return isWork;
    }
}
