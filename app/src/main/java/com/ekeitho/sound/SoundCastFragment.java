package com.ekeitho.sound;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ekeitho on 5/9/15.
 */
public class SoundCastFragment extends Fragment {

    private MainActivity mainActivity;
    private Button cast_button;
    private EditText cast_text;

    @Override
    public void onAttach(Activity activity) {
        mainActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    private void getSoundcloudInfo(String text) {
        String input = text.replaceAll(".*soundcloud.com/", "");
        String username = input.replaceAll("/.*", "");
        String songname = input.replaceAll(".*/", "");
        new FetchSoundCloudApi().execute(username, songname);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.soundcast_fragment, container, false);

        cast_button = (Button) view.findViewById(R.id.cast_button);
        cast_text = (EditText) view.findViewById(R.id.cast_text);

        cast_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity.isConnected()) {
                    getSoundcloudInfo(cast_text.getText().toString());
                } else {
                    Toast.makeText(mainActivity,
                            "Please connect Chromecast first.", Toast.LENGTH_SHORT).show();
                }
                cast_text.setText("");
            }
        });
        return view;
    }

    public void getSoundcloudDataFromJson(String response, String songname) throws JSONException {
        JSONArray user_tracksJSON = new JSONArray(response);
        JSONObject track_infoJSON;

        for (int i = 0; i < user_tracksJSON.length(); i++) {
            track_infoJSON = user_tracksJSON.getJSONObject(i);
            /* check permalink to for the songname */
            if (track_infoJSON.getString("permalink").equals(songname)) {
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

                    System.out.println(album_art_uri);
                    Uri uri = Uri.parse(album_art_uri);

                    /* send to the cast ! */
                    mainActivity.sendTrack(stream_url, username, title, uri);
                }
                /* if the url isn't streamable - notify the user */
                else {
                    Toast.makeText(mainActivity,
                                "URL given is not streamable.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class FetchSoundCloudApi extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {

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
                    getSoundcloudDataFromJson(response, songname);
                } catch (JSONException e) {
                    Log.e("err", e.getMessage(), e);
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(mainActivity, "Bad URL given.", Toast.LENGTH_SHORT).show();
            }

            /* end */
            return null;
        }
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
