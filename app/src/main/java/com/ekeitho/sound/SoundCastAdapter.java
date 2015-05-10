package com.ekeitho.sound;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ekeitho.sound.data.SoundCastDataSource;
import com.ekeitho.sound.data.SoundCastItem;
import com.squareup.picasso.Picasso;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by ekeitho on 5/10/15.
 */
public class SoundCastAdapter extends RecyclerView.Adapter<SoundCastAdapter.ViewHolder> {

    private SoundCastDataSource source;
    private ArrayList<SoundCastItem> castItems;
    private Context context;

    public SoundCastAdapter(Context context) throws SQLException {
        source = new SoundCastDataSource(this.context = context);
        source.open();
        this.castItems = source.getAllCastedItems();
    }

    public SoundCastItem addCastItem(String s_url, String a_url,
                                     String artist, String artist_permalink,
                                     String song, String song_permalink) {
        return source.createCastItem(s_url, a_url, artist, artist_permalink, song, song_permalink);
    }

    public SoundCastItem checkIfAlreadyCasted(String username, String songname) {
        return source.checkIfAlreadyCasted(username, songname);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        CardView view = (CardView) LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.sound_cast_card, viewGroup, false);

        ImageView imageView = (ImageView) view.findViewById(R.id.album_art_view);
        Button cacheButton = (Button) view.findViewById(R.id.cache_cast_button);
        ViewHolder vh = new ViewHolder(view, imageView, cacheButton);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        /* sets the image view from the album art url */
        final SoundCastItem item = castItems.get(i);
        Picasso.with(context).load(item.getAlbumArtUrl()).into(viewHolder.imageView);
        viewHolder.cacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)context).sendTrack(item.getStreamUrl(), item.getArtist(),
                        item.getSong(), Uri.parse(item.getAlbumArtUrl()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return castItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public ImageView imageView;
        public Button cacheButton;

        public ViewHolder(CardView view, ImageView view2, Button button) {
            super(view);
            this.cardView = view;
            this.imageView = view2;
            this.cacheButton = button;
        }

    }
}
