package com.ekeitho.sound;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ekeitho.sound.data.SoundCastDataSource;
import com.ekeitho.sound.data.SoundCastItem;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by ekeitho on 5/10/15.
 */
public class SoundCastAdapter extends RecyclerView.Adapter<SoundCastAdapter.ViewHolder> {

    private SoundCastDataSource source;
    private ArrayList<SoundCastItem> castItems;

    public SoundCastAdapter(Context context) throws SQLException {
        source = new SoundCastDataSource(context);
        source.open();
        this.castItems = source.getAllCastedItems();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        CardView view = (CardView) LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.sound_cast_card, viewGroup, false);


        ImageView imageView = (ImageView) view.findViewById(R.id.album_art_view);
        ViewHolder vh = new ViewHolder(view, imageView);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        /* sets the image view from the album art url */
        viewHolder.imageView.setImageURI(Uri.parse(castItems.get(i).getAlbumArtUrl()));
    }

    @Override
    public int getItemCount() {
        return castItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public ImageView imageView;

        public ViewHolder(CardView view, ImageView view2) {
            super(view);
            this.cardView = view;
            this.imageView = view2;
        }

    }
}
