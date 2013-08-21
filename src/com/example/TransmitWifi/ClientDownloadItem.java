package com.example.TransmitWifi;
import android.net.wifi.ScanResult;
/**
 * for ssid item that will be download or downloading
 * @author haihui.li
 * @version 1.0.0
 */
public class ClientDownloadItem extends TransmitFileItem {
    private static final String TAG = "ClientDownloadItem";
    /**
     * 初始状态
     */
    public static final int SSID_DOWNLOAD_NONE_STATE = 0;
    /**
     * 开始下载时wifi连接状态
     */
    public static final int WIFI_CONNECTING_STATE = 1;
    /**
     *正在下载时状态
     */
    public static final int DOWNLOAD_STATE_DOWNING  = 2;
    /**
     *下载成功时状态
     */
    public static final int DOWNLOAD_STATE_COMPLETE = 3;


    private volatile int mState;
    private int mProgress;
    private String mSSID;
    private String mBSSID;

    ClientDownloadItem(String ssid, String bssid) {
        super(ssid, null, null, 0);
        mSSID = ssid;
        mBSSID = bssid;
        mState = SSID_DOWNLOAD_NONE_STATE;
    }

    /**
     * 获取ssid用来wifi连接
     * @return ssid
     */
    public String getSSID() {
        return mSSID;
    }

    /**
     * 获取Bssid用来wifi连接
     * @return bssid
     */
    public String getBSSID() {
        return mBSSID;
    }
    /**
     *设置当前下载的进度
     * @param percent percent of download
     */
    public void setDownloadProgress(int percent) {
        mProgress = percent;
    }

    /**
     * 获得当前下载的进度
     * @return the value of download percent
     */
    public int getDownloadProgress() {
        return mProgress;
    }

    /**
     * 设置当前的状态
     * @param state  the state will change to
     */
    public void setState(int state) {
        mState = state;
    }

    /**
     * 获得当前的状态
     * @return the ui attached to the item
     */
    public int getState() {
        return mState;
    }
}
