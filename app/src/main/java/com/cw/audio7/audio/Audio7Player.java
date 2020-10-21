/*
 * Copyright (C) 2020 CW Chiu
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
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Audio7Player
{
	private static final String TAG = "AUDIO_PLAYER"; // error logging tag
	private static final int DURATION_1S = 1000; // 1 seconds per slide
    private static Audio_manager mAudioManager; // slide show being played
	private static int mAudio_tryTimes; // use to avoid useless looping in Continue mode
    static private AppCompatActivity act;
    static private Async_audioUrlVerify mAudioUrlVerifyTask;
	public ViewGroup audio_panel;
    public static Handler mAudioHandler;
    static private int notesCount;

	public Audio7Player(AppCompatActivity _act, ViewGroup audio_panel, String audio_uri_str){
		System.out.println("Audio7Player / constructor ");

		act = _act;
		this.audio_panel = audio_panel;

		initAudioBlock(audio_uri_str);

		// start a new handler
        mAudioHandler = new Handler();
	}

    /**
     * prepare audio info
     */
    public static void prepareAudioInfo()
    {
	    // check for continuing audio play when Note audio UI is change Page audio UI
    	if(Audio_manager.mPausedPosition == 0)
            mAudioManager = new Audio_manager();
        mAudioManager.updateAudioInfo();
    }

	/**
     *  Run audio state
     */
    public void runAudioState()
	{
	   	System.out.println("Audio7Player / _runAudioState ");

	   	if(audio_panel != null)
			audio_panel.setVisibility(View.VISIBLE);
	   	else
		    System.out.println("Audio7Player / _runAudioState / audio_panel = null ");

	   	// if media player is null, set new fragment
		if( BackgroundAudioService.mMediaPlayer == null )	//for first
		{
		 	// show toast if Audio file is not found or No selection of audio file
			if( (Audio_manager.getAudioFilesCount() == 0) /*&&
				(Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)*/        )
			{
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
				Toast.makeText(act,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
			}
			else
			{
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);
				mAudio_tryTimes = 0;

				//for 1st play
				audioUrl = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);
				while (!UtilAudio.hasAudioExtension(audioUrl) &&
						   !audioUrl.contains("google"))
				{
                    Audio_manager.mAudioPos++;
                    audioUrl = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);

                    if(Audio_manager.mAudioPos >= TabsHost.getCurrentPage().getNotesCountInPage(MainAct.mAct))
                        break;
				}

				if( (UtilAudio.hasAudioExtension(audioUrl) && Util.isUriExisted(audioUrl,MainAct.mAct)) ||
						audioUrl.contains("google")) {
                    startNewAudio();
                }
                else
                {
                    Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
	                Toast.makeText(act,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
                }
			}
		}
		else
		{
			// from play to pause
			if(BackgroundAudioService.mMediaPlayer.isPlaying())
			{
				System.out.println("Audio7Player / _runAudioState / play -> pause");
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PAUSE);

                //for pause
                if(Build.VERSION.SDK_INT >= 21)
	                MainAct.mMediaControllerCompat.getTransportControls().pause();
                else
                    BackgroundAudioService.mMediaPlayer.pause();
			}
			else // from pause to play
			{
				System.out.println("Audio7Player / _runAudioState / pause -> play");
				mAudio_tryTimes = 0;

                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);

                //for play
                if(Build.VERSION.SDK_INT >= 21)
	                MainAct.mMediaControllerCompat.getTransportControls().play();
                else
                    BackgroundAudioService.mMediaPlayer.start();
			}
		}

		updateAudioPanel(act);
	}

    /**
     * Set audio listeners
     */
	static private void setAudioListeners()
    {
        // on prepared
        BackgroundAudioService.mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                System.out.println("Audio7Player / _setAudioListeners / onPrepared");
                BackgroundAudioService.mIsPrepared = true;
                BackgroundAudioService.mMediaPlayer.start();
                BackgroundAudioService.mMediaPlayer.seekTo(0);
            }
        });

        // on completed
        BackgroundAudioService.mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                System.out.println("Audio7Player / _setAudioListeners / onCompletion");
                if(BackgroundAudioService.mMediaPlayer != null)
                    BackgroundAudioService.mMediaPlayer.release();

                BackgroundAudioService.mMediaPlayer = null;
                BackgroundAudioService.mIsCompleted = true;
            }
        });

        // on error
        BackgroundAudioService.mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // more than one error when playing an index
                System.out.println("Audio7Player / _setAudioListeners / _onError / what = " + what + " , extra = " + extra);
                return false;
            }
        });

        // on buffering update
        BackgroundAudioService.mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                System.out.println("-- Audio7Player / _setAudioListeners / _onBufferingUpdate / percent = " + percent);
                //todo For which condition?
                if (TabsHost.getCurrentPage().seekBarProgress != null)
                    TabsHost.getCurrentPage().seekBarProgress.setSecondaryProgress(percent);
            }
        });
    }

	// set list view footer audio control
	private void showAudioPanel(AppCompatActivity act,boolean enable)
	{
		System.out.println("Audio7Player / _showAudioPanel / enable = " + enable);
        if(audio_panel != null) {
            TextView audio_panel_title_textView = (TextView) audio_panel.findViewById(R.id.audio_title);
            SeekBar seekBarProgress = (SeekBar) audio_panel.findViewById(R.id.seek_bar);

            // show audio panel
            if (enable) {
	            audio_panel.setVisibility(View.VISIBLE);
                if(audio_panel_title_textView != null)
                    audio_panel_title_textView.setVisibility(View.VISIBLE);

                // set footer message with audio name
                String audioStr = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);
                String[] audioName = Util.getDisplayNameByUriString(audioStr, act);
                if(audio_panel_title_textView != null)
                    audio_panel_title_textView.setText(audioName[0] .concat(" / ").concat(audioName[1]));

                // show audio playing item number
                TextView audioPanel_audio_number = (TextView) audio_panel.findViewById(R.id.audioPanel_audio_number);
                String message = act.getResources().getString(R.string.menu_button_play) +
                        "#" +
                        (Audio_manager.mAudioPos +1);
                if(audioPanel_audio_number !=  null)
                    audioPanel_audio_number.setText(message);

                seekBarProgress.setVisibility(View.VISIBLE);
            } else {
	            audio_panel.setVisibility(View.GONE);
            }
        }
	}

	public static boolean isAudioPanelOn(AppCompatActivity act)
    {
        View audio_panel = act.findViewById(R.id.audio_panel);
        boolean isOn = false;
        if(audio_panel != null)
            isOn = (audio_panel.getVisibility() == View.VISIBLE);
        return isOn;
    }

    /**
     * Continue mode runnable
     */
	static private String audioUrl;
	public Runnable audio_runnable = new Runnable() {
		@Override
		public void run() {
//			System.out.println("Audio7Player / _audio_runnable");
			if( !Audio_manager.isRunnableOn)
			{
				stopHandler();
				stopAsyncTask();

				if( (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE) &&
					(!Pref.getPref_cyclic_play_enable(act))   ) {
					Audio_manager.stopAudioPlayer();
					showAudioPanel(act, false);
				}

				System.out.println("Audio7Player / _audio_runnable / return");
				return;
			}

			if (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 1) {

				// for incoming call case and after Key protection
				if (  (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE) &&
						!isAudioPanelOn(act) ) {
					showAudioPanel(act, true);//todo Why not stop?
				}

				// check if audio file exists or not
				audioUrl = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);

				if (!Async_audioUrlVerify.mIsOkUrl) {
					mAudio_tryTimes++;
					play_nextAudio();
					return;
				} else {
					if (BackgroundAudioService.mIsPrepared) {

						// set footer message: media name
						if (!Util.isEmptyString(audioUrl)) {

							// set seek bar progress
							TextView audioPanel_file_length = (TextView) act.findViewById(R.id.audioPanel_file_length);
							if (audioPanel_file_length != null)
								audioPanel_file_length.setText(UtilAudio.getAudioLengthString(act, audioUrl));
						}

						BackgroundAudioService.mIsPrepared = false;
					}

					if (BackgroundAudioService.mIsCompleted) {
//						System.out.println("Audio7Player / _audio_runnable /  BackgroundAudioService.mIsCompleted");
						Audio_manager.setPlayNext(true);
						BackgroundAudioService.mIsCompleted = false;
					}
				}

				if (mAudio_tryTimes < Audio_manager.getAudioFilesCount()) {
					if( Audio_manager.isPlayPrevious() ||
						Audio_manager.isPlayNext()               )
					{
						if(mAudioHandler != null)
							mAudioHandler.removeCallbacks(audio_runnable);
						mAudioHandler = null;

						// play previous
						if (Audio_manager.isPlayPrevious()) {
							audio_previous_btn.performClick();
							Audio_manager.setPlayPrevious(false);
						}

						// play next
						if (Audio_manager.isPlayNext() ) {
							audio_next_btn.performClick();
							Audio_manager.setPlayNext(false);
						}
					} else {
						/** update page audio progress */
						updateAudioProgress();

						// toggle play / pause
						if(Audio_manager.isTogglePlayerState()) {

							/** update audio panel when media controller */
							updateAudioPanel(MainAct.mAct);

							// for page audio gif
							if(  (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE) &&
								 (TabsHost.getCurrentPage().itemAdapter != null)  ) {
								TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
							}

							Audio_manager.setTogglePlayerState(false);
						}

						if (mAudio_tryTimes == 0) {
							mAudioHandler.postDelayed(audio_runnable, DURATION_1S);
						} else
							mAudioHandler.postDelayed(audio_runnable, DURATION_1S / 10);
					}
				}
			}
			else if( (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0 ) )// for non-audio item
			{
//	   			System.out.println("Audio7Player / audio_runnable / for non-audio item");

				if(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE) {
					Audio_manager.stopAudioPlayer();

					// case 1: play next
//					audio_next_btn.performClick();

					// case 2: show unchecked
					Toast.makeText(act,R.string.is_an_unchecked_item,Toast.LENGTH_SHORT).show();
					updateAudioPanel(act);
					updateAudioProgress();
				}

				if (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE) {
					play_nextAudio();

					if(isOnAudioPlayingPage()) {
						scrollPlayingItemToBeVisible(TabsHost.getCurrentPage().recyclerView);
						TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
					}
				}
			}
		}
	};

	private void stopHandler()
    {
        if(mAudioHandler != null) {
            mAudioHandler.removeCallbacks(audio_runnable);
            mAudioHandler = null;
        }
    }

    static private void stopAsyncTask()
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

    // check if is on audio playing page
    public static boolean isOnAudioPlayingPage()
    {
        return ( (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) &&
                     (MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()) &&
		             (TabsHost.getFocus_tabPos() == MainAct.mPlaying_pagePos)     &&
                     (TabsHost.getCurrentPage().recyclerView != null)                     );
    }

