package com.example.TransmitWifi;

import android.database.Cursor;
import android.provider.MediaStore;
import java.util.Properties;

/**
 * 媒体对象
 * @author haihui.li
 * @version 1.0.0
 */
public class MediaItem {
    private static final String TAG = "MediaItem";
    /**
     * database field name
     */
    public static final String[] COLUMNS = {MediaStore.Audio.Media._ID
        , MediaStore.Audio.Media.DATA
        , MediaStore.Audio.Media.TITLE
        , MediaStore.Audio.Media.MIME_TYPE
        , MediaStore.Audio.Media.ARTIST
        , MediaStore.Audio.Media.ALBUM
        , MediaStore.Audio.Media.SIZE
        , MediaStore.Audio.Media.DURATION};

    private long mId;
    private long mSize;
    private long mDuration;
    private String mData;
    private String mTitle;
    private String mMimeType;
    private String mArtist;
    private String mAlbum;

    /**
     * clone item
     * @param item will be used to clone
     */
    protected void cloneItem(MediaItem item) {
        mId = item.mId;
        mSize = item.mSize;
        mDuration = item.mDuration;
        mData = item.mData;
        mTitle = item.mTitle;
        mMimeType = item.mMimeType;
        mArtist = item.mArtist;
        mAlbum = item.mAlbum;
    }

    /**
     * constructor for media item
     */
    public MediaItem() {
    }
    /**
     * Constructor for media item
     * @param data the path of this item
     * @param mimeType the mime type of this item
     * @param title the title of this item
     * @param size the size of this item
     */
    public MediaItem(String data, String mimeType, String title, long size) {
        mData = data;
        mMimeType = mimeType;
        mSize = size;
    }

    /**
     * Constructor for media item
     * @param cursor cursor of media database
     */
    public MediaItem(Cursor cursor) {
        if (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
            mId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            mSize = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            mDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            mData = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            mTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            mMimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
            mArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            mAlbum  = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        }
    }

    /**
     * Get the size of this item
     * @return the size of the item
     */
    long getSize() {
        return mSize;
    }
    /**
     * get path of the item
     * @return  the path
     */
    String getPath() {
        return mData;
    }
    /**
     *get mime type of the item
     *@return the mime type as string
     */
    String getMimeType() {
        return mMimeType;
    }

    /**
     * get Title of the item
     * @return the tile
     */
    String getTitle() {
        return mTitle;
    }

    /**
     * set the size of this item
     * @param size the size of the item
     */
    void setSize(long size) {
        mSize = size;
    }
    /**
     * set path of the item
     * @param path the path
     */
    void setPath(String path) {
        mData = path;
    }
    /**
     *set mime type of the item
     *@return the mime type as string
     */
    void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    /**
     * set Title of the item
     * @return the tile
     */
    void setTitle(String title) {
        mTitle = title;
    }

    /**
     * get id in the media database
     * @return the id of media item in the media database
     */
    long getId() {
        return mId;
    }

    /**
     * Contructor of MediaItem
     * @param properties media item
     */
    public MediaItem(Properties properties) {
        try {
            mId = Integer.parseInt(properties.getProperty(MediaStore.Audio.Media._ID));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            mSize = Integer.parseInt(properties.getProperty(MediaStore.Audio.Media.SIZE));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            mId = Integer.parseInt(properties.getProperty(MediaStore.Audio.Media.DURATION));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        mData = properties.getProperty(MediaStore.Audio.Media.DATA);
        mTitle = properties.getProperty(MediaStore.Audio.Media.TITLE);
        mMimeType = properties.getProperty(MediaStore.Audio.Media.MIME_TYPE);
        mArtist = properties.getProperty(MediaStore.Audio.Media.ARTIST);
        mAlbum = properties.getProperty(MediaStore.Audio.Media.ALBUM);
    }

    /**
     * to get the info of media item
     * @return media item as a string
     */
    public String toString() {
        return mTitle + "   id = " + mId;
    }
}
