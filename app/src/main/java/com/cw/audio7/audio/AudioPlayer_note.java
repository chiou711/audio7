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

package com.cw.audio7.audio;

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
import com.cw.audio7.util.Util;

import androidx.appcompat.app.AppCompatActivity;

public class AudioPlayer_note
{
	private static final int DURATION_1S = 1000; // 1 seconds per slide
    private static Audio_manager mAudioManager; // slide show being played
	public static int mAudioPos; // index of current media to play
	private static int mPlaybackTime; // time in miniSeconds from which media should play
    private AppCompatActivity act;
    private Async_audioUrlVerify mAudioUrlVerifyTask;
    static Handler mAudioHandler; // used to update the slide show
	AudioUi_note audioUi_note;

    public AudioPlayer_note(AppCompatActivity act, AudioUi_note ui){
        this.act = act;
	    audioUi_note = ui;

		// start a new handler
		mAudioHandler = new Handler();
    }

    /**
     * prepare audio info
     */
    public void prepareAudioInfo()
    {
    	if(mAudioManager == null)
		    mAudioManager = new Audio_manager();

    	if(mAudioManager != null)
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
				if(mAudioHandler != null)
					mAudioHandler.removeCallbacks(note_runnable);

				System.out.println("AudioPlayer_note / _runAudioState / play -> pause");
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PAUSE);

				//for pause
				if(Build.VERSION.SDK_INT >= 21)
					MainAct.mMediaControllerCompat.getTransportControls().pause();
				else
					BackgroundAudioService.mMediaPlayer.pause();
			}
			else // from pause to play
			{
				System.out.println("AudioPlayer_note / _runAudioState / pause -> play");

				if( (mAudioHandler != null) &&
						(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE))
					mAudioHandler.post(note_runnable);

                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);

				//for play
				if(Build.VERSION.SDK_INT >= 21)
					MainAct.mMediaControllerCompat.getTransportControls().play();
				else
					BackgroundAudioService.mMediaPlayer.start();
			}
		}
	}


    /**
     * One time mode runnable
     */
	private Runnable note_runnable = new Runnable()
	{   @Override
		public void run()
		{
//			System.out.println("AudioPlayer_note / note_runnable");
            if(!Audio_manager.isRunnableOn_note)
            {
                System.out.println("AudioPlayer_note / note_runnable / Audio_manager.isRunnableOn_note = " + Audio_manager.isRunnableOn_note);
                stopHandler();
                stopAsyncTask();
                return;
            }

            if(!BackgroundAudioService.mIsPrepared)
	   		{
	   			if(Async_audioUrlVerify.mIsOkUrl)
	   			{
                    System.out.println("AudioPlayer_note / note_runnable / Audio_manager.isRunnableOn_note = " + Audio_manager.isRunnableOn_note);

				    // call runnable
				    if (Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE)
					    mAudioHandler.postDelayed(note_runnable, Util.oneSecond / 4);

				    BackgroundAudioService.mIsPrepared = false;
	   			}
	   			else
	   			{
	   				Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();
   					Audio_manager.stopAudioPlayer();
	   			}
	   		}
	   		else // BackgroundAudioService.mIsPrepared = true
	   		{
			    if(BackgroundAudioService.mIsCompleted) {
				    Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
				    audioUi_note.updateAudioPanel_note(act);
				    BackgroundAudioService.mIsCompleted = false;
			    }

			    // media control: play previous / next
			    if(Audio_manager.isPlayPrevious() || Audio_manager.isPlayNext()) {
				    if(mAudioHandler != null)
					    mAudioHandler.removeCallbacks(note_runnable);
				    mAudioHandler = null;

				    if(Audio_manager.isPlayPrevious() ){
					    // play previous
					    audioUi_note.audio_previous_btn.performClick();
					    Audio_manager.setPlayPrevious(false);
				    } else if(Audio_manager.isPlayNext()) {
				    	// play next
					    audioUi_note.audio_next_btn.performClick();
					    Audio_manager.setPlayNext(false);
				    }
			    }
			   else {
				    // toggle play / pause
				    if(Audio_manager.isTogglePlayerState()) {
					    audioUi_note.updateAudioPanel_note(act);
					    Audio_manager.setTogglePlayerState(false);
				    }

				    audioUi_note.updateAudioProgress();
				    mAudioHandler.postDelayed(note_runnable, DURATION_1S); // loop for poling media control
			    }
	   		}
		}
	};

	private void stopHandler()
    {
        if(mAudioHandler != null) {
            mAudioHandler.removeCallbacks(note_runnable);
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

	                    audioUi_note.updateAudioPanel_note(act);
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

            if(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE)
            {
                    Audio_manager.stopAudioPlayer();
	                audioUi_note.updateAudioPanel_note(act);
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
		System.out.println("AudioPlayer_note / _startNewAudio_note");
		// remove call backs to make sure next toast will appear soon
		if(mAudioHandler != null)
			mAudioHandler.removeCallbacks(note_runnable);

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
                if(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE)
                    mAudioHandler.postDelayed(note_runnable, Util.oneSecond / 4); // 1st time
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

}