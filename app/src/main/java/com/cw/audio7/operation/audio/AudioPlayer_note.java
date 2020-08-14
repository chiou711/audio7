/*
 * Copyright (C) 2019 CW Chiu
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

package com.cw.audio7.operation.audio;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.note.AudioUi_note;
import com.cw.audio7.note.Note;
import com.cw.audio7.note.NoteUi;
import com.cw.audio7.util.Util;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

public class AudioPlayer_note
{
	private static final int DURATION_1S = 1000; // 1 seconds per slide
    private static Audio_manager mAudioManager; // slide show being played
	public static int mAudioPos; // index of current media to play
	private static int mPlaybackTime; // time in miniSeconds from which media should play
    private AppCompatActivity act;
    private Async_audioUrlVerify mAudioUrlVerifyTask;
    static Handler mAudioHandler; // used to update the slide show

    public AudioPlayer_note(AppCompatActivity act){
        this.act = act;

		// start a new handler
		mAudioHandler = new Handler();
    }

    /**
     * prepare audio info
     */
    public static void prepareAudioInfo()
    {
        mAudioManager = new Audio_manager();
        mAudioManager.updateAudioInfo();
    }

	/**
     *  Run audio state
     */
    public void runAudioState()
	{
	   	System.out.println("AudioPlayer_note / _runAudioState ");

	   	// if media player is not prepared, start new one
        if(!BackgroundAudioService.mIsPrepared)
		{
            mPlaybackTime = 0;
            if(!AudioUi_note.isPausedAtSeekerAnchor)
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);
            else
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PAUSE);//just slide the progress bar

            startNewAudio_note();
		}
		else
		{
			// from play to pause
			if(BackgroundAudioService.mMediaPlayer.isPlaying())
			{
				System.out.println("AudioPlayer_note / _runAudioState / play -> pause");
                BackgroundAudioService.mMediaPlayer.pause();
				mAudioHandler.removeCallbacks(mRunOneTimeMode);
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PAUSE);
			}
			else // from pause to play
			{
				System.out.println("AudioPlayer_note / _runAudioState / pause -> play");
                BackgroundAudioService.mMediaPlayer.start();

				if(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE)
					mAudioHandler.post(mRunOneTimeMode);

                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);
			}
		}
	}


    /**
     * One time mode runnable
     */
	private Runnable mRunOneTimeMode = new Runnable()
	{   @Override
		public void run()
		{
            if(!Audio_manager.isRunnableOn_note)
            {
                System.out.println("AudioPlayer_note / mRunOneTimeMode / Audio_manager.isRunnableOn_note = " + Audio_manager.isRunnableOn_note);
                stopHandler();
                stopAsyncTask();
                return;
            }

            if(!BackgroundAudioService.mIsPrepared)
	   		{
	   			if(Async_audioUrlVerify.mIsOkUrl)
	   			{
                    System.out.println("AudioPlayer_note / mRunOneTimeMode / Audio_manager.isRunnableOn_note = " + Audio_manager.isRunnableOn_note);

				    // add for calling runnable
				    if (Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE)
					    mAudioHandler.postDelayed(mRunOneTimeMode, Util.oneSecond / 4);

				    BackgroundAudioService.mIsPrepared = false;
	   			}
	   			else
	   			{
	   				Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();
   					Audio_manager.stopAudioPlayer();
	   			}
	   		}
	   		else//Audio_manager.mMediaPlayer != null
	   		{
	   			AudioUi_note.updateAudioProgress(act);
				mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S);
	   		}		    		
		} 
	};

	private void stopHandler()
    {
        if(mAudioHandler != null) {
            mAudioHandler.removeCallbacks(mRunOneTimeMode);
            mAudioHandler = null;
        }
    }

    private void stopAsyncTask()
    {
        // stop async task
        // make sure progress dialog will disappear
        if( (mAudioUrlVerifyTask!= null) &&
                (!mAudioUrlVerifyTask.isCancelled()) )
        {
            mAudioUrlVerifyTask.cancel(true);

            if( (mAudioUrlVerifyTask.mUrlVerifyDialog != null) &&
                    mAudioUrlVerifyTask.mUrlVerifyDialog.isShowing()	)
            {
                mAudioUrlVerifyTask.mUrlVerifyDialog.dismiss();
            }

            if( (mAudioUrlVerifyTask.mAsyncTaskAudioPrepare != null) &&
                    (mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog != null) &&
                    mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog.isShowing()	)
            {
                mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog.dismiss();
            }
        }

    }

    /**
     * Set audio player listeners
     */
	private void setMediaPlayerListeners()
	{
        // - on prepared listener
        BackgroundAudioService.mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
        {	@Override
            public void onPrepared(MediaPlayer mp)
            {
                System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onPrepared");

                if (Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE)
                {
                    if (BackgroundAudioService.mMediaPlayer != null)
                    {
                        BackgroundAudioService.mIsPrepared = true;
                        if (!AudioUi_note.isPausedAtSeekerAnchor)
                        {
                            BackgroundAudioService.mMediaPlayer.start();
                            BackgroundAudioService.mMediaPlayer.getDuration();
                            BackgroundAudioService.mMediaPlayer.seekTo(mPlaybackTime);
                        }
                        else
                            BackgroundAudioService.mMediaPlayer.seekTo(AudioUi_note.mAnchorPosition);

                        AudioUi_note.updateAudioPlayState(act);
                    }
                }
            }
        });

        // On Completion listener
        BackgroundAudioService.mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
        {	@Override
        public void onCompletion(MediaPlayer mp)
        {
            System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onCompletion");

            BackgroundAudioService.mIsPrepared = false;

            mPlaybackTime = 0;

            if(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE) // one time mode
            {
                    Audio_manager.stopAudioPlayer();
                    AudioUi_note.initAudioProgress(act, Note.mAudioUriInDB);
                    AudioUi_note.updateAudioPlayState(act);
            }
        }
        });

        // - on error listener
        BackgroundAudioService.mMediaPlayer.setOnErrorListener(new OnErrorListener()
        {	@Override
            public boolean onError(MediaPlayer mp,int what,int extra)
            {
                // more than one error when playing an index
                System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onError / what = " + what + " , extra = " + extra);
                return false;
            }
        });
	}


    /**
     * Start new audio
     */
	private void startNewAudio_note()
	{
		// remove call backs to make sure next toast will appear soon
		if(mAudioHandler != null)
			mAudioHandler.removeCallbacks(mRunOneTimeMode);
        mAudioHandler = null;
        mAudioHandler = new Handler();

        Audio_manager.isRunnableOn_page = false;
        Audio_manager.isRunnableOn_note = true;
        BackgroundAudioService.mMediaPlayer = null;

        // verify audio
        Async_audioUrlVerify.mIsOkUrl = false;

		mAudioUrlVerifyTask = new Async_audioUrlVerify(act, mAudioManager.getAudioStringAt(mAudioPos));
		mAudioUrlVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");

		while(!Async_audioUrlVerify.mIsOkUrl)
        {
            //wait for Url verification
            try {
                Thread.sleep(Util.oneSecond/20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // prepare audio
        if(Async_audioUrlVerify.mIsOkUrl)
        {
            // launch handler
            if(Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP)
            {
                if(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE) {
                    mAudioHandler.postDelayed(mRunOneTimeMode, Util.oneSecond / 4);
                }
            }

            // during audio Preparing
            Async_audioPrepare mAsyncTaskAudioPrepare = new Async_audioPrepare(act);
            mAsyncTaskAudioPrepare.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Preparing to play ...");

	        String audioStr = Audio_manager.getAudioStringAt(mAudioPos);
	        if (Build.VERSION.SDK_INT >= 21) {
		        MainAct.mMediaControllerCompat
				        .getTransportControls()
				        .playFromUri(Uri.parse(audioStr), null);//todo How to avoid null exception if not using recreate

		        MainAct.mMediaControllerCompat.getTransportControls().play();
	        } else {
		        BackgroundAudioService.mMediaPlayer = new MediaPlayer();
		        BackgroundAudioService.mMediaPlayer.reset();
		        try {
			        BackgroundAudioService.mMediaPlayer.setDataSource(act, Uri.parse(audioStr));

			        // prepare the MediaPlayer to play, this will delay system response
			        BackgroundAudioService.mMediaPlayer.prepare();
			        setMediaPlayerListeners();
		        } catch (Exception e) {
			        Toast.makeText(act, R.string.audio_message_could_not_open_file, Toast.LENGTH_SHORT).show();
			        Audio_manager.stopAudioPlayer();
		        }
	        }
        }

    }
	

    /**
     * Play next audio
     */
    private void playNextAudio()
    {
        System.out.println("AudioPlayer_note / _playNextAudio");
        Audio_manager.stopAudioPlayer();

        // new audio index
        mAudioPos++;

        if(mAudioPos >= NoteUi.getNotesCnt())
            mAudioPos = 0; //back to first index

        mPlaybackTime = 0;
        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);
        startNewAudio_note();
    }

}