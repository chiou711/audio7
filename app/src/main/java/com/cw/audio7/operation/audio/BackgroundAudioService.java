package com.cw.audio7.operation.audio;

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
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.cw.audio7.R;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;
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

// AudioManager.OnAudioFocusChangeListener: added in API level 8
public class BackgroundAudioService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener  {

    public static final String COMMAND_EXAMPLE = "command_example";

    public static MediaPlayer mMediaPlayer;
    public static MediaSessionCompat mMediaSessionCompat;
    public static boolean mIsPrepared;
    public static boolean mIsCompleted;
    final public static int id = 77;

    // for differentiate Pause source: manual or focus change
    private boolean isPausedByButton;

    boolean enDbgMsg = true;
//    boolean enDbgMsg = false;

    BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            // when phone jack is unplugged
            if (android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                if((BackgroundAudioService.mMediaPlayer != null) && BackgroundAudioService.mMediaPlayer.isPlaying() )
                {
                    if(enDbgMsg)
                        System.out.println("BackgroundAudioService / audioNoisyReceiver / _onReceive / play -> pause");
                    pauseAudio();
                }
            }

        }
    };

    // do audio play
    void playAudio(){
        mMediaSessionCompat.setActive(true);
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        initMediaSessionMetadata();
        showPlayingNotification();

        if(mMediaPlayer != null)
            mMediaPlayer.start();

        mMediaPlayer.setVolume(1.0f, 1.0f);

        // update panel status: play
        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);

        Audio_manager.setTogglePlayerState(true);

        isPausedByButton = false;
    }

    // do audio pause
    void pauseAudio() {
        if( mMediaPlayer != null  ) {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.pause();

            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);

            initMediaSessionMetadata();
            showPausedNotification();
        }

        // update panel status: pause
        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PAUSE);
        Audio_manager.setTogglePlayerState(true);
    }

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onSkipToNext");

            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
            mMediaSessionCompat.setActive(true);
            Audio_manager.setPlayNext(true);
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onSkipToPrevious");
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
            mMediaSessionCompat.setActive(true);
            Audio_manager.setPlayPrevious(true);
        }

        @Override
        public void onPlay() {
            super.onPlay();
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / mMediaSessionCallback / _onPlay");

            if( !successfullyRetrievedAudioFocus() ) {
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

            mIsPrepared = false;

            initMediaPlayer();

            setAudioPlayerListeners();

            try {
                mMediaPlayer.setDataSource(MainAct.mAct, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                mMediaPlayer.prepare();
                mIsPrepared = false;
                mIsCompleted = false;
            } catch (IllegalStateException | IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Set audio player listeners
         */
        void setAudioPlayerListeners()
        {
            if(enDbgMsg)
                System.out.println("BackgroundAudioService / _setAudioPlayerListeners");

            // on prepared
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    if(enDbgMsg)
                        System.out.println("BackgroundAudioService / _setAudioPlayerListeners / onPrepared");
                    if (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)
                    {
                        mMediaPlayer.seekTo(0);
                    }
                    mIsPrepared = true;
                }
            });

            // on completed
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if(enDbgMsg)
                        System.out.println("BackgroundAudioService / _setAudioPlayerListeners / onCompleted");

                    if(mMediaPlayer != null) {
                        mMediaPlayer.release();

                        // disconnect media browser
                        if( MainAct.mMediaControllerCompat.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ) {
                            MainAct.mMediaControllerCompat.getTransportControls().stop();// .pause();
                        }
                    }

                    mMediaPlayer = null;
                    mIsCompleted = true;
                }
            });

            // on error
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // more than one error when playing an index
                    if(enDbgMsg)
                        System.out.println("BackgroundAudioService / _setAudioPlayerListeners / _onError / what = " + what + " , extra = " + extra);
                    return false;
                }
            });

            // on buffering update
            BackgroundAudioService.mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    if(enDbgMsg)
                       System.out.println("BackgroundAudioService / _setAudioPlayerListeners / _onBufferingUpdate");
                    if (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE) {
                        if (TabsHost.getCurrentPage().seekBarProgress != null)
                            TabsHost.getCurrentPage().seekBarProgress.setSecondaryProgress(percent);
                    }
                }
            });
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
            if( COMMAND_EXAMPLE.equalsIgnoreCase(command) ) {
                //Custom command here
            }
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _onCreate");
//        initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(audioNoisyReceiver, filter);
    }

    @Override
    public void onDestroy() {
        System.out.println("BackgroundAudioService / _onDestroy");

        super.onDestroy();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(audioNoisyReceiver);
        mMediaSessionCompat.release();
        NotificationManagerCompat.from(this).cancel(id);
    }

    NotificationCompat.Builder builder;
    NotificationManagerCompat manager;
    String CHANNEL_ID = "77";
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

        manager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                                                                                    getResources().getString(R.string.app_name),
                                                                                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
    }

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
            manager.notify(id,builder.setChannelId(CHANNEL_ID).build());
        else
            manager.notify(id, builder.build());
    }

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
            manager.notify(id,builder.setChannelId(CHANNEL_ID).build());
        else
            manager.notify(id, builder.build());
    }


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
    }

    private void setMediaPlaybackState(int state) {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _setMediaPlaybackState / state = " + state);
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                            PlaybackStateCompat.ACTION_PAUSE |
                                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        }
        else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                            PlaybackStateCompat.ACTION_PLAY|
                                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        }

        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void initMediaSessionMetadata() {
        if(enDbgMsg)
            System.out.println("BackgroundAudioService / _initMediaSessionMetadata");

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        //Notification icon in card
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        String audioStr = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);
        String[] displayItems=Util.getDisplayNameByUriString(audioStr, MainAct.mAct);

        // prepare bit map
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try
        {
            mmr.setDataSource(MainAct.mAct,Uri.parse(audioStr));

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

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }
}