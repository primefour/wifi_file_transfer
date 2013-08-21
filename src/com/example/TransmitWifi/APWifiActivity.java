package com.example.TransmitWifi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;

/**
 * 用来建立Wifi热点的Activiy
 * @author haihui.li
 * @version 1.0.0
 */
public class APWifiActivity extends Activity implements AccessPointManager.OnWifiApStateChangeListener {
    private static final String TAG = "APWifiActivity";

    private static final int START_HTTP_SERVER = 0;
    private static final int STOP_HTTP_SERVER = 1;
    private static final int TEST_START_HTTP_SERVER = 2;
    private static final int TEST_STOP_HTTP_SERVER = 3;
    private static final int START_SERVER_DELAY_TIME_MS = 2000;

    private TextView mAccessPointInfoTextView;
    private ListView mAccessPointListView;
    private AccessPointManager mWifiApManager;
    private AccessPointListAdapter mAccessPointAdapter;
    private ProgressDialog mProgressDialog;
    private AccessPointItem mSelectedItem;
    private SimpleWebServer mHttpServer;

    private void stopHTTPServer() {
        if (mHttpServer != null) {
            mHttpServer.stop();
            mHttpServer = null;
        }
    }

    /**
     * 启动 http server
     * @param rootPath www server root directory
     * @param port listen port
     */
    private void startHTTPServer(String rootPath, int port) {
        // Defaults
        stopHTTPServer();
        File wwwRoot = new File(rootPath).getAbsoluteFile();
        mHttpServer  = new SimpleWebServer(NetWorkUtils.getLocalInetAddress(), port, wwwRoot);
        try {
            mHttpServer.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void showProgressDialog(String title, String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void closeAccessPoint() {
        if (mWifiApManager.isWifiApEnabled()) {
            showProgressDialog("Waiting!", "closing an access point Now");
            mWifiApManager.stopWifiAp();
        }
        stopHTTPServer();
    }

    private boolean isAnyAccessPointWorking() {
        return mSelectedItem != null;
    }

    private boolean isSharingItem(AccessPointItem item) {
        return item == mSelectedItem;
    }

    private void createAccessPoint(AccessPointItem item) {
        mSelectedItem = item;
        showProgressDialog("Waiting", "Creating an access point Now");
        mWifiApManager.createWifiApSSID(item.getWifiSuffix());
        if (mWifiApManager.startWifiAp()) {
            mAccessPointInfoTextView.setText("creating access point:" + mWifiApManager.getWifiApSSID());
        } else {
            dismissProgressDialog();
            mAccessPointInfoTextView.setText("create access point failed");
        }
    }

    private View.OnClickListener mCreateButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AccessPointItem selectedItem = (AccessPointItem)v.getTag();
            if (!isAnyAccessPointWorking()) {
                createAccessPoint(selectedItem);
            } else {
                closeAccessPoint();
            }
        }
    };

    private void updateAccessPointList() {
        if (isAnyAccessPointWorking()) {
            mAccessPointAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 实现监听wifi状态的接口
     * @param wifiApState the state of ap now
     */
    public void onWifiStateChanged(int wifiApState) {
        if (wifiApState == AccessPointManager.WIFI_AP_STATE_ENABLED) {
            onBuildWifiApSuccess();
        } else if (AccessPointManager.WIFI_AP_STATE_FAILED == wifiApState) {
            onBuildWifiApFailed();
        } else if (AccessPointManager.WIFI_AP_STATE_DISABLED == wifiApState) {
            onWifiClosed();
        } else if (AccessPointManager.WIFI_AP_STATE_ENABLING == wifiApState) {
            onBuildWifiApStarted();
        }
    }

    private void onBuildWifiApStarted() {
        Log.i(TAG, "onBuildWifiApStarted");
    }

    private void onBuildWifiApSuccess() {
        updateAccessPointList();
        dismissProgressDialog();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAnyAccessPointWorking()) {
                    String wwwDir = mSelectedItem.getWWWRootPath();
                    mSelectedItem.createMetaFile(wwwDir);
                    startHTTPServer(wwwDir, NetWorkUtils.HTTP_LISTERN_PORT);
                }
            }
        }, START_SERVER_DELAY_TIME_MS);
    }

    private void onBuildWifiApFailed() {
        updateAccessPointList();
        dismissProgressDialog();
        mSelectedItem = null;
    }

    private void onWifiClosed() {
        updateAccessPointList();
        dismissProgressDialog();
        mSelectedItem = null;
    }

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.access_point_list);

        mAccessPointListView = (ListView)findViewById(R.id.access_point_list_view);
        mAccessPointInfoTextView = (TextView)findViewById(R.id.access_point_status);

        mWifiApManager = new AccessPointManager(this);
        mWifiApManager.setWifiApStateChangeListener(this);

        mWifiApManager.stopWifiAp();
        mAccessPointInfoTextView.setText(getResources().getString(R.string.access_point_init_status));

        mAccessPointAdapter = new AccessPointListAdapter(this, getAccessPointList());
        mAccessPointListView.setAdapter(mAccessPointAdapter);
    }

    private List<AccessPointItem> getAccessPointList() {
        Cursor cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaItem.COLUMNS, null, null, null);
        if (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
            List<AccessPointItem> accessPointList = new ArrayList<AccessPointItem>();
            while (cursor.moveToNext()) {
                MediaItem tmpItem = new MediaItem(cursor);
                String sizeString = Formatter.formatFileSize(this, tmpItem.getSize());
                accessPointList.add(new AccessPointItem(tmpItem.getTitle(),
                            tmpItem.getPath(), tmpItem.getMimeType() + " " + sizeString,
                            tmpItem.getSize()));
            }
            return accessPointList;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeAccessPoint();
        mWifiApManager.destroy(this);
    }

    /**
     * 文件列表的adapter
     */
    public class AccessPointListAdapter extends BaseAdapter {
        private static final String TAG = "AccessPointListAdapter";
        private List<AccessPointItem> mAccessPointList = new ArrayList<AccessPointItem>();
        private Context mContext;

        private void reloadlistData(List<AccessPointItem> list) {
            mAccessPointList.clear();
            if (list != null) {
                mAccessPointList.addAll(list);
            }
        }

        /**
         * 构造函数
         * @param context context of running
         * @param list the list of MediaItem
         */
        public AccessPointListAdapter(Context context, List<AccessPointItem> list) {
            mContext = context;
            reloadlistData(list);
        }

        @Override
        public int getCount() {
            return (mAccessPointList != null) ? mAccessPointList.size() : null;
        }

        @Override
        public AccessPointItem getItem(int position) {
            return (mAccessPointList != null) ? mAccessPointList.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.access_point_item, null);

                ViewHolder holder = new ViewHolder((Button)convertView.findViewById(R.id.create_button)
                    , (TextView)convertView.findViewById(R.id.song_name_text_view)
                    , (TextView)convertView.findViewById(R.id.song_info_text_view));
                convertView.setTag(holder);
            }
            bindView(position, convertView);
            return convertView;
        }

        private void updateListItem(AccessPointItem item, ViewHolder holder) {
            String buttonHint = getString(R.string.access_point_enable_button_text);
            if (isAnyAccessPointWorking() && isSharingItem(item)) {
                int wifiApState = mWifiApManager.getWifiApState();
                if (wifiApState == AccessPointManager.WIFI_AP_STATE_ENABLED) {
                    buttonHint = mContext.getResources().getString(R.string.access_point_enabled);
                } else if (AccessPointManager.WIFI_AP_STATE_FAILED == wifiApState) {
                    buttonHint = mContext.getResources().getString(R.string.access_point_failed);
                } else if (AccessPointManager.WIFI_AP_STATE_ENABLING == wifiApState) {
                    buttonHint = mContext.getResources().getString(R.string.access_point_enabling);
                }
            }

            holder.mCreateButton.setText(buttonHint);
            holder.mCreateButton.setTag(item);
            holder.mCreateButton.setOnClickListener(mCreateButtonClickListener);
            //initialize text view
            holder.mTitle.setText(item.getTitle());
            holder.mDetail.setText(item.getDetail());
            holder.mCreateButton.setVisibility(View.VISIBLE);
        }


        private void bindView(int position, View view) {
            ViewHolder holder = (ViewHolder)view.getTag();
            AccessPointItem item = mAccessPointList.get(position);
            updateListItem(item, holder);
        }

        private class ViewHolder {
            private Button mCreateButton;
            private TextView mTitle;
            private TextView mDetail;

            public ViewHolder(Button createButton, TextView title, TextView detail) {
                mCreateButton = createButton;
                mTitle = title;
                mDetail = detail;
            }
        }
    }
}
