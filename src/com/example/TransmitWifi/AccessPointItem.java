package com.example.TransmitWifi;

import android.util.Log;

/**
 * @author haihui.li
 * @version 1.0.0
 */
public class AccessPointItem extends TransmitFileItem {
    private final static String TAG = "AccessPointItem ";
    /**
     * 获得www服务的根目录
     * @return the path of www server
     */
    public String getWWWRootPath() {
        return Utils.getFilePathDir(getPath());
    }

    /**
     * 获得wifi名称的后缀
     * @return wifi name
     */
    public String getWifiSuffix() {
        String shortName = Utils.genValidateFileName(getTitle().trim(), "");
        int limitCount = WifiManagerWrap.WIFI_AP_NAME_MAX_COUNT - WifiManagerWrap.IDENTIFY_PREFIX_LENGTH;
        if (shortName.length() < limitCount) {
            limitCount = shortName.length();
        }
        Log.i(TAG, "#########subString " + shortName.substring(0, limitCount));
        return shortName.substring(0, limitCount);
    }

    /**
     * 构造函数
     * @param title file title
     * @param detail file detail
     * @param path file path
     * @param size file size
     */
    public AccessPointItem(String title, String path, String detail, long size) {
        super(title, path, detail, size);
    }
}
