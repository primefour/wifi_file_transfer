package com.example.TransmitWifi;
import android.os.Handler;

import java.io.*;
import java.net.Socket;

/**
 *下载的抽象类
 * @author haihui.li
 * @version 1.0.0
 */
public abstract class SimpleDownload {
    private static final int READ_BUFFER_SIZE = 4096;
    private final String mINetAddress;
    private final String mSavePath;
    private final int mPort;
    private final Handler mHandler;

    /**
     * the file path is file on the server
     */
    protected String mUri;

    /**
     *file offset will be downloaded
     */
    protected int mDataOffset = 0;
    /**
     *to avoid divide zero exception ,length will be downloaded
     */
    protected int mDataSize = 0;
    /**
     * initialize zero, have downloaded length
     */
    private int mDownloadedLength = 0;


    private boolean mIsDownloadOK = true;;
    private Socket mClientSocket;
    private Thread mDownloadThread;
    private OnDownloadResultListener mDownloadResultListener;
    /**
     * constructor of download
     * @param handler main thread handler
     * @param ia internet address
     * @param port port to connect
     * @param savePath path to save download file
     */
    public SimpleDownload(Handler handler, String ia, int port, String savePath) {
        this.mINetAddress = ia;
        this.mPort = port;
        this.mSavePath = savePath;
        this.mHandler = handler;
    }

    /**
     *设置下载完成后的监听函数
     * @param listener callback function when download complete or interrupted
     */
    public void setDownloadResultListener(OnDownloadResultListener listener) {
        mDownloadResultListener = listener;
    }

    /**
     * 获得下载状态
     * @return true为OK否则失败
     */
    public boolean isDownloadOK() {
        return mIsDownloadOK;
    }

    /**
     * 返回下载进度
     * @return percent of download
     */
    public int getProgress() {
        return (mDataSize != 0) ? Utils.PERCENT_FECTOR * mDownloadedLength / mDataSize : 0;
    }

    protected void setDownloadFailed() {
        mIsDownloadOK = false;
    }

    /**
     * 解析协议头并生成下载的数据流
     * @param sockInputStream  socket input stream
     */
    abstract protected InputStream parseHeadAndGetDataInputStream(InputStream sockInputStream) throws IOException;

    /**
     * 发送下载请求
     * @param sockOutputStream socket output stream
     */
    abstract protected void sendRequest(OutputStream sockOutputStream) throws IOException;


    private void triggerResultCallback() {
        if (mDownloadResultListener != null) {
            if (mIsDownloadOK) {
                mDownloadResultListener.onDownloadComplete();
            } else {
                mDownloadResultListener.onDownloadFailed();
            }
        }
    }

   private void saveDownloadData(InputStream dataInputStream) throws IOException {
       byte[] buff = new byte[READ_BUFFER_SIZE];
       File saveFile = new File(mSavePath);
       RandomAccessFile randomFile = new RandomAccessFile(saveFile, "rw");
       randomFile.seek(mDataOffset);
       int remainSize = mDataSize;
       while (mDataSize - mDownloadedLength > 0) {
           int nRead = dataInputStream.read(buff);
           if (nRead > 0) {
               mDownloadedLength  += nRead;
               randomFile.write(buff, 0, nRead);
           } else {
               break;
           }
       }
       randomFile.close();
   }

    /**
     * 开始下载
     */
    public void startDownload() {
        mDownloadThread = new Thread() {
            @Override
            public void run() {
                OutputStream sockOutputStream = null;
                InputStream sockInputStream = null;
                try {
                    mClientSocket = new Socket(mINetAddress, mPort);
                    mClientSocket.setSoTimeout(NetWorkUtils.SO_TIME_OUT);
                    mClientSocket.setSendBufferSize(NetWorkUtils.SEND_BUFFER_SIZE);
                    mClientSocket.setReceiveBufferSize(NetWorkUtils.RECEIVE_BUFFER_SIZE);
                    sockOutputStream = mClientSocket.getOutputStream();
                    sockInputStream = mClientSocket.getInputStream();
                    sendRequest(sockOutputStream);
                    InputStream dataInputStream = parseHeadAndGetDataInputStream(sockInputStream);
                    saveDownloadData(dataInputStream);
                } catch (IOException ie) {
                    NetWorkUtils.safeClose(sockInputStream);
                    NetWorkUtils.safeClose(sockOutputStream);
                    NetWorkUtils.safeClose(mClientSocket);
                    mIsDownloadOK = false;
                    ie.printStackTrace();
                } finally {
                    NetWorkUtils.safeClose(sockInputStream);
                    NetWorkUtils.safeClose(sockOutputStream);
                    NetWorkUtils.safeClose(mClientSocket);
                    mHandler.post(new Runnable() {
                        public void run() {
                            triggerResultCallback();
                        }
                    });
                }
            }
        };
        mDownloadThread.start();
    }
    /**
     * 停止下载
     */
    public void stopDownload() {
        try {
            NetWorkUtils.safeClose(mClientSocket);
            if (mDownloadThread != null) {
                mDownloadThread.interrupt();
                mDownloadThread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载结果监听接口
     */
    public interface OnDownloadResultListener {
        /**
         * download fail call back
         */
        public void onDownloadFailed();
        /**
         * download completely call back
         */
        public void onDownloadComplete();
    }
}

