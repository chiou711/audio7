/*
 * Copyright (C) 2021 CW Chiu
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

import android.app.ProgressDialog;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.system.SystemState;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.cw.audio7.define.Define.ENABLE_MEDIA_CONTROLLER;
import static com.cw.audio7.audio.BackgroundAudioService.audio_manager;

public class Audio7Player
{
	final private AppCompatActivity act;
	private View audio_panel;
	final int DURATION_1S = 1000; // 1 seconds per slide
	int audio_tryTimes; // use to avoid useless looping in Continue mode
    int delayBeforeMediaStart = DURATION_1S;
	String audioUrl;
	TabsHost tabsHost;

	public Audio7Player(AppCompatActivity _act, TabsHost _tabsHost, View audio_panel, String audio_uri_str){
		System.out.println("Audio7Player / constructor ");

		act = _act;
		tabsHost = _tabsHost;
		this.audio_panel = audio_panel;
		initAudioBlock(audio_uri_str);
	}

	public void setAudioPanel(View audio_panel) {
		this.audio_panel = audio_panel;
	}

	public View getAudioPanel() {
		return audio_panel;
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

	   	// if media player is null
		if( BackgroundAudioService.mMediaPlayer == null )	//for first
		{
		 	// show toast if Audio file is not found or No selection of audio file
			if( audio_manager.getAudioFilesCount() == 0) {
                audio_manager.setPlayerState(audio_manager.PLAYER_AT_STOP);
				Toast.makeText(act,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
			} else {
                audio_manager.setPlayerState(audio_manager.PLAYER_AT_PLAY);
				audio_tryTimes = 0;

				//for 1st play
				audioUrl = audio_manager.getAudioStringAt(audio_manager.mAudioPos);
				while (!UtilAudio.hasAudioExtension(audioUrl) &&
						   !audioUrl.contains("google")) {
                    audio_manager.mAudioPos++;
                    audioUrl = audio_manager.getAudioStringAt(audio_manager.mAudioPos);

                    if(audio_manager.mAudioPos >= tabsHost.getCurrentPage().getNotesCountInPage())
                        break;
				}

				if( (UtilAudio.hasAudioExtension(audioUrl) && Util.isUriExisted(audioUrl,act)) ||
						audioUrl.contains("google")) {
                    startNewAudio();
                } else {
                    audio_manager.setPlayerState(audio_manager.PLAYER_AT_STOP);
	                Toast.makeText(act,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
                }
			}
		} else {
			// from play to pause
			if(BackgroundAudioService.mMediaPlayer.isPlaying()) {
				System.out.println("Audio7Player / _runAudioState / play -> pause");
                audio_manager.setPlayerState(audio_manager.PLAYER_AT_PAUSE);

                //for pause
                if(ENABLE_MEDIA_CONTROLLER && Build.VERSION.SDK_INT >= 21)
	                MainAct.mMediaControllerCompat.getTransportControls().pause();
                else
                    BackgroundAudioService.mMediaPlayer.pause();

			} else { // from pause to play
				System.out.println("Audio7Player / _runAudioState / pause -> play");
				audio_tryTimes = 0;

                audio_manager.setPlayerState(audio_manager.PLAYER_AT_PLAY);

                //for play
                if(ENABLE_MEDIA_CONTROLLER && Build.VERSION.SDK_INT >= 21)
	                MainAct.mMediaControllerCompat.getTransportControls().play();
                else
                    BackgroundAudioService.mMediaPlayer.start();
			}
		}

		updateAudioPanel(act);
	}

	// set list view footer audio control
	public void showAudioPanel( boolean enable)
	{
//		System.out.println("Audio7Player / _showAudioPanel / enable = " + enable);
        if(audio_panel != null) {
            TextView audio_panel_title_textView = (TextView) audio_panel.findViewById(R.id.audio_title);
            SeekBar seekBarProgress = (SeekBar) audio_panel.findViewById(R.id.seek_bar);

            // show audio panel
            if (enable) {
	            audio_panel.setVisibility(View.VISIBLE);
                if(audio_panel_title_textView != null)
                    audio_panel_title_textView.setVisibility(View.VISIBLE);

                // show audio playing item number
                TextView audioPanel_audio_number = (TextView) audio_panel.findViewById(R.id.audioPanel_audio_number);
                String message = act.getResources().getString(R.string.menu_button_play) +
                        "#" +
                        (audio_manager.mAudioPos +1);
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

//        System.out.println("Audio7Player / _isAudioPanelOn =  " +isOn );
        return isOn;
    }


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
	public void scrollPlayingItemToBeVisible(RecyclerView recyclerView)
	{
		System.out.println("Audio7Player / _scrollPlayingItemToBeVisible");

		// return if screen off
		if(SystemState.isScreenOff(act))
			return;

        if ( (recyclerView == null) ||
		     (recyclerView.getAdapter() == null) ||
		     (Build.VERSION.SDK_INT < 19)            )
            return;

        boolean showDebugMsg = false;//true;//false;

		LinearLayoutManager layoutMgr = ((LinearLayoutManager) recyclerView.getLayoutManager());
		if(layoutMgr == null)
			return;

		int first_note_pos;
		int last_note_pos;
		int itemHeight = 50;
		int divider_size = 1; // todo check divider.xml size element
		int dividerHeight;

		first_note_pos = layoutMgr.findFirstCompletelyVisibleItemPosition();
		last_note_pos = layoutMgr.findLastCompletelyVisibleItemPosition();

		if(showDebugMsg) {
			System.out.println("---------------- first_note_pos = " + first_note_pos);
			System.out.println("---------------- last_note_pos = " + last_note_pos);
		}

		if( ( (audio_manager.mAudioPos >= first_note_pos ) &&
			  (audio_manager.mAudioPos <= last_note_pos )      ) ||
		    ( (first_note_pos ==  RecyclerView.NO_POSITION) &&
			  (last_note_pos == RecyclerView.NO_POSITION)    ) ) {
			audio_manager.doScroll = false;
			if(showDebugMsg)
				System.out.println("---------------- return and audio_manager.doScroll = false");
			return;
		}


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
			last_note_pos = layoutMgr.findLastCompletelyVisibleItemPosition();
			if(showDebugMsg) {
				System.out.println("---------------- first_note_pos (not complete) = " + first_note_pos);
				System.out.println("---------------- last_note_pos (not complete) = " + last_note_pos);
			}
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
			System.out.println("----- audio_manager.mAudioPos = " + audio_manager.mAudioPos);

		while ( (audio_manager.mAudioPos < first_note_pos) ||
				    (audio_manager.mAudioPos > last_note_pos)    )
		{
			// scroll forwards
			if (first_note_pos > audio_manager.mAudioPos )
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
			last_note_pos = layoutMgr.findLastCompletelyVisibleItemPosition();
			if(showDebugMsg) {
				System.out.println("---------------- new first_note_pos (after offset) = " + first_note_pos);
				System.out.println("---------------- last_note_pos (after offset) = " + last_note_pos);
			}

			// check if recycler view reached the end
			if( (audio_manager.mAudioPos >= first_note_pos) &&
				(audio_manager.mAudioPos <= last_note_pos)   ) {
				audio_manager.doScroll = false;
				break;
			}

			if(showDebugMsg)
				System.out.println("---------------- new first_note_pos (after check) = " + first_note_pos);

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
		if(tabsHost != null) {
			tabsHost.store_listView_vScroll(recyclerView);
			tabsHost.resume_listView_vScroll(recyclerView);
		}
	}

    /**
     * Start new audio
     */
    private void startNewAudio()
    {
        System.out.println("Audio7Player / _startNewAudio / audio_manager.mAudioPos = " + audio_manager.mAudioPos);

        // remove call backs to make sure next toast will appear soon
        if(audio_manager.audioHandler != null)
	        audio_manager.audioHandler.removeCallbacks(audio_manager.audio_runnable);

        audio_manager.audioHandler = null;
	    audio_manager.audioHandler = new Handler();

        BackgroundAudioService.mMediaPlayer = null;

        // verify audio URL
        Async_audioUrlVerify.mIsOkUrl = false;

	    if(audio_manager.getCheckedAudio(audio_manager.mAudioPos) == 0) {
		    audio_manager.audioHandler.postDelayed(audio_manager.audio_runnable,Util.oneSecond/4);
	    }
	    else {
	    	ProgressDialog dlg = new ProgressDialog(act);
			Async_audioUrlVerify asyncAudioUrlVerify = new Async_audioUrlVerify(act, this, dlg,audio_manager.getAudioStringAt(audio_manager.mAudioPos));
			asyncAudioUrlVerify.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Searching media ...");
	    }
    }

	// prepare Audio
	public void prepareAudio() {
		if (ENABLE_MEDIA_CONTROLLER &&  Build.VERSION.SDK_INT >= 21) {
			MainAct.mMediaControllerCompat
					.getTransportControls()
					.playFromUri(Uri.parse(audioUrl), null); // will call onPlayFromUri
		} else {
			BackgroundAudioService.mMediaPlayer = new MediaPlayer();
			BackgroundAudioService.mMediaPlayer.reset();

			if(Build.VERSION.SDK_INT >= 21)
			BackgroundAudioService.mMediaPlayer.setAudioAttributes(
					new AudioAttributes.Builder()
							.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
							.setUsage(AudioAttributes.USAGE_MEDIA)
							.build()
			);

			try {
				BackgroundAudioService.mMediaPlayer.setDataSource(act.getApplicationContext(), Uri.parse(audioUrl));

				// prepare the MediaPlayer to play, this will delay system response
				BackgroundAudioService.mMediaPlayer.prepare();
			} catch (Exception e) {
				Toast.makeText(act, R.string.audio_message_could_not_open_file, Toast.LENGTH_SHORT).show();
				audio_manager.stopAudioPlayer();
			}

			// set audio player listeners
			if (!ENABLE_MEDIA_CONTROLLER)
				setAudioPlayerListeners();

		}

		// Async for waiting Audio Prepare flag
		ProgressDialog dlg = new ProgressDialog(act);
		Async_audioPrepare asyncAudioPrepare = new Async_audioPrepare(act, this,dlg);
		asyncAudioPrepare.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Preparing to play ...");
	}

    // start audio runnable
    public void startAudioRunnable() {
	    if ( audio_manager.getPlayerState() != audio_manager.PLAYER_AT_STOP ) {
		    audio_manager.audioHandler.postDelayed(audio_manager.audio_runnable, delayBeforeMediaStart); // delay before media start
		    System.out.println("Audio7Player / _startAudioRunnable / 1st post page_runnable");
	    }
    }

    /**
     * Play next audio at AudioPlayer
     */
    void tryPlay_nextAudio()
    {
//		Toast.makeText(act,"Can not open file, try next one.",Toast.LENGTH_SHORT).show();
        System.out.println("Audio7Player / _playNextAudio");
        if(BackgroundAudioService.mMediaPlayer != null)
        {
            BackgroundAudioService.mMediaPlayer.release();
            BackgroundAudioService.mMediaPlayer = null;
        }

        // new audio index
        audio_manager.mAudioPos++;

        // check try times,had tried or not tried yet, anyway the audio file is found
	    System.out.println("Audio7Player / check mTryTimes = " + audio_tryTimes);
	    if(audio_tryTimes < audio_manager.getAudioFilesCount() ) {
		    audioUrl = audio_manager.getAudioStringAt(audio_manager.mAudioPos);

		    if(UtilAudio.hasAudioExtension(audioUrl) && Util.isUriExisted(audioUrl,act))
			    startNewAudio();
	    } else { // try enough times: still no audio file is found
		    Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();

		    // do not show highlight
		    if(MainAct.mSubMenuItemAudio != null)
			    MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);

		    // stop media player
		    audio_manager.stopAudioPlayer();
	    }
        System.out.println("Audio7Player / _playNextAudio / audio_manager.mAudioPos = " + audio_manager.mAudioPos);
    }

	public TextView audio_title;
	public TextView audio_artist;
	public TextView audio_curr_pos;
	public SeekBar audio_seek_bar;
	public TextView audio_length;
	public ImageView audio_previous_btn;
	public ImageView audio_play_btn;
	public ImageView audio_next_btn;

	private int mProgress;
	private int mediaFileLength; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class

	public int getMediaFileLength() {
		return mediaFileLength;
	}

	public void setMediaFileLength(int mediaFileLength) {
		this.mediaFileLength = mediaFileLength;
	}

	// update audio progress
	public void updateAudioProgress()
	{
		int currentPos=0;

		// return if screen off
		if(SystemState.isScreenOff(act))
			return;

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

//		System.out.println("Audio7Player / _updateAudioProgress / mediaFileLength = " + Util.getTimeFormatString(mediaFileLength));
		mProgress = (int)(((float)currentPos/ getMediaFileLength())*100);
//		System.out.println("Audio7Player / _updateAudioProgress / getMediaFileLength = " + getMediaFileLength());
//		System.out.println("Audio7Player / _updateAudioProgress / currentPos = " + currentPos);

		if(audio_seek_bar != null)
			audio_seek_bar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
	}


	// initialize audio block
	public void initAudioBlock(String audio_uriStr)
	{
		System.out.println("Audio7Player / _initAudioBlock " );

		audio_manager.mAudioUri = audio_uriStr;
		audio_title = (TextView) audio_panel.findViewById(R.id.audio_title); // first setting
		audio_artist = (TextView) audio_panel.findViewById(R.id.audio_artist);

		audio_title.setTextColor(act.getResources().getColor(R.color.colorWhite));
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
		audio_curr_pos.setTextColor(act.getResources().getColor(R.color.colorWhite));

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
		audio_length.setTextColor(act.getResources().getColor(R.color.colorWhite));

		// audio buttons
		audio_previous_btn = (ImageView) audio_panel.findViewById(R.id.audioPanel_previous);
		audio_previous_btn.setImageResource(R.drawable.ic_media_previous);

		audio_play_btn = (ImageView) audio_panel.findViewById(R.id.audioPanel_play);
		audio_play_btn.setImageResource(R.drawable.ic_media_play);

		audio_next_btn = (ImageView) audio_panel.findViewById(R.id.audioPanel_next);
		audio_next_btn.setImageResource(R.drawable.ic_media_next);
	}


	// Set Audio Player Listeners for simple case
	//if (!ENABLE_MEDIA_CONTROLLER)
	void setAudioPlayerListeners(){
		// on prepared
		BackgroundAudioService.mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {

			// workaround for Can not play issue:
			// add delay before media player start
			try {
				Thread.sleep(delayBeforeMediaStart );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			BackgroundAudioService.mMediaPlayer.seekTo(0);
			BackgroundAudioService.mIsPrepared = true;

			// prepared, start Play audio
			if(BackgroundAudioService.mMediaPlayer != null) {
				BackgroundAudioService.mMediaPlayer.start();
				BackgroundAudioService.mMediaPlayer.setVolume(1.0f, 1.0f);
			}

			// update panel status: play
			audio_manager.setPlayerState(audio_manager.PLAYER_AT_PLAY);

				audio_manager.setTogglePlayerState(true);
			}
		});

		// on completed
		BackgroundAudioService.mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {

			if(BackgroundAudioService.mMediaPlayer != null) {
				BackgroundAudioService.mMediaPlayer.release();
			}

			// delay interval between each media change
			try {
				Thread.sleep(Util.oneSecond * 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

				BackgroundAudioService.mMediaPlayer = null;
				BackgroundAudioService.mIsCompleted = true;
			}
		});

	}

	// show audio name
	void showAudioName(AppCompatActivity act,String audio_uriStr)
	{
//		System.out.println("Audio7Player / _showAudioName / audio_uriStr = " + audio_uriStr);
		// title: set marquee
		if(Util.isUriExisted(audio_uriStr, act)) {
			String[] audio_name = Util.getDisplayNameByUriString(audio_uriStr, act);

			// Note view
			if(audio_artist != null) {
				audio_title.setText(audio_name[0] );
				audio_artist.setText(audio_name[1]);
			} else { // Page view
				if(Util.isEmptyString(audio_name[1]))
					audio_title.setText(audio_name[0]);
				else
					audio_title.setText(String.format("%s / %s", audio_name[0], audio_name[1]));
			}
		}
		else {
			audio_title.setText("N/A");
			if(audio_artist != null)
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
		// return if screen off
		if(SystemState.isScreenOff(act))
			return;

		// update playing state
		if(audio_manager.getPlayerState() == audio_manager.PLAYER_AT_PLAY)
		{
//			System.out.println("Audio7Player / _updateAudioPanel / at play");
			audio_play_btn.setImageResource(R.drawable.ic_media_pause);
			showAudioName(act, audio_manager.getAudioStringAt(audio_manager.mAudioPos));
			audio_title.setTextColor(ColorSet.getHighlightColor(act) );
			audio_title.setSelected(true);

			if(audio_artist != null) {
				audio_artist.setTextColor(ColorSet.getHighlightColor(act));
				audio_artist.setSelected(true);
			}
		}
		else if(audio_manager.getPlayerState() != audio_manager.PLAYER_AT_PLAY)
		{
//			System.out.println("Audio7Player / _updateAudioPanel / not at play");
			audio_play_btn.setImageResource(R.drawable.ic_media_play);
			showAudioName(act, audio_manager.getAudioStringAt(audio_manager.mAudioPos));
			audio_title.setTextColor(ColorSet.getPauseColor(act));
			audio_title.setSelected(false);

			if(audio_artist != null) {
				audio_artist.setTextColor(ColorSet.getPauseColor(act));
				audio_artist.setSelected(false);
			}
		}
	}

}