package com.ekeitho.sound;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ekeitho.sound.data.SoundCastItem;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;

/**
 * Created by ekeitho on 5/9/15.
 */
public class SoundCastFragment extends Fragment {

    private static final String TAG = "SoundCastFragment";

    private MainActivity mainActivity;
    private RecyclerView castRecyclerView;
    private SoundCastAdapter soundCastAdapter;
    private FloatingActionsMenu menu;
    protected FloatingActionButton addToQueueActionButton;
    protected FloatingActionButton castActionButton;

    @Override
    public void onAttach(Activity activity) {
        mainActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.soundcast_fragment, container, false);

        setUpFAB(view);

        castRecyclerView = (RecyclerView) view.findViewById(R.id.cast_recycler_view);
        castRecyclerView.setHasFixedSize(true);
        try {
            soundCastAdapter = new SoundCastAdapter(mainActivity);
            castRecyclerView.setAdapter(soundCastAdapter);
            castRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
            castRecyclerView.setItemAnimator(new DefaultItemAnimator());
        } catch (SQLException e) {
            Log.e("SQLError", "SQL error: " + e);
        }

        return view;
    }

    public void setUpFAB(View view) {
           /* menu button */
        menu = (FloatingActionsMenu) view.findViewById(R.id.multiple_actions);

        /* TODO: change for later - just want it to work! */
        int normal_pink = Color.parseColor("#e91e63");
        int pressed_pink = Color.parseColor("#ec407a");

        /* add to queue button */
        addToQueueActionButton = new FloatingActionButton(mainActivity);
        addToQueueActionButton.setColorNormal(normal_pink);
        addToQueueActionButton.setColorPressed(pressed_pink);
        addToQueueActionButton.setIconDrawable(getResources().getDrawable(R.drawable.ic_queue_music_white_24dp));
        addToQueueActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "addToQueueActionButton c");
            }
        });
        menu.addButton(addToQueueActionButton);


        /* cast button */
        castActionButton = new FloatingActionButton(mainActivity);
        castActionButton.setIconDrawable(getResources().getDrawable(R.drawable.ic_cast_white_24dp));
        castActionButton.setColorNormal(normal_pink);
        castActionButton.setColorPressed(pressed_pink);
        castActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final ClipboardManager myClipboard;
                myClipboard = (ClipboardManager) mainActivity.getSystemService(Context.CLIPBOARD_SERVICE);

                /* if not connected lets see if we are near so user can cast
                   and automatically select it for the user */
                if (!mainActivity.isConnected()) {
                    /* if they are near a cast */

                    if (mainActivity.getRouteQueue().peek() != null) {
                        /* lets store a copy of the users result and wait for it to be casted
                           so the user doesnt have to click again
                         */
                        if (myClipboard.getPrimaryClip() != null) {
                           mainActivity.addToPostponedQueue(myClipboard.getText().toString());
                        }
                        mainActivity.getRouteQueue().peek().select();
                    }
                    else {
                        Toast.makeText(mainActivity,
                                "Please connect a Chromecast first.", Toast.LENGTH_SHORT).show();
                    }
                }

                /* they are connected to a chromecast */
                else {
                    if (myClipboard.getPrimaryClip() != null) {
                        getSoundcloudInfo(myClipboard.getText().toString());
                    }
                    else {
                        Toast.makeText(mainActivity,
                                "Please copy a link first, then cast.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        menu.addButton(castActionButton);
    }

    public void getSoundcloudInfo(String text) {

        String input = text.replaceAll(".*soundcloud.com/", "");

        String username = input.replaceAll("/.*", "");
        String songname = input.replaceAll(".*/", "");
        SoundCastItem item = null;

        if ((item = soundCastAdapter.checkIfAlreadyCasted(username, songname)) != null) {
            mainActivity.sendTrack(item.getStreamUrl(), item.getArtist(),
                    item.getSong(), Uri.parse(item.getAlbumArtUrl()));
            Log.v(TAG, "File already cached. Casting!");
        }
        else {
            Log.v(TAG, "File not in cache. Find and Cast!");
            new FetchSoundCloudApi().execute(username, songname);
        }
    }

    private class FetchSoundCloudApi extends AsyncTask<String, Void, SoundCastItem> {
        @Override
        protected SoundCastItem doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }


            String username = params[0];
            String songname = params[1];

            String response = null;
            // Will contain the raw JSON response as a string.
            String scBaseUri = "https://api.soundcloud.com/";
            String client_query = ".json?client_id=" + getString(R.string.soundcloud_id);
            String USERS = scBaseUri + "users/" + username;

            /* since not all tracks/ are unique, i have to look up through users/id/track first */
            response = fetchJSON(USERS + "/tracks" + client_query);


            if (response != null) {
                try {
                    SoundCastItem item = null;
                    /* if the recevied data is okay */
                    if ((item = getSoundcloudDataFromJson(response, songname, username)) != null ) {
                        return item;
                    }
                    else {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mainActivity, "Rejected by Soundcloud.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e("err", e.getMessage(), e);
                    e.printStackTrace();
                }
            }
            else {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mainActivity, "Bad URL given.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            /* end */
            return null;
        }

        @Override
        protected void onPostExecute(SoundCastItem soundCastItem) {
            super.onPostExecute(soundCastItem);
            soundCastAdapter.notifyDataSetChanged();
        }
    }

    public SoundCastItem getSoundcloudDataFromJson(String response, String songPermalink, String perm_username) throws JSONException {
        JSONArray user_tracksJSON = new JSONArray(response);
        JSONObject track_infoJSON;


        for (int i = 0; i < user_tracksJSON.length(); i++) {
            track_infoJSON = user_tracksJSON.getJSONObject(i);
            /* check permalink to for the songPermalink */
            if (track_infoJSON.getString("permalink").equals(songPermalink)) {
                /* make sure it is streamable */
                if (track_infoJSON.getBoolean("streamable")) {
                    /* get streamable url */
                    String stream_url = track_infoJSON.getString("stream_url") +
                            "?client_id=" + getString(R.string.soundcloud_id);
                    /* get the username */
                    String username = track_infoJSON.getJSONObject("user").getString("username");
                    /* get the title of the song */
                    String title = track_infoJSON.getString("title");

                    /* sometimes there isn't an album art - so get the users picture then */
                    String album_art_uri = track_infoJSON.getString("artwork_url");
                    if (album_art_uri.equals("null")) {
                        album_art_uri = track_infoJSON.getJSONObject("user").getString("avatar_url");
                    }
                    album_art_uri = album_art_uri.replaceAll("large", "t500x500");
                    Uri uri = Uri.parse(album_art_uri);

                    /* add the data to a sqlite databse */
                    SoundCastItem item = soundCastAdapter.addCastItem(stream_url, album_art_uri, username,
                            perm_username, title, songPermalink);
                    /* send to the cast ! */
                    mainActivity.sendTrack(stream_url, username, title, uri);

                    return item;
                }
                /* if the url isn't streamable - notify the user */
                else {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainActivity,
                                    "URL given is not streamable.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        return null;
    }

    private String fetchJSON(String scFetchDataString) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String soundcloudJsonString = null;

        try {
            URL url = new URL(scFetchDataString);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            soundcloudJsonString = buffer.toString();
        } catch (IOException e) {
            Log.e("err", "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("err", "Error closing stream", e);
                }
            }
        }
        return soundcloudJsonString;
    }

}
