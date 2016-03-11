package com.thin.downloadmanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by bcoien on 3/16/16.
 */
public class NetworkHelper {
    private static final String TAG = NetworkHelper.class.getSimpleName();
    private final boolean DEBUG = false;
    private static final String WIFI_LOCK_NAME = "ThinDownloadManager.NetworkHelper";
    private static NetworkHelper sInstance = null;

    private final int sApiLevel = Build.VERSION.SDK_INT;

    private final Context mContext;
    private final ConnectivityManager mConManager;
    private final WifiManager mWifiManager;
    public static WifiManager.WifiLock sWifiLock = null;

    public static NetworkHelper getInstance(Context context) {
        if (sInstance == null) sInstance = new NetworkHelper(context.getApplicationContext());
        return sInstance;
    }

    /**
     * Get instance if it has already been constructed
     * @return May be null
     */
    public static NetworkHelper getInstance() {
        return sInstance;
    }

    private NetworkHelper(Context context) {
        if (DEBUG) Log.d(TAG, "NetworkHelper.constructor");
        mContext = context;
        mConManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        sWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_NAME);
        sWifiLock.setReferenceCounted(false);
        if(sWifiLock.isHeld()) sWifiLock.release();
    }

    /**
     * Gets the active network type
     * @return Of ConnectivityManager.TYPE_[typeName]. Returns -1 for no active network.
     */
    private int getActiveNetworkType() {
        NetworkInfo networkInfo = mConManager.getActiveNetworkInfo();
        int networkType;
        if (networkInfo != null && networkInfo.isConnected()) {
            networkType = networkInfo.getType();
        } else {
            networkType = -1; // no active network connection or not connected
        }

        if (DEBUG) Log.d(TAG, "getActiveNetworkType() - return networkType="+networkType+"; for reference WiFi="+ConnectivityManager.TYPE_WIFI);
        return networkType;
    }

    @TargetApi(21) // hush the compiler, we have taken care of it
    private HttpURLConnection getUrlConnectionOnNetwork(URL url, int networkType) throws IOException  {
        if (DEBUG) Log.d(TAG,"getUrlConnectionOnNetwork() - enter: url="+url.getPath());
        HttpURLConnection urlConnection = null;

        if (sApiLevel >= 21) {
            for(Network network : mConManager.getAllNetworks()) {
                if(mConManager.getNetworkInfo(network).getType() == networkType) {
                    urlConnection = (HttpURLConnection) network.openConnection(url);
                    if (DEBUG) Log.d(TAG, "getUrlConnectionOnNetwork() - found network with matching networkType="+networkType);
                    break;
                }
            }
        } else {
            int activeNetworkType = getActiveNetworkType();
            if (activeNetworkType == networkType) {
                urlConnection = (HttpURLConnection) url.openConnection();
                if (DEBUG) Log.d(TAG, "getUrlConnectionOnNetwork() - active network matched networkType="+networkType);
            } else {
                if (DEBUG) Log.d(TAG, "getUrlConnectionOnNetwork() - activeNetworkType="+activeNetworkType+" did NOT match desired networkType="+networkType);
            }
        }

        String connectionInfoString = urlConnection!=null ? urlConnection.getURL().getPath() : "null";
        if (DEBUG) Log.d(TAG,"getUrlConnectionOnNetwork() - exit: urlFromConnection="+connectionInfoString);
        return urlConnection;
    }

    public HttpURLConnection getUrlConnectionOnWifiNetwork(URL url) throws IOException {
        return getUrlConnectionOnNetwork(url, ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Not required for api>21 because we use getUrlConnectionOnWifiNetwork() to get the UrlConnection directly from the WiFi network
     */
    private void setPreferredNetworkType(int networkType) {
        if (sApiLevel < 21) {
            mConManager.setNetworkPreference(networkType);
        }
    }

    /**
     * On api <21 sets preferred network type to WiFi.
     */
    public void setPreferWifi() {
        if (DEBUG) Log.d(TAG, "setPreferWifi()");
        setPreferredNetworkType(ConnectivityManager.TYPE_WIFI);

        if(!sWifiLock.isHeld()) sWifiLock.acquire();
    }


    public boolean isActiveNetworkWifi() {
        boolean activeNetworkIsWifi = getActiveNetworkType() == ConnectivityManager.TYPE_WIFI;
        if (DEBUG) Log.d(TAG, "isActiveNetworkWifi() - return="+activeNetworkIsWifi);
        return activeNetworkIsWifi;
    }

    public boolean isWifiAvailable() {
        boolean wifiAvailable = false;

        int wifiState = mWifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            wifiAvailable = true;
        }

        if (DEBUG) Log.d(TAG, "isWifiAvailable() - return="+wifiAvailable);
        return wifiAvailable;
    }

    /**
     * On api <21 sets preferred network type.
     */
    public void clearNetworkPreference() {
        if (DEBUG) Log.d(TAG, "clearNetworkPreference()");
        if (sApiLevel < 21) {
            mConManager.setNetworkPreference(ConnectivityManager.DEFAULT_NETWORK_PREFERENCE);
        }

        if(sWifiLock.isHeld()) sWifiLock.release();
    }

    /**
     * Get App Context for the download
     * @return may return null
     */
    public Context getAppContext() {
        return mContext;
    }
}
