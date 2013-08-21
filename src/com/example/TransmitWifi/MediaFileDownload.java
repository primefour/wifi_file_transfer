package com.example.TransmitWifi;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 下载多媒体文件控制类
 * @author haihui.li
 * @version 1.0.0
 */

public class MediaFileDownload {
    private static final String TAG = "MediaFileDownload";
    private SimpleDownload mMetaDownload;
    private SimpleDownload mMediaDownload;
    private Map<String, String> mMetaEntry = new HashMap<String, String>();
    private OnMediaFileDownloadResultListener mDownloadResultListener;
    private String mIpAddress;
    private Handler mHandler;

    /**
     * 设置下载完状态监听接口
     * @param listener listener of download complete or error
     */
    public void setMediaFileDownloadListener(OnMediaFileDownloadResultListener listener) {
        mDownloadResultListener = listener;
    }

    private String getMetaFilePathOnServer() {
        return "/" + TransmitFileItem.META_FILE_NAME;
    }

    private String getMetaFileSavePath() {
        return NetWorkUtils.DOWNLOAD_SAVE_PATH + "/" + TransmitFileItem.META_FILE_NAME;
    }

    private String getMediaFilePathOnServer() {
        String path = mMetaEntry.get(TransmitFileItem.META_DATA_PATH);
        return "/" + Utils.getFileName(path);
    }

    private String getMediaFileSavePath() {
        String path = mMetaEntry.get(TransmitFileItem.META_DATA_PATH);
        return NetWorkUtils.DOWNLOAD_SAVE_PATH + "/" + Utils.getFileName(path);
    }

    private void parseMetaFile() {
        TransmitFileItem.parseMetaString(NetWorkUtils.DOWNLOAD_SAVE_PATH, mMetaEntry);
    }


    private void downloadMetaFile() {
        mMetaDownload = new HttpDownload(getMetaFilePathOnServer(),
                                                getMetaFileSavePath(),
                                                0, mIpAddress,
                                                NetWorkUtils.HTTP_LISTERN_PORT,
                                                mHandler);
        mMetaDownload.setDownloadResultListener(mMetaDownloadListener);
        mMetaDownload.startDownload();

    }

    private void downloadMediaFile() {
        mMediaDownload = new HttpDownload(getMediaFilePathOnServer(),
                                                getMediaFileSavePath(),
                                                0, mIpAddress,
                                                NetWorkUtils.HTTP_LISTERN_PORT,
                                                mHandler);
        mMediaDownload.setDownloadResultListener(mMediaDownloadListener);
        mMediaDownload.startDownload();
    }

    /**
     * 开始文件下载
     * @param ipAddress the internet address of server Will connect to
     * @param handler main thread handler
     */
    public void startDownloadMediaFile(String ipAddress, Handler handler) {
        mIpAddress = ipAddress;
        mHandler = handler;
        downloadMetaFile();
    }

    /**
     * 停止文件下载
     */
    public void stopDownloadMediaFile() {
        if (mMetaDownload != null) {
            mMetaDownload.stopDownload();
        }

        if (mMediaDownload != null) {
            mMediaDownload.stopDownload();
        }
    }

    /**
     * 获得下载的进度
     * @return percent of download
     */
    public int getProgress() {
        return mMediaDownload != null ? mMediaDownload.getProgress() : 0;
    }

    /**
     * 获得下载文件的描述
     * @return a map of meta parameter
     */
    public Map<String, String> getMetaParameter() {
        return mMetaEntry;
    }

    private SimpleDownload.OnDownloadResultListener mMetaDownloadListener = new SimpleDownload.OnDownloadResultListener() {
        @Override
        public void onDownloadFailed() {
            Log.i(TAG, " ####onDownloadFailed### ");
            if (mDownloadResultListener != null) {
                mDownloadResultListener.onMediaDownloadFailed();
            }
        }
        @Override
        public void onDownloadComplete() {
            parseMetaFile();
            Log.i(TAG, "  ###onDownloadComplete### ");
            downloadMediaFile();
        }

    };

    private SimpleDownload.OnDownloadResultListener mMediaDownloadListener = new SimpleDownload.OnDownloadResultListener() {
        @Override
        public void onDownloadFailed() {
            if (mDownloadResultListener != null) {
                mDownloadResultListener.onMediaDownloadFailed();
            }
        }
        @Override
        public void onDownloadComplete() {
            if (mDownloadResultListener != null) {
                mDownloadResultListener.onMediaDownloadCompleted();
            }
        }

    };

    /**
     * 多媒体文件下载完监听函数
     */
    public interface OnMediaFileDownloadResultListener {
        /**
         * download fail call back
         */
        public void onMediaDownloadFailed();
        /**
         * download completely call back
         */
        public void onMediaDownloadCompleted();
    }

}