//    public static int media_file_length;

	/**
	* Scroll playing item to be visible
	*
	* At the following conditions
	* 	1) click audio item of list view
	* 	2) click previous/next item in audio controller
	* 	3) change tab to playing tab
	* 	4) toggle play/pause button
	* 	5) if seeker bar reaches the end
	* In order to view audio highlight item, playing(highlighted) audio item can be auto scrolled to top,
	* unless it is at the end page of list view, there is no need to scroll.
	*/
	static public void scrollPlayingItemToBeVisible(RecyclerView recyclerView)
	{
		System.out.println("Audio7Player / _scrollPlayingItemToBeVisible");
        if ( (recyclerView == null) ||
		     (recyclerView.getAdapter() == null) ||
		     (Build.VERSION.SDK_INT < 19)            )
            return;

        boolean showDebugMsg = false;

		LinearLayoutManager layoutMgr = ((LinearLayoutManager) recyclerView.getLayoutManager());
		if(layoutMgr == null)
			return;

		int first_note_pos;
		int itemHeight = 50;
		int divider_size = 1; // todo check divider.xml size element
		int dividerHeight;

		first_note_pos = layoutMgr.findFirstCompletelyVisibleItemPosition();

		if(showDebugMsg)
			System.out.println("---------------- first_note_pos = " + first_note_pos);

		// no complete visible position, do offset
		if(first_note_pos == RecyclerView.NO_POSITION)
		{
            int top_offset = 0;
            // check if child at 0 is null for changing to high card view to avoid exception
            if( recyclerView.getChildAt(0) != null)
            	top_offset = recyclerView.getChildAt(0).getTop();

			if(showDebugMsg)
	            System.out.println("---------------- top_offset 1 = " + top_offset);

            if(top_offset < 0)
                recyclerView.scrollBy(0,top_offset);

            first_note_pos = layoutMgr.findFirstCompletelyVisibleItemPosition();
        }

		// https://stackoverflow.com/questions/6157652/android-getmeasuredheight-returns-wrong-values
		// item height
		View childView = layoutMgr.findViewByPosition(first_note_pos);
        if(childView != null) {
            ViewUtil.measure(childView);
            itemHeight = childView.getMeasuredHeight();
        }

		if(showDebugMsg)
			System.out.println("---------------- itemHeight = " + itemHeight);

        // divider height
		float scale = act.getResources().getDisplayMetrics().density;
		dividerHeight =  (int)(divider_size * scale + 0.0f);

		if(showDebugMsg)
			System.out.println("---------------- dividerHeight = " + dividerHeight);

		int offset = itemHeight + dividerHeight;

		// base on Audio_manager.mAudioPos to scroll
		if(showDebugMsg)
			System.out.println("----- Audio_manager.mAudioPos = " + Audio_manager.mAudioPos);

		while ((first_note_pos != Audio_manager.mAudioPos) )
		{
			int startPos = first_note_pos;
			// scroll forwards
			if (first_note_pos > Audio_manager.mAudioPos )
			{
                recyclerView.scrollBy(0,-offset);
				if(showDebugMsg)
					System.out.println("----- highlight item No. (-1), offset = " + (-offset));
			}
			// scroll backwards
			else
			{
				// when real item height could be larger than visible item height, so
				// scroll twice here in odder to do scroll successfully, otherwise scroll could fail
				recyclerView.scrollBy(0,offset);
				if(showDebugMsg)
					System.out.println("----- highlight item No. (+1), offset =  " + offset);
			}

			first_note_pos = layoutMgr.findFirstCompletelyVisibleItemPosition();

			// check if recycler view reached the end
			if(first_note_pos == startPos)
				first_note_pos = Audio_manager.mAudioPos;

			// no complete visible position, do offset
			if(first_note_pos == RecyclerView.NO_POSITION) {
				int top_offset = recyclerView.getChildAt(0).getTop();

				if(showDebugMsg)
					System.out.println("---------------- top_offset 2 = " + top_offset);

				if(top_offset < 0)
					// restore index and top position
					recyclerView.scrollBy(0,top_offset);
			}
		}

		// do v scroll
		TabsHost.store_listView_vScroll(recyclerView);
		TabsHost.resume_listView_vScroll(recyclerView);
	}

    /**
     * Start new audio
     */
    private void startNewAudio()
    {
        System.out.println("Audio7Player / _startNewAudio / Audio_manager.mAudioPos = " + Audio_manager.mAudioPos);

        // remove call backs to make sure next toast will appear soon
        if(mAudioHandler != null)
            mAudioHandler.removeCallbacks(audio_runnable);
        mAudioHandler = null;
        mAudioHandler = new Handler();

        Audio_manager.isRunnableOn = true;
        BackgroundAudioService.mMediaPlayer = null;

        // verify audio URL
        Async_audioUrlVerify.mIsOkUrl = false;

	    if( /*(Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE) &&*/
			(Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0)          )
	    {
		    mAudioHandler.postDelayed(audio_runnable,Util.oneSecond/4);
	    }
	    else {
		    mAudioUrlVerifyTask = new Async_audioUrlVerify(act, mAudioManager.getAudioStringAt(Audio_manager.mAudioPos));
		    mAudioUrlVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");

		    while (!Async_audioUrlVerify.mIsOkUrl) {
			    //wait for Url verification
			    try {
				    Thread.sleep(Util.oneSecond / 20);
			    } catch (InterruptedException e) {
				    e.printStackTrace();
			    }
		    }

		    // prepare audio
		    if (Async_audioUrlVerify.mIsOkUrl) {

			    if (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)
			        showAudioPanel(act, true);

			    initAudioBlock(mAudioManager.getAudioStringAt(Audio_manager.mAudioPos));

			    // launch handler
			    if ( Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP ) {
				    mAudioHandler.postDelayed(audio_runnable, Util.oneSecond / 4);
				    System.out.println("Audio7Player / _startNewAudio / 1st post page_runnable");
			    }

			    // during audio Preparing
			    Async_audioPrepare mAsyncTaskAudioPrepare = new Async_audioPrepare(act);
			    mAsyncTaskAudioPrepare.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Preparing to play ...");

			    if (Build.VERSION.SDK_INT >= 21) {
				    MainAct.mMediaControllerCompat
						    .getTransportControls()
						    .playFromUri(Uri.parse(audioUrl), null);//todo How to avoid null exception if not using recreate

				    MainAct.mMediaControllerCompat.getTransportControls().play();
			    } else {
				    BackgroundAudioService.mMediaPlayer = new MediaPlayer();
				    BackgroundAudioService.mMediaPlayer.reset();
				    try {
					    BackgroundAudioService.mMediaPlayer.setDataSource(act, Uri.parse(audioUrl));

					    // prepare the MediaPlayer to play, this will delay system response
					    BackgroundAudioService.mMediaPlayer.prepare();
					    setAudioListeners();
				    } catch (Exception e) {
					    Toast.makeText(act, R.string.audio_message_could_not_open_file, Toast.LENGTH_SHORT).show();
					    Audio_manager.stopAudioPlayer();
				    }
			    }
		    }//if(Async_audioUrlVerify.mIsOkUrl)
	    }
    }

    /**
     * Play next audio at AudioPlayer
     */
    private void play_nextAudio()
    {
//		Toast.makeText(act,"Can not open file, try next one.",Toast.LENGTH_SHORT).show();
        System.out.println("Audio7Player / _playNextAudio");
        if(BackgroundAudioService.mMediaPlayer != null)
        {
            BackgroundAudioService.mMediaPlayer.release();
            BackgroundAudioService.mMediaPlayer = null;
        }

        // new audio index
        Audio_manager.mAudioPos++;

        // check try times,had tried or not tried yet, anyway the audio file is found
	    System.out.println("Audio7Player / check mTryTimes = " + mAudio_tryTimes);
	    if(mAudio_tryTimes < Audio_manager.getAudioFilesCount() )
	    {
		    audioUrl = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);

		    if(UtilAudio.hasAudioExtension(audioUrl) && Util.isUriExisted(audioUrl,MainAct.mAct))
			    startNewAudio();
	    }
	    else // try enough times: still no audio file is found
	    {
		    Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();

		    // do not show highlight
		    if(MainAct.mSubMenuItemAudio != null)
			    MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);

		    // stop media player
		    Audio_manager.stopAudioPlayer();
	    }
        System.out.println("Audio7Player / _playNextAudio / Audio_manager.mAudioPos = " + Audio_manager.mAudioPos);
    }

	public TextView audio_title;
	public TextView audio_artist;
	public TextView audio_curr_pos;
	public SeekBar audio_seek_bar;
	public TextView audio_length;
	public TextView audio_number; //todo
	public ImageView audio_previous_btn;
	public ImageView audio_play_btn;
	public ImageView audio_next_btn;

	private static int mProgress;
	private static int mediaFileLength; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class

	public static int getMediaFileLength() {
		return mediaFileLength;
	}

	public static void setMediaFileLength(int mediaFileLength) {
		Audio7Player.mediaFileLength = mediaFileLength;
	}

	// update audio progress
	public void updateAudioProgress()
	{
		int currentPos=0;

		if(BackgroundAudioService.mMediaPlayer != null)
			currentPos = BackgroundAudioService.mMediaPlayer.getCurrentPosition();

		int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
		int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
		int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
		String curr_time_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
				String.format(Locale.ENGLISH,"%02d", curMin)+":" +
				String.format(Locale.ENGLISH,"%02d", curSec);

//		System.out.println("Audio7Player / _updateAudioProgress / curr_time_str = " + curr_time_str);

		// set current play time and the play length of audio file
		if(audio_curr_pos != null) {
			audio_curr_pos.setText(curr_time_str);
		}

//		System.out.println("Audio7Player / _updateAudioProgress / mediaFileLength = " + mediaFileLength);
		mProgress = (int)(((float)currentPos/ getMediaFileLength())*100);

		if(audio_seek_bar != null)
			audio_seek_bar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
	}


	// initialize audio block
	public void initAudioBlock(String audio_uriStr)
	{
		System.out.println("Audio7Player / _initAudioBlock " );
		audio_panel.setBackgroundColor(ColorSet.color_black);

		audio_title = (TextView) audio_panel.findViewById(R.id.audio_title); // first setting
		audio_artist = (TextView) audio_panel.findViewById(R.id.audio_artist);

		audio_title.setTextColor(ColorSet.color_white);
		if (Util.isLandscapeOrientation(act))
		{
			audio_title.setMovementMethod(new ScrollingMovementMethod());
			audio_title.scrollTo(0,0);
		}
		else
		{
			audio_title.setSingleLine(true);
			audio_title.setSelected(true);
		}

		audio_title.setTextColor(ColorSet.getPauseColor(act));
		showAudioName(act,audio_uriStr);
		audio_title.setSelected(false);

		// audio progress
		audio_curr_pos = (TextView) audio_panel.findViewById(R.id.audioPanel_current_pos);

		// current position
		mProgress = 0;
		int curHour = Math.round((float)(mProgress / 1000 / 60 / 60));
		int curMin = Math.round((float)((mProgress - curHour * 60 * 60 * 1000) / 1000 / 60));
		int curSec = Math.round((float)((mProgress - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
		String curr_pos_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
				String.format(Locale.ENGLISH,"%02d", curMin)+":" +
				String.format(Locale.ENGLISH,"%02d", curSec);

		audio_curr_pos.setText(curr_pos_str);
		audio_curr_pos.setTextColor(ColorSet.color_white);

		// audio seek bar
		audio_seek_bar = (SeekBar) audio_panel.findViewById(R.id.seek_bar);
		audio_length = (TextView) audio_panel.findViewById(R.id.audioPanel_file_length);

		audio_seek_bar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
		audio_seek_bar.setMax(99); // It means 100% .0-99
		audio_seek_bar.setVisibility(View.VISIBLE);

		// audio length
		try
		{
			if(Util.isUriExisted(audio_uriStr, act)) {
				MediaPlayer mp = MediaPlayer.create(act, Uri.parse(audio_uriStr));
				setMediaFileLength(mp.getDuration());
				mp.release();
			}
		}
		catch(Exception e) {
			System.out.println("Audio7Player / _initAudioBlock / exception");
		}

		// set audio file length
		int fileHour = Math.round((float)(getMediaFileLength() / 1000 / 60 / 60));
		int fileMin = Math.round((float)((getMediaFileLength() - fileHour * 60 * 60 * 1000) / 1000 / 60));
		int fileSec = Math.round((float)((getMediaFileLength() - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));

		String strHour = String.format(Locale.ENGLISH,"%2d", fileHour);
		String strMinute = String.format(Locale.ENGLISH,"%02d", fileMin);
		String strSecond = String.format(Locale.ENGLISH,"%02d", fileSec);
		String strLength = strHour + ":" + strMinute+ ":" + strSecond;

		audio_length.setText(strLength);
		audio_length.setTextColor(ColorSet.color_white);

		// audio buttons
		audio_previous_btn = (ImageView) audio_panel.findViewById(R.id.audioPanel_previous);
		audio_previous_btn.setImageResource(R.drawable.ic_media_previous);

		audio_play_btn = (ImageView) audio_panel.findViewById(R.id.audioPanel_play);
		audio_play_btn.setImageResource(R.drawable.ic_media_play);

		audio_next_btn = (ImageView) audio_panel.findViewById(R.id.audioPanel_next);
		audio_next_btn.setImageResource(R.drawable.ic_media_next);
	}

	// show audio name //todo Duplicate with AudioUi_note
	void showAudioName(AppCompatActivity act,String audio_uriStr)
	{
		System.out.println("Audio7Player / _showAudioName / audio_uriStr = " + audio_uriStr);
		// title: set marquee
		if(Util.isUriExisted(audio_uriStr, act)) {
			String[] audio_name = Util.getDisplayNameByUriString(audio_uriStr, act);
			audio_title.setText(audio_name[0] );

			if(audio_artist != null)
				audio_artist.setText(audio_name[1]);
		}
		else {
			audio_title.setText("N/A");
			audio_artist.setText("");
		}

		audio_title.setSelected(false);
		if(audio_artist != null)
			audio_artist.setSelected(false);
	}

	/**
	 *  update audio panel
	 * */
	public void updateAudioPanel(AppCompatActivity act)
	{
//		System.out.println("Audio7Player / _updateAudioPanel");

		// update playing state
		if(Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PLAY)
		{
			System.out.println("Audio7Player / _updateAudioPanel / at play");
			audio_play_btn.setImageResource(R.drawable.ic_media_pause);
			showAudioName(act, Audio_manager.getAudioStringAt(Audio_manager.mAudioPos));
			audio_title.setTextColor(ColorSet.getHighlightColor(act) );
			audio_title.setSelected(true);

			if(audio_artist != null) {
				audio_artist.setTextColor(ColorSet.getHighlightColor(act));
				audio_artist.setSelected(true);
			}
		}
		else if(Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_PLAY)
		{
			System.out.println("Audio7Player / _updateAudioPanel / not at play");
			audio_play_btn.setImageResource(R.drawable.ic_media_play);
			showAudioName(act, Audio_manager.getAudioStringAt(Audio_manager.mAudioPos));
			audio_title.setTextColor(ColorSet.getPauseColor(act));
			audio_title.setSelected(false);

			if(audio_artist != null) {
				audio_artist.setTextColor(ColorSet.getPauseColor(act));
				audio_artist.setSelected(false);
			}
		}
	}

}