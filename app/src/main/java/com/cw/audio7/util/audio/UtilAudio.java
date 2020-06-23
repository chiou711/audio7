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

package com.cw.audio7.util.audio;

import java.io.File;
import java.util.Locale;

import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.R;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;

//import android.support.v7.app.AppCompatActivity;
//import android.telephony.PhoneStateListener;
//import android.telephony.TelephonyManager;
import android.widget.ImageView;
import android.widget.TextView;

//import static android.content.Context.TELEPHONY_SERVICE;

public class UtilAudio {

//	public static void setPhoneListener(AppCompatActivity act)
//	{
//		System.out.println("UtilAudio/ _setPhoneListener");
//
//		// To Registers a listener object to receive notification when incoming call
//		TelephonyManager telMgr = (TelephonyManager) act.getSystemService(TELEPHONY_SERVICE);
//		if (telMgr != null) {
//			telMgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//		}
//	}

    public static void stopAudioIfNeeded()
    {
		if( ( (BackgroundAudioService.mMediaPlayer != null) &&
              (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) ) &&
			(MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()) &&
			(TabsHost.getFocus_tabPos() == MainAct.mPlaying_pagePos)                           )
		{
            if(BackgroundAudioService.mMediaPlayer != null){
                Audio_manager.stopAudioPlayer();
                Audio_manager.mAudioPos = 0;
            }

			if(MainAct.mSubMenuItemAudio != null)
				MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);

		}
    }
    
    // update audio panel
    public static void updateAudioPanel(ImageView playBtn, TextView titleTextView)
    {
    	System.out.println("UtilAudio/ _updateAudioPanel / Audio_manager.getPlayerState() = " + Audio_manager.getPlayerState());
		titleTextView.setBackgroundColor(ColorSet.color_black);
		if(Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PLAY)
		{
			titleTextView.setTextColor(ColorSet.getHighlightColor(MainAct.mAct));
			titleTextView.setSelected(true);
			playBtn.setImageResource(R.drawable.ic_media_pause);
		}
		else if( (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PAUSE) ||
				 (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_STOP)    )
		{
			titleTextView.setSelected(false);
			titleTextView.setTextColor(ColorSet.getPauseColor(MainAct.mAct));
			playBtn.setImageResource(R.drawable.ic_media_play);
		}

    }

    // check if file has audio extension
    // refer to http://developer.android.com/intl/zh-tw/guide/appendix/media-formats.html
    public static boolean hasAudioExtension(File file)
    {
    	boolean hasAudio = false;
    	String fn = file.getName().toLowerCase(Locale.getDefault());
    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
       		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
       		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
       		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
       		fn.endsWith("wav") || fn.endsWith("wma")
    		) 
	    	hasAudio = true;
	    
    	return hasAudio;
    }
    
    // check if string has audio extension
    public static boolean hasAudioExtension(String string)
    {
    	boolean hasAudio = false;
    	if(!Util.isEmptyString(string))
    	{
	    	String fn = string.toLowerCase(Locale.getDefault());
	    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
	           		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
	           		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
	           		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
	           		fn.endsWith("wav") || fn.endsWith("wma")
	        		) 
	    		hasAudio = true;
    	}
    	return hasAudio;
    }     
    
//    public static boolean mIsCalledWhilePlayingAudio;
    // for Pause audio player when incoming phone call
    // http://stackoverflow.com/questions/5610464/stopping-starting-music-on-incoming-calls
//    public static PhoneStateListener phoneStateListener = new PhoneStateListener()
//    {
//        @Override
//        public void onCallStateChanged(int state, String incomingNumber)
//        {
//			System.out.print("UtilAudio / _onCallStateChanged");
//            if ( (state == TelephonyManager.CALL_STATE_RINGING) ||
//                 (state == TelephonyManager.CALL_STATE_OFFHOOK )   )
//            {
//            	System.out.println(" -> Incoming phone call:");
//
//                //from Play to Pause
//            	if(Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PLAY)
//            	{
//                    if( (BackgroundAudioService.mMediaPlayer != null) &&
//							BackgroundAudioService.mMediaPlayer.isPlaying() ) {
//                        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PAUSE);
//						BackgroundAudioService.mMediaPlayer.pause();
//                    }
//            		mIsCalledWhilePlayingAudio = true;
//            	}
//            }
//            else if(state == TelephonyManager.CALL_STATE_IDLE)
//            {
//            	System.out.println(" -> Not in phone call:");
//                // from Pause to Play
//            	if( (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PAUSE) &&
//            		mIsCalledWhilePlayingAudio )
//            	{
//                    if( (BackgroundAudioService.mMediaPlayer != null) &&
//                        !BackgroundAudioService.mMediaPlayer.isPlaying() ) {
//                        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);
//						BackgroundAudioService.mMediaPlayer.start();
//                    }
//                    mIsCalledWhilePlayingAudio = false;
//            	}
//            }
//            super.onCallStateChanged(state, incomingNumber);
//        }
//    };
    
    
}
