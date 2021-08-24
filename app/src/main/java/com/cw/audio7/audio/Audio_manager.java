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

import android.os.Build;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import static com.cw.audio7.define.Define.ENABLE_MEDIA_CONTROLLER;

public class Audio_manager
{
	private  List<String> audioList;
	private  List<Integer> audioList_checked;

    private  int mAudioPlayMode;
    public final  int NOTE_PLAY_MODE = 0;
    public final  int PAGE_PLAY_MODE = 1;

    private static int mPlayerState;
    public  int PLAYER_AT_STOP = 0;
    public  int PLAYER_AT_PLAY = 1;
    public  int PLAYER_AT_PAUSE = 2;
    public  int mAudioPos; // index of current media to play

	// control buttons
	public  boolean playPrevious;
	public  boolean playNext;

	public  boolean togglePlayerState;
	public  String mAudioUri;
	public  boolean kill_runnable = false;
	AppCompatActivity act;
	public boolean doScroll;
	public Audio7Player audio7Player;
	public Runnable audio_runnable;
	public Handler audioHandler;
	Folder folder;


	public Audio_manager(AppCompatActivity _act, Folder _folder) {
		act = _act;
		folder = _folder;

		// start a new handler
		audioHandler = new Handler();

		/**
		 * Continue mode runnable
		 */
		if(audio_runnable == null)
			audio_runnable = new Runnable() {
			@Override
			public void run() {
//				System.out.println("Audio_manager / _audio_runnable");

				if(kill_runnable) {
//					System.out.println("------------ Audio_manager / _audio_runnable / kill runnable = true");
					removeRunnable();
				}

				if (getCheckedAudio(mAudioPos) == 1) {

					// for incoming call case and after Key protection
					if (  (getAudioPlayMode() == PAGE_PLAY_MODE) &&
							audio7Player.isAudioPanelOn(act) ) {
						audio7Player.showAudioPanel(true);
					}

					/** update audio progress */
					audio7Player.updateAudioProgress();

					if(ENABLE_MEDIA_CONTROLLER && Build.VERSION.SDK_INT >= 21)
						BackgroundAudioService.setSeekerBarProgress(BackgroundAudioService.mMediaPlayer);

					// check if audio file exists or not
					audio7Player.audioUrl = getAudioStringAt(mAudioPos);

					if (!Async_audioUrlVerify.mIsOkUrl) {
						audio7Player.audio_tryTimes++;
						audio7Player.tryPlay_nextAudio();
						return;
					} else {
						if (BackgroundAudioService.mIsPrepared) {
							//						System.out.println("Audio_manager / _audio_runnable /  BackgroundAudioService.mIsPrepared");

							// set media file length
							if (!Util.isEmptyString(audio7Player.audioUrl)) {
								TextView audioPanel_file_length = (TextView) act.findViewById(R.id.audioPanel_file_length);
								if (audioPanel_file_length != null)
									audioPanel_file_length.setText(UtilAudio.getAudioLengthString(act, audio7Player.audioUrl));
							}
							BackgroundAudioService.mIsPrepared = false;
						}

						if (BackgroundAudioService.mIsCompleted) {
							System.out.println("Audio_manager / _audio_runnable /  BackgroundAudioService.mIsCompleted");
							setPlayNext(true);
							BackgroundAudioService.mIsCompleted = false;
						}
					}

					if (audio7Player.audio_tryTimes < getAudioFilesCount()) {
						if( isPlayPrevious() ||
								isPlayNext()               )
						{
							if(audioHandler != null)
								audioHandler.removeCallbacks(audio_runnable);
							audioHandler = null;

							// play previous
							if (isPlayPrevious()) {
								System.out.println("Audio_manager / _audio_runnable /  isPlayPrevious");
								audio7Player.audio_previous_btn.performClick();
								setPlayPrevious(false);
							}

							// play next
							if (isPlayNext() ) {
								System.out.println("Audio_manager / _audio_runnable /  isPlayNext");
								audio7Player.audio_next_btn.performClick();
								setPlayNext(false);
							}
						} else {
							// toggle play / pause
							if(isTogglePlayerState()) {

								/** update audio panel when media controller */
								audio7Player.updateAudioPanel(act);

								// for page audio gif play/pause
								if( (folder !=null) &&
									(folder.tabsHost != null) &&
									(folder.tabsHost.getCurrentPage().itemAdapter != null) &&
									(getAudioPlayMode() == PAGE_PLAY_MODE) ) {
									folder.tabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
								}

								setTogglePlayerState(false);
							}

							if (audio7Player.audio_tryTimes == 0) {
								audioHandler.postDelayed(audio_runnable, audio7Player.DURATION_1S);
							} else
								audioHandler.postDelayed(audio_runnable, audio7Player.DURATION_1S / 10);
						}
					}

					// do Scroll for changing Note play to Page play
					if( doScroll && willDoScroll())
						audio7Player.scrollPlayingItemToBeVisible(folder.tabsHost.getCurrentPage().recyclerView);

				}
				else if( (getCheckedAudio(mAudioPos) == 0 ) )// for non-audio item
				{
					//	   			System.out.println("Audio_manager / audio_runnable / for non-audio item");

					if(getAudioPlayMode() == NOTE_PLAY_MODE) {
						stopAudioPlayer();

						// case 1: play next
						//					audio_next_btn.performClick();

						// case 2: show unchecked
						Toast.makeText(act,R.string.is_an_unchecked_item,Toast.LENGTH_SHORT).show();
						audio7Player.updateAudioPanel(act);
						audio7Player.updateAudioProgress();
					}

					if (getAudioPlayMode() == PAGE_PLAY_MODE) {
						audio7Player.tryPlay_nextAudio();

						if(isOnAudioPlayingPage()) {
							audio7Player.scrollPlayingItemToBeVisible(folder.tabsHost.getCurrentPage().recyclerView);
							folder.tabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
						}
					}
				}
			}
		};
	}

