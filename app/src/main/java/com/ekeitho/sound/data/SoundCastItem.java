package com.ekeitho.sound.data;

import java.sql.Timestamp;

/**
 * Created by ekeitho on 5/9/15.
 */
public class SoundCastItem {

    private long id;
    private String stream_url;
    private String album_art_url;
    private String artist_name;
    private String artist_permalink;
    private String song_name;
    private String song_permalink;
    private Timestamp first_casted_timestamp;
    private Timestamp last_casted_timestamp;



    public SoundCastItem(long id, String stream_url, String album_art_url,
                            String artist_name, String artist_permalink,
                            String song_name, String song_permalink,
                            Timestamp first, Timestamp last) {
        this.id = id;
        this.stream_url = stream_url;
        this.album_art_url = album_art_url;
        this.artist_name = artist_name;
        this.artist_permalink = artist_permalink;
        this.song_name = song_name;
        this.song_permalink = song_permalink;
        this.first_casted_timestamp = first;
        this.last_casted_timestamp = last;
    }

    public long getId() {
        return id;
    }
    public String getArtistPermalink() {
        return artist_permalink;
    }
    public String getStreamUrl() {
        return stream_url;
    }
    public String getAlbumArtUrl() {
        return this.album_art_url;
    }
    public String getArtist() {
        return artist_name;
    }
    public String getSong() {
        return song_name;
    }
    public String getSongPermalink() {
        return song_permalink;
    }
    public Timestamp getFirstCastedTimestamp() {
        return first_casted_timestamp;
    }
    public Timestamp getlastCastedTimestamp() {
        return last_casted_timestamp;
    }

}
