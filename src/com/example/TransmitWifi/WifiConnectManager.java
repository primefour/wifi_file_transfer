package com.example.TransmitWifi;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.*;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理客户端wifi连接的类
 * @author haihui.li
 * @version 1.0.0
 */
public class WifiConnectManager extends WifiManagerWrap {

    private static final String TAG = "WifiConnectManager";
    private static final int DEFAULT_PRIORITY = 10000;

    private OnWifiScanFinishListener mWifiScanFinishListener;
    private OnWifiConnectListener mWifiConnectListener;

    /**
     *构造函数
     * @param context run context
     */
    public WifiConnectManager(Context context) {
        super(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
        context.registerReceiver(mWifiReceiver, intentFilter);
    }

    /**
     * 设置WifiScan完的监听函数
     * @param listener callback  interface
     */
    public void setScanFinishListener(OnWifiScanFinishListener listener) {
        mWifiScanFinishListener = listener;
    }

    /**
     * 设置网络状态监听接口
     * @param listener callback interface
     */
    public void setOnNetworkConnect(OnWifiConnectListener listener) {
        mWifiConnectListener = listener;
    }

    private void handleNetworkStateChanged(NetworkInfo networkInfo) {
        if (networkInfo.isConnected() && mWifiConnectListener != null) {
            //network connected complete
            mWifiConnectListener.onWifiConnected();
        }
    }


    private void handleScanResultsFinish() {
        List<ScanResult> list = mWifiManager.getScanResults();
        if (list != null) {
            for (ScanResult item : list) {
                Log.i(TAG, "##############scan item = " + item);
            }
            List<ScanResult> filterList = scanFinishedEvent(list);
            if (mWifiScanFinishListener != null) {
                mWifiScanFinishListener.onScanFinished(filterList);
            }
        }
    }

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        /**
         * interface of BroadcastReceiver
         */
         @Override
         public void onReceive(Context context, Intent intent) {
             if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                 Log.i(TAG, "WIFI_STATE_CHANGED_ACTION");
             } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                 /* handle wifi connect success */
                 handleNetworkStateChanged((NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
             } else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                 /* handle scan finish */
                 handleScanResultsFinish();
             } else if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                 /*handle supplicant connection change later*/
                 Log.i(TAG, "SUPPLICANT_CONNECTION_CHANGE_ACTION");
             } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                 Log.i(TAG, "SUPPLICANT_STATE_CHANGED_ACTION");
             } else if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
                 /*handle later*/
                 Log.i(TAG, "RSSI_CHANGED_ACTION");
             } else if (intent.getAction().equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)) {
                 /* handle network id change info later */
                 Log.i(TAG, "NETWORK_IDS_CHANGED_ACTION");
             } else {
                 Log.e(TAG, "Received an unknown Wifi Intent");
             }
         }
    };

    /**
     * 停止wifi热点，解绑定监听器
     * @param context 上下文
     */
    public void destroy(Context context) {
        removeNetwork(SSID_PREFIX);
        context.unregisterReceiver(mWifiReceiver);
        releaseWifiLock();
    }

    /**
     * Check Whether or not connect
     * @return network of wifi connected return true or false
     */
    public boolean isWifiConnect() {
        final ConnectivityManager connectivity = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Log.i(TAG, "wifi is connect =============" + info.isConnected());
        return info.isConnected();
    }


    private List<ScanResult> scanFinishedEvent(List<ScanResult> scanResults) {
        List<ScanResult> filter = filterAccessPoint(scanResults);
        return filter;
    }

    private List<ScanResult> filterAccessPoint(List<ScanResult> results) {
        if (results != null) {
            List<ScanResult> filteredResult = new ArrayList<ScanResult>();
            for (ScanResult result : results) {
                if (result.SSID.contains(SSID_PREFIX)) {
                    filteredResult.add(result);
                }
            }

            return filteredResult;
        }

        return null;
    }

    /**
     *连接热点
     * @param ssid the ssid will to connect
     * @param bssid the bssid will be take part in
     * @return the result of operation
     */
    public boolean connectToAccessPoint(String ssid, String bssid) {
        boolean ret = false;
        removeNetwork(SSID_PREFIX);

        Log.i(TAG, "connectToHotspot SSID =" + ssid);
        //如果当前网络不是需要连接的网络，则断开
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();

        mWifiManager.disableNetwork(connectionInfo.getNetworkId());
        mWifiManager.disconnect();

        //将所有其他网络的的优先级降低
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (!config.SSID.equals("\"" + ssid + "\"")) {
                config.priority = 0;
                mWifiManager.updateNetwork(config);
            }
        }

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + ssid + "\"";
        wifiConfig.preSharedKey = "\"" + DEFAULT_PASSWORD + "\"";
        wifiConfig.BSSID = bssid;
        wifiConfig.priority = DEFAULT_PRIORITY;

        setWifiConfigAsWPA(wifiConfig);

        try {
            Field field = wifiConfig.getClass().getField("ipAssignment");
            field.set(wifiConfig, Enum.valueOf((Class<Enum>)field.getType(), "DHCP"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        int networkId = mWifiManager.addNetwork(wifiConfig);

        ret = mWifiManager.enableNetwork(networkId, true);
        return ret;
    }

    /**
     * 开始扫描wifi热点
     */
    public void startScan() {
        //打开wifi
        openWifi();
        acquireWifiLock();
        mWifiManager.startScan();
    }
    /**
     * 获取指定网络id的wifi配置
     * @param ssid 网络id
     * @return WifiConfiguration
     */
    private WifiConfiguration getWifiConfiguration(String ssid) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            loadWifiConfigurationFromProfile(config);
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }

        return null;
    }

    /**
     * 清除已连接的网络的配置信息
     */
    private void removeConnection() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        mWifiManager.removeNetwork(wifiInfo.getNetworkId());
        mWifiManager.saveConfiguration();
    }

    /**
     * 获取连接信息
     * @return WifiInfo
     */
    private WifiInfo getConnectionInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * wifi scan end listener
     */
    public static interface OnWifiScanFinishListener {
        /**
         * 扫描结束的回调
         * @param scanResults List
         */
        public void onScanFinished(List<ScanResult> scanResults);
    }

    /**
     * 网络连接状态监听接口
     */
    public interface OnWifiConnectListener {
        /**
         * network status of wifi
         */
        public void onWifiConnected();
    }

}

