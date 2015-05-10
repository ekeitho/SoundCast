package com.ekeitho.sound.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by ekeitho on 5/10/15.
 */
public class SoundCastSQLHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "soundcast.db";

    public static final String TABLE_NAME = "Soundcast";
    public static final String UNIQUE_ID = "id";
    public static final String STREAM_URL = "streamUrl";
    public static final String ALBUM_ART_URL = "albumArtUrl";
    public static final String ARTIST = "artistName";
    public static final String ARTIST_PERMALINK = "artistPermalinkName";
    public static final String SONG = "songName";
    public static final String SONG_PERMALINK = "songPermalinkName";
    public static final String FIRST_CASTED_TIMESTAMP = "firstTimeCasted";
    public static final String LAST_CASTED_TIMESTAMP = "lastTimeCasted";

    public SoundCastSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_SOUNDCAST_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                UNIQUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                STREAM_URL + " TEXT NOT NULL, " +
                ALBUM_ART_URL + " TEXT NOT NULL, " +
                ARTIST + " TEXT NOT NULL, " +
                ARTIST_PERMALINK + " TEXT NOT NULL, " +
                SONG + " TEXT NOT NULL, " +
                SONG_PERMALINK + " TEXT NOT NULL, " +
                FIRST_CASTED_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                LAST_CASTED_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
        db.execSQL(SQL_CREATE_SOUNDCAST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