	// get play previous
	public boolean isPlayPrevious() {
		return playPrevious;
	}

	// set play previous
	public void setPlayPrevious(boolean _playPrevious) {
		playPrevious = _playPrevious;
	}

	// get play next
	public boolean isPlayNext() {
		return playNext;
	}

	// set play next
	public void setPlayNext(boolean _playNext) {
		playNext = _playNext;
	}

	// get toggle player state
	public boolean isTogglePlayerState() {
		return togglePlayerState;
	}

	// set toggle player state
	public void setTogglePlayerState(boolean _togglePlayerState) {
		togglePlayerState = _togglePlayerState;
	}

    /**
     * Setters and Getters
     *
     */
    // player state
    public int getPlayerState() {
        return mPlayerState;
    }

    public void setPlayerState(int playerState) {
        mPlayerState = playerState;
    }

    // Audio play mode
    public int getAudioPlayMode() {
        return mAudioPlayMode;
    }

    public void setAudioPlayMode(int audioPlayMode) {
        mAudioPlayMode = audioPlayMode;
    }

    /**
     * Stop audio
     */
    public void stopAudioPlayer()
    {
        System.out.println("Audio_manager / _stopAudioPlayer");

        // stop media player
        if(BackgroundAudioService.mMediaPlayer != null) {
            if (BackgroundAudioService.mMediaPlayer.isPlaying()) {
                BackgroundAudioService.mMediaPlayer.pause();
                BackgroundAudioService.mMediaPlayer.stop();
            }
            BackgroundAudioService.mMediaPlayer.release();
            BackgroundAudioService.mMediaPlayer = null;
        }

        setPlayerState(PLAYER_AT_STOP);

        //hide notification
	    if(ENABLE_MEDIA_CONTROLLER) {
	    	if(act != null)
		        NotificationManagerCompat.from(act).cancel(BackgroundAudioService.notification_id);
	    }

	    removeRunnable();
    }




   // Get audio files count
   int getAudioFilesCount()
   {
	   int size = 0; 
	   if(audioList != null)
	   {
		  for(int i=0;i< audioList.size();i++)
		  {
			  if( !Util.isEmptyString(audioList.get(i)) && (getCheckedAudio(i) == 1) )
				  size++;
		  }
	   }
	   return size;
   }

