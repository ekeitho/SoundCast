package com.ekeitho.sound.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Created by ekeitho on 5/10/15.
 */
public class SoundCastDataSource {

    private SQLiteDatabase db;
    private SoundCastSQLHelper dbHelper;
    private String[] allColumns = { SoundCastSQLHelper.UNIQUE_ID,
            SoundCastSQLHelper.STREAM_URL, SoundCastSQLHelper.ALBUM_ART_URL,
            SoundCastSQLHelper.ARTIST, SoundCastSQLHelper.ARTIST_PERMALINK,
            SoundCastSQLHelper.SONG, SoundCastSQLHelper.SONG_PERMALINK,
            SoundCastSQLHelper.FIRST_CASTED_TIMESTAMP, SoundCastSQLHelper.LAST_CASTED_TIMESTAMP};

    public SoundCastDataSource(Context context) {
        dbHelper = new SoundCastSQLHelper(context);
    }

    public void open() throws SQLException {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /* TODO: */
    public void updateCastItemTimestamp() {

    }

    public SoundCastItem checkIfAlreadyCasted(String username, String songname) {
        Cursor cursor = db.query(SoundCastSQLHelper.TABLE_NAME,
                allColumns,
                SoundCastSQLHelper.ARTIST_PERMALINK + " = '" + username + "' AND " +
                SoundCastSQLHelper.SONG_PERMALINK + " = '" + songname + "'", null, null, null, null);

        /* if nothing matches then the user hasn't casted this item yet */
        if (!cursor.moveToFirst()) {
            return null;
        }
        return cursorToCastItem(cursor);
    }

    public ArrayList<SoundCastItem> getAllCastedItems() {
        ArrayList<SoundCastItem> items = new ArrayList<>();

        Cursor cursor = db.query(SoundCastSQLHelper.TABLE_NAME,
                allColumns, null, null, null, null, null, null);
        cursor.moveToFirst();

        while(!cursor.isAfterLast()) {
            SoundCastItem castItem = cursorToCastItem(cursor);
            items.add(castItem);
            cursor.moveToNext();
        }
        cursor.close();
        return items;
    }

    public SoundCastItem createCastItem(String s_url, String a_url, String artist,
                                        String permalink_username, String song,
                                        String song_permalink) {
        ContentValues values = new ContentValues();
        values.put(SoundCastSQLHelper.STREAM_URL, s_url);
        values.put(SoundCastSQLHelper.ALBUM_ART_URL, a_url);
        values.put(SoundCastSQLHelper.ARTIST, artist);
        values.put(SoundCastSQLHelper.ARTIST_PERMALINK, permalink_username);
        values.put(SoundCastSQLHelper.SONG, song);
        values.put(SoundCastSQLHelper.SONG_PERMALINK, song_permalink);
        /* when inserted the first & last timestamp are the same */
        long insert_id = db.insert(SoundCastSQLHelper.TABLE_NAME, null, values);
        Cursor cursor = db.query(SoundCastSQLHelper.TABLE_NAME, allColumns,
                SoundCastSQLHelper.UNIQUE_ID + "=" + insert_id, null, null, null, null);
        cursor.moveToFirst();

        SoundCastItem item = cursorToCastItem(cursor);
        cursor.close();
        return item;
    }

    public SoundCastItem cursorToCastItem(Cursor cursor) {
        SoundCastItem item = new SoundCastItem(
                cursor.getLong(0), /* id */
                cursor.getString(1), /* stream url */
                cursor.getString(2), /* album art url */
                cursor.getString(3), /* artist name */
                cursor.getString(4), /* artist permalink */
                cursor.getString(5), /* song name */
                cursor.getString(6), /* song permalink */
                new Timestamp(cursor.getLong(7)), /* first time stamp */
                new Timestamp(cursor.getLong(8)) /* last time stamp */
        );
        return item;
    }

}
