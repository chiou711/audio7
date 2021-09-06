package com.cw.audio7.audio;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.cw.audio7.R;
import com.cw.audio7.db.DatabaseHelper;
import com.cw.audio7.util.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import static com.cw.audio7.define.Define.ENABLE_MEDIA_CONTROLLER;

// AudioManager.OnAudioFocusChangeListener: added in API level 8
public class BackgroundAudioService extends MediaBrowserServiceCompat
        implements MediaPlayer.OnPreparedListener,
                            MediaPlayer.OnCompletionListener,
                            MediaPlayer.OnSeekCompleteListener,
                            MediaPlayer.OnErrorListener,
                            AudioManager.OnAudioFocusChangeListener  {

    public static MediaPlayer mMediaPlayer;
    public static boolean mIsPrepared;
    public static boolean mIsCompleted;

    public static MediaSessionCompat mMediaSessionCompat;

    //if (ENABLE_MEDIA_CONTROLLER)
    public static MediaBrowserCompat mMediaBrowserCompat;
    public static MediaControllerCompat mMediaControllerCompat;

    final public static int mNotification_id = 78;

    // for differentiate Pause source: manual or focus change
    private boolean isPausedByButton;

    public static Audio_manager mAudio_manager;

    //    boolean enDbgMsg = true;
    boolean enDbgMsg = true;

    public static PlaybackStateCompat.Builder playbackStateBuilder;

    public static DatabaseHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _onCreate");

        if (ENABLE_MEDIA_CONTROLLER)
            initMediaSession();

        initNoisyReceiver();
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(audioNoisyReceiver, filter);
    }

    BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            // when phone jack is unplugged
            if (android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                if((mMediaPlayer != null) && mMediaPlayer.isPlaying() )
                {
                    if(enDbgMsg)
                        System.out.println("BackgroundAudioService / audioNoisyReceiver / _onReceive / play -> pause");
                    pauseAudio();
                }
            }

        }
    };

    NotificationCompat.Builder builder;
    NotificationManagerCompat manager;
    String CHANNEL_ID = "77";

    // init Media Player
    private void initMediaPlayer() {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _initMediaPlayer");

        if(mMediaPlayer != null) {
            mMediaPlayer.pause();
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }

        mMediaPlayer = null;

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setVolume(1.0f, 1.0f);

        setAudioPlayerListeners();
    }

    // set Audio Player Listeners
    void setAudioPlayerListeners() {
        mMediaPlayer.setOnPreparedListener(BackgroundAudioService.this);
        mMediaPlayer.setOnCompletionListener(BackgroundAudioService.this);
        mMediaPlayer.setOnSeekCompleteListener(BackgroundAudioService.this);
        mMediaPlayer.setOnErrorListener(BackgroundAudioService.this);
    }

    // init notification
    void initNotification() {
        if (ENABLE_MEDIA_CONTROLLER && Build.VERSION.SDK_INT >= 26) {
            manager = NotificationManagerCompat.from(this);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getResources().getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Set audio player listeners
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _onPrepared");

        mediaPlayer.seekTo(0);
        mIsPrepared = true;

        // prepared, start Play audio
        playAudio();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _setAudioPlayerListeners / onCompleted");

        if(mediaPlayer != null) {
            mediaPlayer.release();

            // disconnect media browser
            if (ENABLE_MEDIA_CONTROLLER &&  Build.VERSION.SDK_INT >= 21) {
                if( mMediaControllerCompat.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ) {
                    mMediaControllerCompat.getTransportControls().stop();// .pause();
                }
            }
        }

        // delay interval between each media change
//        try {
//            Thread.sleep(Util.oneSecond * 2);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        mMediaPlayer = null;
        mIsCompleted = true;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
            setSeekerBarProgress(mediaPlayer);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // ... react appropriately ...
        // The MediaPlayer has moved to the Error state, must be reset!
        if(enDbgMsg) {
            System.out.println("BackgroundAudioService / _onError / what = " + what);
            System.out.println("BackgroundAudioService / _onError / extra = " + extra);
        }

        return false;
    }

    @Override
    public void onDestroy() {
        System.out.println("BackgroundAudioService / _onDestroy");
        super.onDestroy();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(audioNoisyReceiver);

        if (ENABLE_MEDIA_CONTROLLER) {
            mMediaSessionCompat.release();
            NotificationManagerCompat.from(this).cancel(mNotification_id);
        }

        if (mMediaPlayer != null)
            mMediaPlayer.release();
    }

    // do audio play
    void playAudio(){
        System.out.println("BackgroundAudioService / _playAudio");

        if (ENABLE_MEDIA_CONTROLLER) {
            mMediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            initMediaSessionMetadata();
            showPlayingNotification();
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            mMediaPlayer.setVolume(1.0f, 1.0f);
        }

        // update panel status: play
        mAudio_manager.setPlayerState(mAudio_manager.PLAYER_AT_PLAY);

        mAudio_manager.setTogglePlayerState(true);

        isPausedByButton = false;
    }

    // do audio pause
    void pauseAudio() {

        if (ENABLE_MEDIA_CONTROLLER) {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            initMediaSessionMetadata();
            showPausedNotification();
        }

        if( mMediaPlayer != null  ) {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.pause();
        }

        // update panel status: pause
        mAudio_manager.setPlayerState(mAudio_manager.PLAYER_AT_PAUSE);
        mAudio_manager.setTogglePlayerState(true);
    }

    // is Failed Retrieved Audio Focus
    private boolean isFailedRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result != AudioManager.AUDIOFOCUS_GAIN;
    }

    // set seeker bar progress
    public static void setSeekerBarProgress(MediaPlayer mediaPlayer) {

        int currPos;
        if(mediaPlayer==null)
            return;
        else
            currPos =  mediaPlayer.getCurrentPosition();

        if (ENABLE_MEDIA_CONTROLLER) {
            if (mAudio_manager.getPlayerState() == mAudio_manager.PLAYER_AT_PLAY) {
                playbackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, currPos, 0);
                mMediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
            } else if (mAudio_manager.getPlayerState() == mAudio_manager.PLAYER_AT_PAUSE) {
                playbackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, currPos, 0);
                mMediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
            }
        }
    }

    //    if (ENABLE_MEDIA_CONTROLLER)
    /** Callback functions of Media Session Compat */
    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onSkipToNext");

            if (ENABLE_MEDIA_CONTROLLER) {
                setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
                mMediaSessionCompat.setActive(true);
            }

            mAudio_manager.setPlayNext(true);
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onSkipToPrevious");

            if (ENABLE_MEDIA_CONTROLLER) {
                setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
                mMediaSessionCompat.setActive(true);
            }

            mAudio_manager.setPlayPrevious(true);
        }

        @Override
        public void onPlay() {
            super.onPlay();
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onPlay");

            if(isFailedRetrievedAudioFocus()) {
                return;
            }
            playAudio();
        }

        @Override
        public void onPause() {
            super.onPause();
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onPause");

            isPausedByButton = true;
            pauseAudio();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onPlayFromUri / uri = " + uri);

            if(isFailedRetrievedAudioFocus()) {
                return;
            }

            mIsPrepared = false;

            initMediaPlayer();

            if(ENABLE_MEDIA_CONTROLLER)
                initNotification();

            try {
                mMediaPlayer.setDataSource(getApplicationContext(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                mMediaPlayer.prepareAsync();
                mIsPrepared = false;
                mIsCompleted = false;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            setSeekerBarProgress(mMediaPlayer);
        }
    };

    //if (ENABLE_MEDIA_CONTROLLER)
    /** init Media Session*/
    private void initMediaSession() {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _initMediaSession");

        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", mediaButtonReceiver, null);
        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());

        playbackStateBuilder = new PlaybackStateCompat.Builder();
    }

    //if (ENABLE_MEDIA_CONTROLLER)
    private void initMediaSessionMetadata() {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _initMediaSessionMetadata");

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        //Notification icon in card
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        String audioStr = mAudio_manager.getAudioStringAt(mAudio_manager.mAudioPos);
        String[] displayItems=Util.getDisplayNameByUriString(audioStr, getApplicationContext());

        // prepare bit map
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try
        {
            mmr.setDataSource(this.getApplicationContext(),Uri.parse(audioStr));

            byte[] artBytes =  mmr.getEmbeddedPicture();
            if(artBytes != null)
            {
                InputStream is = new ByteArrayInputStream(mmr.getEmbeddedPicture());
                bitmap = BitmapFactory.decodeStream(is);
            }
            mmr.release();
        }
        catch(Exception e)
        {
            Log.e("BackgroundAudioService", "setDataSource / illegal argument");
        }

        //lock screen icon for pre lollipop
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(MainAct.mAct.getResources(), R.drawable.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, displayItems[0]);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, displayItems[1]);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        // for wearable device title
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,  displayItems[0]);

        // for seeker bar progress
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mMediaPlayer.getDuration());

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    //if (ENABLE_MEDIA_CONTROLLER)
    private void setMediaPlaybackState(int state) {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _setMediaPlaybackState / state = " + state);
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                            PlaybackStateCompat.ACTION_PAUSE |
                                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                            PlaybackStateCompat.ACTION_SEEK_TO);
        }
        else if ( state == PlaybackStateCompat.STATE_PAUSED ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                            PlaybackStateCompat.ACTION_PLAY|
                                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                            PlaybackStateCompat.ACTION_SEEK_TO);
        }

        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }

    //    if (ENABLE_MEDIA_CONTROLLER)
    private void showPlayingNotification() {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _showPlayingNotification");

        builder = MediaStyleHelper.from(this, mMediaSessionCompat);

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous,
                "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setMediaSession(mMediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setShowWhen(false);

        if (Build.VERSION.SDK_INT >= 26)
            manager.notify(mNotification_id,builder.setChannelId(CHANNEL_ID).build());
        else
            manager.notify(mNotification_id, builder.build());
    }

    //if (ENABLE_MEDIA_CONTROLLER)
    private void showPausedNotification() {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _showPausedNotification");

        builder = MediaStyleHelper.from(this, mMediaSessionCompat);

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous,
                "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setMediaSession(mMediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setShowWhen(false);

        if (Build.VERSION.SDK_INT >= 26)
            manager.notify(mNotification_id,builder.setChannelId(CHANNEL_ID).build());
        else
            manager.notify(mNotification_id, builder.build());
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        System.out.println("BackgroundAudioService / _onAudioFocusChange");

        switch( focusChange ) {
            case AudioManager.AUDIOFOCUS_LOSS:
            {
                System.out.println("BackgroundAudioService / _onAudioFocusChange / AudioManager.AUDIOFOCUS_LOSS");
                // example: play YouTube
                pauseAudio();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                // example: when phone call is coming in, when call out
                // example: play video of FB
                System.out.println("BackgroundAudioService / _onAudioFocusChange / AudioManager.AUDIOFOCUS_LOSS_TRANSIENT");
                pauseAudio();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                System.out.println("BackgroundAudioService / _onAudioFocusChange / AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                if( mMediaPlayer != null ) {
                    mMediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                System.out.println("BackgroundAudioService / _onAudioFocusChange / AudioManager.AUDIOFOCUS_GAIN");
                // example: when incoming phone call is off line
                // example: when pausing video of FB

                // do not play if user pressed the play button
                if(!isPausedByButton)
                    playAudio();
                break;
            }
        }
    }

    //Not important for general audio service, required for class
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }
        return null;
    }

    //Not important for general audio service, required for class
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }
}