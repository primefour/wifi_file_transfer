package com.example.TransmitWifi;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.*;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Method;

/**
 * wifi 热点管理,wifi连接管理
 * @author haihui.li 
 * @version 1.0.0
 */
public class AccessPointManager extends WifiManagerWrap {
    private final static String TAG = "AccessPointManager";
    private static int mWifiApStateDisabled;
    private static int mWifiApStateDisabling;
    private static int mWifiApStateEnabled;
    private static int mWifiApStateEnabling;
    private static int mWifiApStateFailed;
    private static String mWifiApStateChangedAction;
    private static String mExtraWifiApState;
    private static String mExtraPreviousWifiApState;
    private Context mContext;
    private String mWifiApSSID;

    static {
        try {
            mWifiApStateDisabled = WifiManager.class.getField("WIFI_AP_STATE_DISABLED").getInt(WifiManager.class);
            mWifiApStateDisabling = WifiManager.class.getField("WIFI_AP_STATE_DISABLING").getInt(WifiManager.class);
            mWifiApStateEnabled = WifiManager.class.getField("WIFI_AP_STATE_ENABLED").getInt(WifiManager.class);
            mWifiApStateEnabling = WifiManager.class.getField("WIFI_AP_STATE_ENABLING").getInt(WifiManager.class);
            mWifiApStateFailed = WifiManager.class.getField("WIFI_AP_STATE_FAILED").getInt(WifiManager.class);

            mWifiApStateChangedAction = (String)WifiManager.class.getField("WIFI_AP_STATE_CHANGED_ACTION").get(WifiManager.class);
            mExtraWifiApState = (String)WifiManager.class.getField("EXTRA_WIFI_AP_STATE").get(WifiManager.class);
            mExtraPreviousWifiApState = (String)WifiManager.class.getField("EXTRA_PREVIOUS_WIFI_AP_STATE").get(WifiManager.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**wifi热点状态 已经失效*/
    public static final int WIFI_AP_STATE_DISABLED = mWifiApStateDisabled;
    /**wifi热点状态 正在失效*/
    public static final int WIFI_AP_STATE_DISABLING = mWifiApStateDisabling;
    /**wifi热点状态 有效状态*/
    public static final int WIFI_AP_STATE_ENABLED = mWifiApStateEnabled;
    /**wifi热点状态 正在生效*/
    public static final int WIFI_AP_STATE_ENABLING = mWifiApStateEnabling;
    /**wifi热点状态 失败状态*/
    public static final int WIFI_AP_STATE_FAILED = mWifiApStateFailed;
    /**wifi热点状态改变的消息*/
    public static final String WIFI_AP_STATE_CHANGED_ACTION = mWifiApStateChangedAction;
    /**wifi热点状态信息*/
    public static final String EXTRA_WIFI_AP_STATE = mExtraWifiApState;
    /**之前的wifi热点状态信息*/
    public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = mExtraPreviousWifiApState;

    private OnWifiApStateChangeListener mOnApStateChangeListener;

    private BroadcastReceiver mWifiApStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "Access point receiver get a " + action);
            if (WIFI_AP_STATE_CHANGED_ACTION.equals(action) && mOnApStateChangeListener != null) {
                mOnApStateChangeListener.onWifiStateChanged(
                                intent.getIntExtra(EXTRA_WIFI_AP_STATE, WIFI_AP_STATE_FAILED));
            }
        }
    };

    /**
     * 构造函数
     * @param context 上下文
     */
    public AccessPointManager(Context context) {
        super(context);
        if (!TextUtils.isEmpty(WIFI_AP_STATE_CHANGED_ACTION)) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
            context.registerReceiver(mWifiApStateReceiver, intentFilter);
        } else {
            Log.i(TAG, "##########failed to register receiver for checking the status of wifi");
        }
    }

    /**
     * 设置热点的监听函数
     * @param listener to monitor the state of access point
     */
    public void setWifiApStateChangeListener(OnWifiApStateChangeListener listener) {
        mOnApStateChangeListener = listener;
    }


    /**
     * 设置wifi热点是否有效
     * @param configuration WifiConfiguration
     * @param enabled 是否有效
     * @return 设置是否成功
     */
    private boolean setWifiApEnabled(WifiConfiguration configuration, boolean enabled) {
        try {
            if (enabled) {
                mWifiManager.setWifiEnabled(false);
            }

            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean)method.invoke(mWifiManager, configuration, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取Wifi热点的状态
     * @return Wifi热点状态
     */
    public int getWifiApState() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApState");
            return (Integer)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mWifiApStateFailed;
    }

    /**
     * 获取Wifi热点配置
     * @return WifiConfiguration
     */
    private WifiConfiguration getWifiApConfiguration() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration config = (WifiConfiguration)method.invoke(mWifiManager);
            loadWifiConfigurationFromProfile(config);
            return config;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean setWifiApConfiguration(WifiConfiguration configuration) {
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            return (Boolean)method.invoke(mWifiManager, configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * wifi热点是否有效
     * @return wifi热点是否有效
     */
    public boolean isWifiApEnabled() {
        try {
            Method method = mWifiManager.getClass().getMethod("isWifiApEnabled");
            return (Boolean)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 通过歌名作为后缀来创建Wifi热点名
     * @param suffix is the suffix of access point name
     */
    public void createWifiApSSID(String suffix) {
        mWifiApSSID = DEFAULT_SSID + suffix;
    }

    /**
     * 创建wifi热点
     * @return 打开wifi热点是否成功
     */
    public boolean startWifiAp() {
        //先关闭wifi
        closeWifi();
        //关闭已经打开的热点
        if (isWifiApEnabled()) {
            setWifiApEnabled(getWifiApConfiguration(), false);
        }
        //激活需要创建的热点
        acquireWifiLock();
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID =  getWifiApSSID();
        Log.i(TAG, "wifi ap configure ssid = " + wifiConfig.SSID);
        wifiConfig.preSharedKey = DEFAULT_PASSWORD;
        setWifiConfigAsWPA(wifiConfig);
        return setWifiApEnabled(wifiConfig, true);
    }


    /**
     * 停止wifi热点，并恢复之前的网络状态
     */
    public void stopWifiAp() {
        releaseWifiLock();
        //关闭wifi热点
        if (isWifiApEnabled()) {
            setWifiApEnabled(getWifiApConfiguration(), false);
        }
        //恢复wifi
        openWifi();
    }

    /**
     * 停止wifi热点，解绑定监听器
     * @param context 上下文
     */
    public void destroy(Context context) {
        stopWifiAp();
        removeNetwork(SSID_PREFIX);
        context.unregisterReceiver(mWifiApStateReceiver);
    }

    /**
     * get access point ssid
     * @return ssid string
     */
    public String getWifiApSSID() {
        return mWifiApSSID;
    }

    /**
     * wifi AP网络状态监听器
     */
    public static interface OnWifiApStateChangeListener {
        /**
         * access point status listener
         * @param state wifi state now
         */
       public void onWifiStateChanged(int state);
    };
}