   // Add audio to list
   private void addAudio(String path)
   {
      audioList.add(path);
   }

	// Add audio with marking to list
	private void addCheckedAudio(int i)
	{
		audioList_checked.add(i);
	}

	private void setCheckedAudio(int index, int marking)
	{
		audioList_checked.set(index,marking);
	}

	public int getCheckedAudio(int index)
	{
		return  audioList_checked.get(index);
	}

	// return String at position index
   public String getAudioStringAt(int index)
   {
      if (index >= 0 && index < audioList.size())
         return audioList.get(index);
      else
         return null;
   }
   
	// Set up audio list
	public void setupAudioList()
	{
		audioList = new ArrayList<>();
		audioList_checked = new ArrayList<>();

		// init
		setPlayPrevious(false);
		setTogglePlayerState(false);
		setPlayNext(false);

		DB_page db_page = new DB_page(TabsHost.getCurrentPageTableId());

		int notesCount =  db_page.getNotesCount(true);
		setPlayingPage_notesCount(notesCount);

		db_page.open();
	 	// update media info
	 	for(int i = 0; i< notesCount; i++)
	 	{
	 		String audioUri = db_page.getNoteAudioUri(i,false);

	 		// initialize
	 		addAudio(audioUri);
		    addCheckedAudio(i);

		    // set playable
		    if( !Util.isEmptyString(audioUri)  &&
				    (db_page.getNoteMarking(i,false) == 1) )
			    setCheckedAudio(i,1);
		    else
			    setCheckedAudio(i,0);
	 	}
	 	db_page.close();
	}

	static int playingPage_notesCount;
	public static void setPlayingPage_notesCount(int count) {
		playingPage_notesCount = count;
	}

	public static int getPlayingPage_notesCount() {
	    return playingPage_notesCount;
    }

	// check if is on audio playing page
	public boolean isOnAudioPlayingPage()
	{
		if((folder == null) || (folder.tabsHost == null))
			return false;

		String prefix = "Audio_manager / _isOnAudioPlayingPage / ";
		boolean showDbgMsg = false;

		boolean isSameTabPos = (TabsHost.getFocus_tabPos() == MainAct.mPlaying_pagePos);
		if(showDbgMsg)
			System.out.println( prefix + "isSameTabPos = " + isSameTabPos);

		boolean isPlayOrPause = (getPlayerState() != PLAYER_AT_STOP);
		if(showDbgMsg)
			System.out.println( prefix + "isPlayOrPause = " +isPlayOrPause);

		boolean isPlayingOnFocusFolderPos = (MainAct.mPlaying_folderPos == folder.getFocus_folderPos());
		if(showDbgMsg)
			System.out.println(prefix + "isPlayingOnFocusFolderPos = " + isPlayingOnFocusFolderPos);

		boolean isPlayingOnCurrPageTableId = (MainAct.mPlaying_pageTableId == TabsHost.getCurrentPageTableId());
		if(showDbgMsg)
			System.out.println(prefix + "isPlayingOnCurrPageTableId = " + isPlayingOnCurrPageTableId);

		boolean isCurrRecycleViewExist = (folder.tabsHost.getCurrentPage().recyclerView != null);
		if(showDbgMsg)
			System.out.println(prefix + "isCurrRecycleViewExist  = " + isCurrRecycleViewExist);

		return  (isPlayOrPause &&
				isPlayingOnFocusFolderPos &&
				isSameTabPos     &&
				isPlayingOnCurrPageTableId &&
				isCurrRecycleViewExist);
	}

	public boolean willDoScroll() {
		return
	    (getPlayerState() != PLAYER_AT_STOP) &&
	    (MainAct.mPlaying_folderPos == Folder.getFocus_folderPos()) &&
		(TabsHost.getFocus_tabPos() == MainAct.mPlaying_pagePos)     &&
	    (MainAct.mPlaying_pageTableId == TabsHost.getCurrentPageTableId()) ;
    }

	// remove runnable for update audio playing
	public void removeRunnable() {
		if (audioHandler != null && audio_runnable != null) {
			audioHandler.removeCallbacks(audio_runnable);
			kill_runnable = false;
		}
	}
	
}