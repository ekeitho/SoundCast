package com.ekeitho.sound;

/* Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

/**
 * Main activity to send messages to the receiver.
 */
public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1;
    private static int test = 1;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private Cast.Listener mCastListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private boolean mApplicationStarted;
    private boolean mWaitingForReconnect;
    private String mSessionId;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private Queue<RouteInfo> routes;
    private Queue<String> postponedCasts;
    private SoundCastFragment soundCastFragment;
    private String shareMsgFromSoundcloud;

    /* this is used when the app is open and the user wants to share from soundcloud */
    private static int hasOpened = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("show up", "here!");
        setContentView(R.layout.activity_main);
        /* intiialize data structs */
        routes = new ArrayDeque<>();
        postponedCasts = new ArrayDeque<>();


        /* get reference to our fragment */
        soundCastFragment = (SoundCastFragment)
                getSupportFragmentManager().findFragmentById(R.id.castFragment);

        // Configure Cast device discovery
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        CastMediaControlIntent.categoryForCast(getResources()
                                .getString(R.string.app_id))).build();
        mMediaRouterCallback = new MyMediaRouterCallback();

        /* initialize our cast media player */
        mRemoteMediaPlayer = new RemoteMediaPlayer();


        shareMsgFromSoundcloud = handleIncomingIntentShareIfAny();
        /* this will happen when user wants to share from soundcloud & app is open */
        if (hasOpened > 0) {
            routes.add(mMediaRouter.getRoutes().get(1));
            postponedCasts.add(shareMsgFromSoundcloud);

            if (routes.peek().isSelected()) {
                mMediaRouter.unselect(1);
            }
            routes.peek().select();
        }
        /* this will happen if share happens and the app isn't open yet */
        else if(shareMsgFromSoundcloud != null) {
            postponedCasts.add(shareMsgFromSoundcloud);
        }

        /* update to show change in phase */
        hasOpened++;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        double VOLUME_INCREMENT = 0.05;
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mRemoteMediaPlayer != null) {
                        double currentVolume = Cast.CastApi.getVolume(mApiClient);
                        if (currentVolume < 1.0) {
                            try {
                                Cast.CastApi.setVolume(mApiClient,
                                        Math.min(currentVolume + VOLUME_INCREMENT, 1.0));
                            } catch (Exception e) {
                                Log.e(TAG, "unable to set volume", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "dispatchKeyEvent - volume up");
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mRemoteMediaPlayer != null) {
                        double currentVolume = Cast.CastApi.getVolume(mApiClient);
                        if (currentVolume > 0.0) {
                            try {
                                Cast.CastApi.setVolume(mApiClient,
                                        Math.max(currentVolume - VOLUME_INCREMENT, 0.0));
                            } catch (Exception e) {
                                Log.e(TAG, "unable to set volume", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "dispatchKeyEvent - volume down");
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start media router discovery
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    public boolean isConnected() {
        return mApplicationStarted;
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            // End media router discovery
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        teardown();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem castMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
                .getActionProvider(castMenuItem);
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    /**
     * Callback for MediaRouter events
     */
    private class MyMediaRouterCallback extends MediaRouter.Callback {


        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
            super.onRouteAdded(router, route);
            /* there could be more than one route seen */
            Log.v("route", "added!");
            if (!routes.add(route)) {
                Log.e(TAG, "Adding to queue failed.");
            }
            /* lets launch the cast right away for the user */
            if (shareMsgFromSoundcloud != null) {
                routes.peek().select();
            }
        }

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
            // Handle the user route selection.
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            launchReceiver();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            teardown();
            mSelectedDevice = null;
        }
    }

    /**
     * Start the receiver app
     */
    private void launchReceiver() {
        try {
            mCastListener = new Cast.Listener() {

                @Override
                public void onApplicationDisconnected(int errorCode) {
                    Log.d(TAG, "application has stopped");
                    teardown();
                }

            };
            // Connect to Google Play services
            mConnectionCallbacks = new ConnectionCallbacks();
            mConnectionFailedListener = new ConnectionFailedListener();
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastListener);

            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            mApiClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Failed launchReceiver", e);
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected");

            if (mApiClient == null) {
                // We got disconnected while this runnable was pending
                // execution.
                return;
            }

            try {
                if (mWaitingForReconnect) {
                    mWaitingForReconnect = false;

                    // Check if the receiver app is still running
                    if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                        Log.d(TAG, "App  is no longer running");
                        teardown();
                    } else {
                        // Re-create the custom message channel
                        try {
                            Cast.CastApi.setMessageReceivedCallbacks(
                                    mApiClient,
                                    mRemoteMediaPlayer.getNamespace(),
                                    mRemoteMediaPlayer);
                        } catch (IOException e) {
                            Log.e(TAG, "Exception while creating channel", e);
                        }
                    }
                } else {
                    // Launch the receiver app
                    Cast.CastApi
                            .launchApplication(mApiClient,
                                    getString(R.string.app_id), false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(
                                                ApplicationConnectionResult result) {
                                            Status status = result.getStatus();

                                            if (status.isSuccess()) {
                                                // Create the custom message
                                                // channel
                                                try {
                                                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                                            mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
                                                } catch (IOException e) {
                                                    Log.e(TAG, "Exception while creating media channel", e);
                                                }

                                                mApplicationStarted = true;
                                                mRemoteMediaPlayer
                                                        .requestStatus(mApiClient)
                                                        .setResultCallback(
                                                                new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                                                    @Override
                                                                    public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                                                        if (!result.getStatus().isSuccess()) {
                                                                            Log.e(TAG, "Failed to request status.");

                                                                        } else {
                                                                            if (postponedCasts.peek() != null) {
                                                                                soundCastFragment.getSoundcloudInfo(postponedCasts.poll());
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                            } else {
                                                Log.e(TAG,
                                                        "application could not launch");
                                                teardown();
                                            }
                                        }
                                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended");
            mWaitingForReconnect = true;
        }

    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed ");

            teardown();
        }
    }

    /**
     * Tear down the connection to the receiver
     */
    private void teardown() {
        Log.d(TAG, "teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected() || mApiClient.isConnecting()) {
                    try {
                        Cast.CastApi.stopApplication(mApiClient, mSessionId);
                        if (mRemoteMediaPlayer != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    mApiClient,
                                    mRemoteMediaPlayer.getNamespace());
                            mRemoteMediaPlayer = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while removing channel", e);
                    }
                    mApiClient.disconnect();
                }
                mApplicationStarted = false;
            }
            mApiClient = null;
        }
        mSelectedDevice = null;
        mWaitingForReconnect = false;
        mSessionId = null;
    }

    private String handleIncomingIntentShareIfAny() {
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        /* soundclouds share link comes in second and will not have a split
           greater than 0! hackity hack hack!
         */
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String input = intent.getStringExtra(Intent.EXTRA_TEXT);
                return input.split("\n")[1];
            }
        }
        return null;
    }

    /* this is useful if the user isn't connected to a cast yet,
        but clicks the cast button, so lets not let them do more work!
     */
    public void addToPostponedQueue(String url) {
        postponedCasts.add(url);
    }

    public Queue<RouteInfo> getRouteQueue() {
        return routes;
    }


    public void sendTrack(String url, String artist, String title, Uri album_art) {
        if (mApiClient != null) {
            MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            mediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, artist);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
            mediaMetadata.addImage(new WebImage(album_art));
            MediaInfo mediaInfo = new MediaInfo.Builder(
                    url)
                    .setContentType("audio/mpeg")
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(mediaMetadata)
                    .build();
            try {
                mRemoteMediaPlayer.load(mApiClient, mediaInfo, true)
                        .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                if (result.getStatus().isSuccess()) {
                                    Log.d(TAG, "Media loaded successfully");
                                }
                            }
                        });
            } catch (IllegalStateException e) {
                Log.e(TAG, "Problem occurred with media during loading", e);
            } catch (Exception e) {
                Log.e(TAG, "Problem opening media during loading", e);
            }
        } else {
            Log.e(TAG, "Problem with null mApiClient.");
        }
    }

}