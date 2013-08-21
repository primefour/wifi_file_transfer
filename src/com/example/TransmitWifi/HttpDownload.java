package com.example.TransmitWifi;

import android.os.Handler;
import android.util.Log;

import java.io.*;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * 用http协议实现Download抽象类 
 * @author haihui.li
 * @version 1.0.0
 */
public class HttpDownload extends SimpleDownload {
    private static final String TAG = "HttpDownload ";
    private static final int BUFSIZE = 8192;
    private Map mEntryParams = new HashMap<String, String>();

    /**
     * 构造函数
     * @param downloadPath download file path
     * @param savePath path to save download file
     * @param offset offset of download file
     * @param inetAddress internet address
     * @param port server listen port
     * @param handler main thread handler
     */
    public HttpDownload(String downloadPath, String savePath, int offset, String inetAddress, int port, Handler handler) {
        super(handler, inetAddress, port, savePath);
        mUri = downloadPath;
        mDataOffset = offset;
    }

    private String constructRequestString() {
        String reqStr = "GET " + NetWorkUtils.encodeURIWithPercent(mUri) + " HTTP/1.1" + "\r\n";
        reqStr += "Accept-Encoding: gzip\r\n";
        reqStr += "Accept-Language: zh-CN, en-US\r\n";
        reqStr += "User-Agent: Android\r\n";
        reqStr += "Accept: application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n";
        reqStr += "Accept-Charset: utf-8, iso-8859-1, utf-16, *;q=0.7\r\n";
        reqStr += "\r\n";
        Log.i(TAG, "constructRequestString = " + reqStr);
        return reqStr;
    }

    protected  void sendRequest(OutputStream sockOutputStream) throws IOException {
        byte[] sendByte = constructRequestString().getBytes();
        sockOutputStream.write(sendByte);
        sockOutputStream.flush();
    }

    /**
     * 获得http协议的相关参数
     * @return parameter map
     */
    public Map getEntryParams() {
        return mEntryParams;
    }

    /**
     * 获得Content的大小
     * @return size should be downloaded
     */
    public int getDataSize() {
        return mDataSize;
    }

    /**
     * 解析http header结果存在params map 里面
     */
    private boolean decodeHeader(BufferedReader in, Map<String, String> parms) {
        try {
            // Read the response line
            String inLine = in.readLine();
            if (inLine == null) {
                return false;
            }
            if (inLine.indexOf("200") < 0) {
                return false;
            }
            String line = in.readLine();
            while (line != null && line.trim().length() > 0) {
                Log.i(TAG, "get request string " + line);
                int p = line.indexOf(':');
                if (p >= 0) {
                    parms.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
                }
                line = in.readLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Log.i(TAG, "parse response exception");
        }
        return true;
    }
    private int findHeaderEnd(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 3 < rlen) {
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                return splitbyte + 4;
            }
            splitbyte++;
        }
        return 0;
    }


    private void getContentLength(Map params) {
        String key = "content-length";
        String contentlength = (String) params.get(key);
        if (contentlength == null) {
            mDataSize = 0;
        } else {
            mDataSize = Integer.parseInt(contentlength);
        }
        Log.i(TAG, "mDataSize  = " + mDataSize);
    }


    protected InputStream parseHeadAndGetDataInputStream(InputStream sockInputStream) throws IOException {

        byte[] buf = new byte[BUFSIZE];
        int splitbyte = 0;
        int rlen = 0;
        int read = sockInputStream.read(buf, 0, BUFSIZE);
        InputStream dataInputStream = null;
        if (read == -1) {
            // socket was been closed
            throw new SocketException();
        }

        while (read > 0) {
            rlen += read;
            splitbyte = findHeaderEnd(buf, rlen);
            if (splitbyte > 0) {
                break;
            }
            read = sockInputStream.read(buf, rlen, BUFSIZE - rlen);
        }

        if (splitbyte < rlen) {
            ByteArrayInputStream splitInputStream = new ByteArrayInputStream(buf, splitbyte, rlen - splitbyte);
            SequenceInputStream sequenceInputStream = new SequenceInputStream(splitInputStream, sockInputStream);
            dataInputStream = sequenceInputStream;
        } else {
            dataInputStream = sockInputStream;
        }

        BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));
        decodeHeader(hin, mEntryParams);
        Log.i(TAG, "mEntryParams = " + mEntryParams);
        getContentLength(mEntryParams);

        return dataInputStream;
    }
}
