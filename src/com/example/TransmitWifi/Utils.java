package com.example.TransmitWifi;

import java.io.File;

/**
 * 网络工具
 * @author simon.liu
 * @version 1.0.0
 */

public class Utils {
    private static final String TAG = "Utils";

    private static final int MASK_0XFF = 0xFF;
    private static final int SHIFT_8 = 8;
    private static final int SHIFT_16 = 16;
    private static final int SHIFT_24 = 24;
    private static final int KILO_BYTE = 1024;
    private static final int MEGA_BYTE = KILO_BYTE * KILO_BYTE;
    /**
     * percent factor
     */
    public static final int PERCENT_FECTOR = 100;

    /**
     * change internet address to string
     * @param i internet address
     * @return xxx.xxx.xxx.xxx.
     */
    public static String intToIpString(int i) {
        return (i & MASK_0XFF)
                + "." + ((i >> SHIFT_8) & MASK_0XFF)
                + "." + ((i >> SHIFT_16) & MASK_0XFF)
                + "." + ((i >> SHIFT_24) & MASK_0XFF);
    }

    /**
     * generate a unique file path
     * @param path template of path
     * @return template path + _number.suffix
     */
    public static String genUniqueFilePath(String path) {
        String dir = getFilePathDir(path);
        String shortName = getFileShortName(path);
        String suffix = "." + getFileExtension(path);

        String frontPortion = dir.endsWith(File.separator) ? dir + shortName : dir + File.separator + shortName;

        String uniqueFilePath = frontPortion + suffix;
        File file = new File(uniqueFilePath);

        int number = 0;
        while (file.exists()) {
            uniqueFilePath = frontPortion + "_" + (++number) + suffix;
            file = new File(uniqueFilePath);
        }
        return uniqueFilePath;
    }

    /**
     * get directory of path
     * @param path of file
     * @return directory of file place in
     */
    public static String getFilePathDir(String path) {
        int separatorIndex = -1;

        if (path != null && path.startsWith(File.separator)) {
            separatorIndex = path.lastIndexOf(File.separatorChar);
        }

        return (separatorIndex == -1) ? File.separator : path.substring(0, separatorIndex);
    }

    /**
     * get file name of path
     * @param path name of file
     * @return file name exclude path
     */
    public static String getFileName(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }

        int query = path.lastIndexOf('?');
        if (query > 0) {
            path = path.substring(0, query);
        }

        int filenamePos = path.lastIndexOf(File.separatorChar);
        return (filenamePos >= 0) ? path.substring(filenamePos + 1) : path;
    }

    /**
     * get file name exclude suffix
     * @param path file path
     * @return short name of file
     */

    public static String getFileShortName(String path) {
        String fileName = getFileName(path);
        int separatorIndex = fileName.lastIndexOf('.');
        return (separatorIndex == -1) ? fileName : fileName.substring(0, separatorIndex);
    }

    /**
     * get file suffix
     * @param path of file
     * @return suffix of file
     */
    public static String getFileExtension(String path) {
        String fileName = getFileName(path);

        if (fileName.length() > 0) {
            int dotPos = fileName.lastIndexOf('.');
            if (0 <= dotPos) {
                return fileName.substring(dotPos + 1);
            }
        }

        return "";
    }

    /**
     * filter the special character
     * @param fileName the path of file
     * @param replacement the character to replace the shoot
     * @return the filtered name
     */
    public static String genValidateFileName(String fileName, String replacement) {
        // {} \ / : * ? " < > |
        return fileName == null ? null : fileName.replaceAll("([{/\\\\:*?\"<>|}\\u0000-\\u001f\\uD7B0-\\uFFFF]+)", replacement);
    }

    /**
     * parse \r\n\r\n
     * @param buf parse string
     * @param offset the offset of parse from begin
     * @return the pattern location
     */
    public static int locationAfterCRLFCRLF(byte[] buf, int offset) {
        int pos = offset;
        boolean found = false;
        while (pos < (buf.length - 4)) {
            if (buf[pos] == '\r' && buf[++pos] == '\n' && buf[++pos] == '\r' && buf[++pos] == '\n') {
                found = true;
                break;
            }
            pos++;
        }
        pos++;

        return found ? pos : -1;
    }

    /**
     * format size to k and M
     * @param size the size of file
     * @return format readable string
     */
    public static String formatDataSizeToString(long size) {
        float baseValue = 0;
        if (MEGA_BYTE > size) {
            baseValue = size / KILO_BYTE;
            return Float.toString(baseValue) + " K";
        } else {
            baseValue = size / MEGA_BYTE;
            return Float.toString(baseValue) + " M";
        }
    }

}
