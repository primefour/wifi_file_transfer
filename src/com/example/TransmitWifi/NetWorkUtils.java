package com.example.TransmitWifi;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import android.util.Log;
import org.apache.http.conn.util.InetAddressUtils;


/**
 * 网络方面的功能函数及常量
 * @author haihui.li
 * @version 1.0.0
 */
public class NetWorkUtils {

    private static final String TAG = "NetWorkUtils";
    /**
     * listen port
     */
    public static final int HTTP_LISTERN_PORT = 37899;
    /**
     * download save path
     */
    public static final String DOWNLOAD_SAVE_PATH = "/sdcard";

    /**
     * socket time out
     */
    public static final int SO_TIME_OUT = 1000 * 60 * 60;
    /**
     * log
     */
    public static final int DEFAULT_BACKLOG = 100;
    /**
     * receive buffer size
     */
    public static final int RECEIVE_BUFFER_SIZE = 1024 * 32;
    /**
     * send buffer size
     */
    public static final int SEND_BUFFER_SIZE = 1024 * 32;

    /**
     * 获得本地ip地址xxx.xxx.xxx.xxx
     * @return internet address as ip version 4
     */
    public static String getLocalIpAddress() {
        try {
            String ipv4;
            List<NetworkInterface>  nilist = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface ni: nilist) {
                Log.i(TAG, "NetworkInterface####### " + ni);
                List<InetAddress>  ialist = Collections.list(ni.getInetAddresses());
                for (InetAddress address: ialist) {
                    Log.i(TAG, "######InetAddress  = " + address);
                    ipv4 = address.getHostAddress();
                    if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4)) {
                        return ipv4;
                    }
                }

            }

        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
            return null;
        }
        return null;
    }

    /**
     * 获得本地ip地址 inetaddress
     * @return internet address or null
     */
    public static InetAddress getLocalInetAddress() {  
        try {  
            String ipv4;  
            List<NetworkInterface>  nilist = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface ni: nilist) {
                Log.i(TAG, "NetworkInterface####### " + ni);
                List<InetAddress>  ialist = Collections.list(ni.getInetAddresses());
                for (InetAddress address: ialist) {
                    Log.i(TAG, "######InetAddress  = " + address);
                    ipv4 = address.getHostAddress();
                    if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4)) {
                        return address;  
                    }  
                }  

            }  

        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }  
        return null;  
    }

    /**
     * 关闭server socket
     * @param serverSocket server socket
     */
    public static final void safeClose(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *关闭客户端socket 
     * @param socket socket will be closed
     */
    public static final void safeClose(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * close closeable interface
     * @param closeable closeable interface
     */
    public static final void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * filter space and some special character
     * @param str will to filter
     * @return filtered string
     */
    public static String encodeURIWithPercent(String str) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
            ignored.printStackTrace();
        }
        return encoded;
    }
}

