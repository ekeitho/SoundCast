package com.ekeitho.sound.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
            SoundCastSQLHelper.ARTIST, SoundCastSQLHelper.SONG,
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

    public SoundCastItem createCastItem(long id, String s_url, String a_url, String artist, String song) {
        ContentValues values = new ContentValues();
        values.put(allColumns[0], id);
        values.put(allColumns[1], s_url);
        values.put(allColumns[2], a_url);
        values.put(allColumns[3], artist);
        values.put(allColumns[4], song);
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
                cursor.getString(4), /* song name */
                new Timestamp(cursor.getLong(5)), /* first time stamp */
                new Timestamp(cursor.getLong(6)) /* last time stamp */
        );
        return item;
    }

}
