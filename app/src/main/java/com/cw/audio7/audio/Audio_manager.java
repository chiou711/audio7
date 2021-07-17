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

import java.util.ArrayList;
import java.util.List;

import com.cw.audio7.db.DB_page;
import com.cw.audio7.util.Util;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import static com.cw.audio7.define.Define.ENABLE_MEDIA_CONTROLLER;
import static com.cw.audio7.main.MainAct.mFolderUi;
import static com.cw.audio7.main.MainAct.removeRunnable;

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

	public Audio_manager(AppCompatActivity _act) {
		act = _act;
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
    public void stopAudioPlayer(AppCompatActivity act)
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
	    if(ENABLE_MEDIA_CONTROLLER)
            NotificationManagerCompat.from(act).cancel(BackgroundAudioService.notification_id);

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
	public void setupAudioList(AppCompatActivity act)
	{
		audioList = new ArrayList<>();
		audioList_checked = new ArrayList<>();

		// init
		setPlayPrevious(false);
		setTogglePlayerState(false);
		setPlayNext(false);

		DB_page db_page = new DB_page(act, mFolderUi.tabsHost.getCurrentPageTableId());

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
	
}