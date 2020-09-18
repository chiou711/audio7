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

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
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

	// get audio file length
	public static int getAudioLength(Activity act, String audiUri) {
		int len = 0;
		if (Util.isUriExisted(audiUri, act)) {
			try {
				MediaPlayer mp = MediaPlayer.create(act, Uri.parse(audiUri));
				if(mp!= null) {
					len = mp.getDuration();
					mp.release();
				}
			}
			catch(Exception e)
			{
				System.out.println("UtilAudio / _getAudioLength / exception");
			}
		}

		return len;
	}

	// get audio file length string
	public static String getAudioLengthString(Activity act, String audiUri) {

		int len = getAudioLength(act,audiUri);

		// set audio file length
		int fileHour = Math.round((float)(len / 1000 / 60 / 60));
		int fileMin = Math.round((float)((len - fileHour * 60 * 60 * 1000) / 1000 / 60));
		int fileSec = Math.round((float)((len - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));

		String strHour = String.format(Locale.ENGLISH,"%2d", fileHour);
		String strMinute = String.format(Locale.ENGLISH,"%02d", fileMin);
		String strSecond = String.format(Locale.ENGLISH,"%02d", fileSec);
		String strLength = strHour + ":" + strMinute+ ":" + strSecond;

    	return strLength;
	}
    
}
