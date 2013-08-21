package com.example.TransmitWifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.DhcpInfo;
import android.net.wifi.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 用来下载文件的Activity
 * @author haihui.li
 * @version 1.0.0
 */
public class WifiClientActivity extends Activity implements WifiConnectManager.OnWifiScanFinishListener
                                                            , WifiConnectManager.OnWifiConnectListener
                                                            , MediaFileDownload.OnMediaFileDownloadResultListener {
    private static final String TAG = "WifiClientActivity";
    private static final int UPDATE_UI_INTERVAL_MS = 200; //200MS
    private static final int CLIENT_UPDATE_UI_TIMER = 0;

    private static final int ON_DOWNLOAD_COMPLETE = 1;
    private static final int ON_DOWNLOAD_FAILED = 2;

    private ListView mListView;
    private Button mScanButton;
    private TextView mEmptyText;
    private ProgressBar mScanProgressBar;
    private ProgressDialog mProgressDialog;
    private ClientWifiListAdapter mWifiListAdapter;

    private ClientDownloadItem mCurrentDownloadItem;
    private MediaFileDownload mDownloadEngine;
    private WifiConnectManager mConnectManager;

    private boolean isAnyDownloadWorking() {
        return mCurrentDownloadItem != null;
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what) {
                case CLIENT_UPDATE_UI_TIMER:
                    if (isAnyDownloadWorking()) {
                        Log.i(TAG, "mDownloadEngine.getProgress() = " + mDownloadEngine.getProgress());
                        mCurrentDownloadItem.setDownloadProgress(mDownloadEngine.getProgress());
                        mWifiListAdapter.notifyDataSetChanged();
                        if (ClientDownloadItem.DOWNLOAD_STATE_DOWNING == mCurrentDownloadItem.getState()) {
                            dismissProgressDialog();
                        }
                        updateDownloadItem();
                        runUpdateUITimer();
                    } else {
                        stopUpdateUITimer();
                    }
                return true;
                default:
                return false;
            }
        }
    }
    );

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

    private void runUpdateUITimer() {
        mHandler.sendEmptyMessageDelayed(CLIENT_UPDATE_UI_TIMER, UPDATE_UI_INTERVAL_MS);
    }

    private void startUpdateUITimer() {
        mHandler.sendEmptyMessageDelayed(CLIENT_UPDATE_UI_TIMER, UPDATE_UI_INTERVAL_MS);
    }

    private void stopUpdateUITimer() {
        if (mHandler.hasMessages(CLIENT_UPDATE_UI_TIMER)) {
            mHandler.removeMessages(CLIENT_UPDATE_UI_TIMER);
        }
    }

    private void updateDownloadItem() {
        Map<String, String> param = mDownloadEngine.getMetaParameter();
        mCurrentDownloadItem.initialize(param);
    }

    @Override
    public void onScanFinished(List<ScanResult> scanResults) {
        mScanProgressBar.setVisibility(View.INVISIBLE);
        List<ClientDownloadItem> list = new ArrayList<ClientDownloadItem>();

        if (isAnyDownloadWorking()) {
            list.add(mCurrentDownloadItem);
        }

        for (ScanResult item : scanResults) {
            if (mCurrentDownloadItem != null && item.SSID.equals(mCurrentDownloadItem.getSSID())) {
                continue;
            }
            ClientDownloadItem downloadItem = new ClientDownloadItem(item.SSID, item.BSSID);
            list.add(downloadItem);
        }

        if (mWifiListAdapter != null) {
            mWifiListAdapter.init(list);
        } else {
            mWifiListAdapter = new ClientWifiListAdapter(WifiClientActivity.this, list);
            mListView.setAdapter(mWifiListAdapter);
        }
    }

    @Override
    public void onWifiConnected() {
        if (isAnyDownloadWorking()) {
            mCurrentDownloadItem.setState(ClientDownloadItem.DOWNLOAD_STATE_DOWNING);
            mDownloadEngine.startDownloadMediaFile(getServerIpAddress(), mHandler);
        }
    }

    @Override
    public void onMediaDownloadFailed() {
        mCurrentDownloadItem.setState(ClientDownloadItem.SSID_DOWNLOAD_NONE_STATE);
        handleDownloadFailed(mCurrentDownloadItem);
        mCurrentDownloadItem = null;
    }

    @Override
    public void onMediaDownloadCompleted() {
        mCurrentDownloadItem.setState(ClientDownloadItem.DOWNLOAD_STATE_COMPLETE);
        handleDownloadComplete(mCurrentDownloadItem);
        mCurrentDownloadItem = null;
    }

    private void handleDownloadFailed(ClientDownloadItem item) {
        Toast.makeText(this, R.string.client_download_error_msg, Toast.LENGTH_SHORT).show();
        mWifiListAdapter.notifyDataSetChanged();
    }

    private void handleDownloadComplete(ClientDownloadItem item) {
        Toast.makeText(this, R.string.client_download_complete_msg, Toast.LENGTH_SHORT).show();
        mWifiListAdapter.notifyDataSetChanged();
    }

    private void handleDownloadCancel(ClientDownloadItem item) {
        Toast.makeText(this, R.string.client_download_cancel_msg, Toast.LENGTH_SHORT).show();
        mWifiListAdapter.notifyDataSetChanged();
        startScanAccessPoint();
    }

    private void startScanAccessPoint() {
        mScanProgressBar.setVisibility(View.VISIBLE);
        mConnectManager.startScan();
    }

    private String getServerIpAddress() {
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        Log.i(TAG, "dhcpInfo " + dhcpInfo);
        Log.i(TAG, "getLocalIpAddress = " + NetWorkUtils.getLocalIpAddress());
        return Utils.intToIpString(dhcpInfo.gateway);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_client);

        mListView = (ListView)findViewById(R.id.client_ap_list_view);
        mScanButton = (Button)findViewById(R.id.client_scan_button);
        mEmptyText = (TextView)findViewById(R.id.client_ap_list_empty_view);
        mScanProgressBar = (ProgressBar)findViewById(R.id.progressBarAction);
        mScanButton.setOnClickListener(mOnScanButtonClickListener);
        mListView.setEmptyView(mEmptyText);
        mConnectManager = new WifiConnectManager(this);
        mConnectManager.setScanFinishListener(this);
        mConnectManager.setOnNetworkConnect(this);
        mDownloadEngine = new MediaFileDownload();
        mDownloadEngine.setMediaFileDownloadListener(this);
        startScanAccessPoint();
    }

    private View.OnClickListener mOnScanButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startScanAccessPoint();
        }
    };


    private void cancelCurrentTask() {
        mDownloadEngine.stopDownloadMediaFile();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCurrentTask();
        mConnectManager.destroy(this);
    }

    private void startConnectToAccessPoint() {

        Thread connectThread = new Thread() {
            @Override
            public void run() {
                mConnectManager.openWifi();
                //connect to the ap
                if (!mConnectManager.connectToAccessPoint(mCurrentDownloadItem.getSSID(),
                        mCurrentDownloadItem.getBSSID())) {
                    Log.i(TAG, "startConnectToAccessPoint################");
                }
            }
        };
        connectThread.start();
    }


    private void handleClickedEventNewATask(ClientDownloadItem item) {
        startUpdateUITimer();
        showProgressDialog(getResources().getString(R.string.client_dialog_title_wait),
                    getResources().getString(R.string.client_dialog_title_wait_msg));
        mCurrentDownloadItem = item;
        mCurrentDownloadItem.setState(ClientDownloadItem.WIFI_CONNECTING_STATE);
        startConnectToAccessPoint();
    }


    private void handlerClickedEventGetOtherTask(ClientDownloadItem item) {
        Log.i(TAG, "mCurrentTransmitTask != null will give a dialog#####################");
        /**give a dialog to confirm user's selection*/
        new AlertDialog.Builder(WifiClientActivity.this)
            .setTitle(R.string.client_dialog_title_cancel)
            .setMessage(R.string.client_dialog_content_cancel)
            .setPositiveButton(R.string.client_dialog_button_ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //cancel current download task
                            cancelCurrentTask();
                        }
                    }
                    )
            .setNegativeButton(R.string.client_dialog_button_no, null)
            .create()
            .show();
    }

    private View.OnClickListener mOnDownloadClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ClientDownloadItem item = (ClientDownloadItem)v.getTag();

            if (isAnyDownloadWorking()) {
                //current task is running we
                //should warn user whether or not
                //to new a task
                handlerClickedEventGetOtherTask(item);
            } else {
                handleClickedEventNewATask(item);
            }
        }
    };


    /**
     * Adapter for client
     */
    public class ClientWifiListAdapter extends BaseAdapter {
        private static final String TAG = "ClientWifiListAdapter";
        private List<ClientDownloadItem> mItemlist = new ArrayList<ClientDownloadItem>();
        private Context mContext;

        ClientWifiListAdapter(Context context, List<ClientDownloadItem> list) {
            mContext = context;
            init(list);
        }

        /**
         * results the list when scan finished
         * @param list the result of scan
         */
        public void init(List<ClientDownloadItem> list) {
            if (list != null) {
                mItemlist.clear();
                mItemlist.addAll(list);
                Log.i(TAG, "mItemlist.size = " + mItemlist.size());
                notifyDataSetChanged();
            }
        }
        @Override
        public int getCount() {
            return mItemlist.size();
        }

        @Override
        public ClientDownloadItem getItem(int position) {
            return mItemlist.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //init UI
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.wifi_client_item, null);
                ViewHolder holder = new ViewHolder((ProgressBar)convertView.findViewById(R.id.client_translate_progress)
                    , (ImageView)convertView.findViewById(R.id.client_image_state)
                    , (Button)convertView.findViewById(R.id.client_button_download)
                    , (TextView)convertView.findViewById(R.id.client_text_title)
                    , (TextView)convertView.findViewById(R.id.client_text_detail));
                convertView.setTag(holder);
            }
            //bind view
            bindView(position, convertView);
            return convertView;
        }
        private void bindView(int position, View view) {
            ViewHolder holder = (ViewHolder)view.getTag();
            ClientDownloadItem item = mItemlist.get(position);
            updateListView(item, holder);
        }

        /**
         * 更新listView
         * @param item download item
         * @param uiHolder ui holder instance
         */
        public void updateListView(ClientDownloadItem item, ViewHolder uiHolder) {
            String buttonTitle = mContext.getResources().getString(R.string.client_button_download);
            uiHolder.getDownloadButton().setTag(item);
            uiHolder.getDownloadButton().setOnClickListener(mOnDownloadClickListener);
            uiHolder.getDetailTextView().setText(item.getDetail());
            uiHolder.getTitleTextview().setText((item.getTitle() == null) ? item.getSSID() : item.getTitle());
            uiHolder.getDownloadButton().setText(buttonTitle);
            uiHolder.getProgressBar().setMax(Utils.PERCENT_FECTOR);
            uiHolder.getProgressBar().setProgress(item.getDownloadProgress());
        }


        /**
         * UI holder for the client item
         */
        private class ViewHolder {
            private Button      mDownloadButton;
            private ProgressBar mProgressBar;
            private ImageView   mDownloadStatusImage;
            private TextView    mTitle;
            private TextView    mDetail;

            /**
             * get download button
             * @return download button
             */
            public Button getDownloadButton() {
                return mDownloadButton;
            }

            /**
             * get progress bar
             * @return progress bar
             */
            public ProgressBar getProgressBar() {
                return mProgressBar;
            }

            /**
             * get title text view
             * @return title text view
             */
            public TextView getTitleTextview() {
                return mTitle;
            }

            /**
             * get detail text view
             * @return detail text view
             */
            public TextView getDetailTextView() {
                return mDetail;
            }

            /**
             * constructor for view holder
             * @param progress download progress
             * @param stateView download state
             * @param downloadButton download button
             * @param title download file name
             * @param detail download details
             */
            public ViewHolder(ProgressBar progress, ImageView stateView,
                            Button downloadButton, TextView title, TextView detail) {
                mProgressBar         = progress;
                mDownloadStatusImage = stateView;
                mDownloadButton      = downloadButton;
                mTitle               = title;
                mDetail              = detail;
            }
        }
    }
}
