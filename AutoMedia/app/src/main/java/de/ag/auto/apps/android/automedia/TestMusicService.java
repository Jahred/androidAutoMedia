package de.ag.auto.apps.android.automedia;

import android.content.Intent;
import android.drm.DrmStore;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestMusicService extends MediaBrowserService {
    private MediaSession mediaSession;
    private List<MediaMetadata> mMusic;
    private MediaMetadata mCurrentTrack;


    public TestMusicService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mMusic = new ArrayList<MediaMetadata>();
        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "http://storage.googleapis.com/")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Music 1")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist 1")
                .putString(MediaMetadata.METADATA_KEY_DURATION, "30000").build());

        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "http://storage.googleapis.com/")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Music 2")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist 2")
                .putString(MediaMetadata.METADATA_KEY_DURATION, "30000").build());

        final MediaPlayer mediaPlayer = new MediaPlayer();
        mediaSession = new MediaSession(this, "myMusicService");
        mediaSession.setCallback(new MediaSession.Callback() {
                                     @Override
                                     public void onPlayFromMediaId(String mediaTitle, Bundle extras) {
                                         for (MediaMetadata item : mMusic) {
                                             if (item.getDescription().getMediaId().equals(mediaTitle)) {
                                                 mCurrentTrack = item;
                                             }
                                             break;
                                         }
                                         handlePlay();
                                     }


                                     @Override
                                     public void onPlay() {
                                         if (mCurrentTrack == null) {
                                             mCurrentTrack = mMusic.get(0);
                                             handlePlay();
                                         } else {
                                             mediaPlayer.start();
                                             mediaSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
                                         }
                                     }

                                     private void handlePlay() {
                                         mediaSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
                                         mediaSession.setMetadata(mCurrentTrack);
                                         try {
                                             mediaPlayer.reset();
                                             mediaPlayer.setDataSource(TestMusicService.this
                                                     , Uri.parse(mCurrentTrack.getDescription().getMediaId()));
                                         } catch (IOException e) {
                                             e.printStackTrace();
                                         }
                                         mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                             @Override
                                             public void onPrepared(MediaPlayer mp) {
                                                 mp.start();
                                             }
                                         });
                                         mediaPlayer.prepareAsync();
                                     }

                                     @Override
                                     public void onPause() {
                                         mediaPlayer.pause();
                                         mediaSession.setPlaybackState(buildState(PlaybackState.STATE_PAUSED));

                                     }

                                     private PlaybackState buildState(int state) {
                                         return new PlaybackState.Builder()
                                                 .setActions(PlaybackState.ACTION_PLAY
                                                         | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                                                         | PlaybackState.ACTION_SKIP_TO_NEXT
                                                         | PlaybackState.ACTION_FAST_FORWARD
                                                         | PlaybackState.ACTION_PLAY_PAUSE
                                                         | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
                                                 .setState(state, mediaPlayer.getCurrentPosition()
                                                         , 1
                                                         , SystemClock.elapsedRealtime())
                                                 .build();
                                     }

                                 }

        );
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
        setSessionToken(mediaSession.getSessionToken());

    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> list = new ArrayList<MediaBrowser.MediaItem>();
        for (MediaMetadata metadata : mMusic) {
            list.add(new MediaBrowser.MediaItem(metadata.getDescription()
                    , MediaBrowser.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(list);
    }
}
