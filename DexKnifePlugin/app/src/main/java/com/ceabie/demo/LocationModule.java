package com.ceabie.demo;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class LocationModule implements LocationListener {

    private static final String TAG = "LocationUtil";
    private static final long INTERVAL = 1 * 60 * 1000; // 5min
    private static final long TIMEOUT = 30 * 1000; // timeout for get location
    private static final long TIMEOUT_RM_UPDATES = 70 * 1000;

    private LocationManager mLM;
    private String strProvider;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mIsNeedCallback = true;
    private long mGotTime = 0;
    private long mReqTime = 0;
    private Runnable mRunnableRemove;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (null != mLM) {
            mLM.removeUpdates(this);
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnableUpdate);
            mHandler.removeCallbacks(mRunnableRemove);
        }
    }

    public String getProvider(Context context) {
        if (context == null) {
            return null;
        }

        getLocationManager(context);
        if (null == mLM) {
            return null;
        }

        String strProvider = null;

        if (!mLM.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && !mLM.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return null;
        }

        if (mLM.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            strProvider = LocationManager.NETWORK_PROVIDER;

        } else if (mLM.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            strProvider = LocationManager.GPS_PROVIDER;
        }

        return strProvider;
    }

    public Location getLastKnownLocation(Context context) {
        getLocationManager(context);

        Location loc = null;
        if (null != mLM) {
            if (null == loc) {
                loc = mLM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (null == loc) {
                loc = mLM.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (null == loc) {
                loc = mLM.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
        }

        return loc;
    }

    private void getLocationManager(Context context) {
        if (mLM == null && context != null) {
            mLM = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public Location getLocation(Context context, OnLocationListener onLocationListener) {
        if (null == context) {
            return null;
        }

        getLocationManager(context);

        strProvider = getProvider(context);

        if (null == mLM || TextUtils.isEmpty(strProvider)) {
            return getLastKnownLocation(context);
        }

        Location location = null;
        mIsNeedCallback = true;

        mReqTime = System.currentTimeMillis();

        // interval is too short, use last known location
        if (mReqTime <= mGotTime + INTERVAL) {
            location = getLastKnownLocation(context);
            if (null != location) {
                mIsNeedCallback = false;
            }
        }

        if (mIsNeedCallback) {
            registerListener(onLocationListener);
        }

        requestLocationFromSystem();

        return location;
    }

    private LocationListener mGpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void requestLocationFromSystem() {
        Log.d("gps", "requestLocationFromSystem");
        try {

            mLM.requestSingleUpdate(LocationManager.GPS_PROVIDER, mGpsListener, null);
            mLM.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
            mLM.requestSingleUpdate(LocationManager.PASSIVE_PROVIDER, this, null);

            mHandler.removeCallbacks(mRunnableUpdate);
            mHandler.postDelayed(mRunnableUpdate, TIMEOUT);
        } catch (NoSuchMethodError e) {
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("gps", "onLocationChanged");
        if (location != null) {
            Log.d("gps", "location: " + location.getLatitude() + ", " + location.getLongitude());
            Log.d("gps", "location: " + location.getProvider() + " - " + location.getAccuracy());
            mGotTime = System.currentTimeMillis();
        }

        mHandler.removeCallbacks(mRunnableUpdate);
        mHandler.removeCallbacks(mRunnableRemove);

        mLM.removeUpdates(this);

        callbackLocation(location);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private Runnable mRunnableUpdate = new Runnable() {
        @Override
        public void run() {
            if (null != mHandler) {
                mHandler.removeCallbacks(this);
            }

            callbackLocation(null);

            // handle timeout for mLM.removeUpdates
            startRemoveUpdatesTimer();
        }
    };

    private void startRemoveUpdatesTimer() {
        if (mRunnableRemove == null) {
            mRunnableRemove = new Runnable() {
                @Override
                public void run() {
                    mLM.removeUpdates(LocationModule.this);
                    mHandler.removeCallbacks(this);
                }
            };
        } else {
            mHandler.removeCallbacks(mRunnableRemove);
        }

        mHandler.postDelayed(mRunnableRemove, TIMEOUT_RM_UPDATES);
    }

    public void callbackLocation(Location location) {
//		Log.d("gps", "callbackLocation");

        Location lastLocation = getLastKnownLocation(null);

        if (mIsNeedCallback && mCallbacks.size() > 0) {
            mIsNeedCallback = false;
            if (null == location) {
                location = lastLocation;
            }
            notifyChanged(location);
        }
    }

    public void notifyChanged(final Location location) {
//		Log.d("gps", "notifyChanged");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator<OnLocationListener> iterator = mCallbacks.iterator();
                while (iterator.hasNext()) {
                    OnLocationListener callback = iterator.next();
                    if (callback != null && !callback.onLocationChanged(location)) {
                        iterator.remove();
                    }
                }
            }
        });

    }

    // call back ///////////////////////////////
    private List<OnLocationListener> mCallbacks = new Vector<OnLocationListener>();


    public void registerListener(OnLocationListener listener) {
        if (listener != null && !mCallbacks.contains(listener)) {
            mCallbacks.add(listener);
        }
    }

    public void unregisterListener(OnLocationListener listener) {
        mCallbacks.remove(listener);
    }

    public interface OnLocationListener {
        /**
         * On location changed.
         *
         * @param location the location
         * @return the boolean 返回false则不再监听
         * @author ceabie
         */
        boolean onLocationChanged(Location location);
    }

}
