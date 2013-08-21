package com.example.TransmitWifi;

import android.util.Log;

import java.io.*;
import java.util.Map;

/**
 * 传输文件项
 * @author haihui.li
 * @version 1.0.0
 */

public class TransmitFileItem {
    private static final String TAG = "TransmitFileItem";
    /**
     * meta html 文件路径参数
     */
    public static final String META_DATA_PATH = "data";
    /**
     * meta html 文件名称参数
     */
    public static final String META_DATA_TITLE = "title";
    /**
     * meta html 文件大小参数
     */
    public static final String META_DATA_SIZE = "size";
    /**
     * meta html 文件描述参数
     */
    public static final String META_DATA_DETAIL = "mimetype";

    /**
     * meta html 默认名称
     */
    public static final String META_FILE_NAME = "metaFile.html";


    private  String mTitle;
    private  String mDetail;
    private  String mPath;
    private  long mSize;

    TransmitFileItem(String title, String path, String detail, long size) {
        mSize = size;
        mPath = path;
        mDetail = detail;
        mTitle = title;
    }
    TransmitFileItem() {
    }

    /**
     *获得文件名称 
     * @return file title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     *获得文件描述 
     * @return file detail
     */
    public String getDetail() {
        return (mDetail != null) ? mDetail : " ";
    }

    /**
     *获得文件大小 
     * @return the size of file
     */
    public long getSize() {
        return mSize;
    }

    /**
     *获得文件存放路径 
     * @return file path
     */
    public String getPath() {
        return mPath;
    }

    /**
     * 初始化成员 
     * @param metaMap meta value map
     */
    public void initialize(Map<String, String> metaMap) {
        mTitle = metaMap.get(META_DATA_TITLE);
        mPath = metaMap.get(META_DATA_PATH);
        mDetail = metaMap.get(META_DATA_DETAIL);
        if (metaMap.get(META_DATA_SIZE) != null) {
            mSize = Integer.parseInt(metaMap.get(META_DATA_SIZE));
        }
    }

    /**
     * 创建下载所用的metafile 
     * @param wwwRootDir the html file placed
     */
    public void createMetaFile(String wwwRootDir) {
        StringBuilder strBuild = new StringBuilder();
        strBuild.append(META_DATA_PATH);
        strBuild.append(" :");
        strBuild.append(mPath);
        strBuild.append("\r\n");
        strBuild.append(META_DATA_TITLE);
        strBuild.append(" :");
        strBuild.append(mTitle);
        strBuild.append("\r\n");
        strBuild.append(META_DATA_SIZE);
        strBuild.append(" :");
        strBuild.append(mSize);
        strBuild.append("\r\n");
        strBuild.append(META_DATA_DETAIL);
        strBuild.append(" :");
        strBuild.append(mDetail);
        strBuild.append("\r\n\r\n");
        Log.i(TAG, "create meta file = " + strBuild);

        try {
            File metaFile = new File(wwwRootDir + "/" + META_FILE_NAME);
            if (metaFile.exists()) {
                metaFile.delete();
            }
            metaFile.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(metaFile);

            byte[] buf = strBuild.toString().getBytes();
            Log.i(TAG, "###########buf length" + buf.length);
            fileOutputStream.write(buf, 0, buf.length);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException ex) {
            Log.i(TAG, "file not found ");
        } catch (IOException ie) {
            Log.i(TAG, "write meta file failed");
        }
    }

    /**
     *解析metafile文件
     *@param metaDir the directory of html placed
     *@param param map of parameters to store the parse result
     */
    public static void parseMetaString(String metaDir, Map<String, String> param) {
        final int bufferSize = 4096;
        byte[] buf = new byte[bufferSize];
        try {
            File metaFile = new File(metaDir + "/" + META_FILE_NAME);
            FileInputStream fileInputStream = new FileInputStream(metaFile);
            int size = fileInputStream.read(buf, 0, bufferSize);
            Log.i(TAG, "buf length = " + buf.length + "    size=" + size);

            BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, size)));
            String line = in.readLine();
            while (line != null && line.trim().length() > 0) {
                int p = line.indexOf(':');
                if (p >= 0) {
                    param.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
                }
                line = in.readLine();
            }
        } catch (IOException ie) {
            Log.i(TAG, "parse io exception ");
        }
    }
}
