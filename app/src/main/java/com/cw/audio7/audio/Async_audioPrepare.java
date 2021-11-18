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

import com.cw.audio7.R;
import com.cw.audio7.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.Objects;
import static com.cw.audio7.audio.BackgroundAudioService.mAudio_manager;
import static com.cw.audio7.audio.BackgroundAudioService.mMediaPlayer;

/***************************************************************
 * 
 * audio prepare task
 * 
 */
public class Async_audioPrepare extends AsyncTask<String,Integer,String>
{
	final ThreadLocal<Activity> act = new ThreadLocal<>();
	final ProgressDialog progressDialog;
	Audio7Player audio7Player;

	 Async_audioPrepare(Activity act, Audio7Player _audio7Player,ProgressDialog _dlg) {
	 	 this.progressDialog = _dlg;
		 this.act.set(act);
		 audio7Player = _audio7Player;
	 }
	 
	 @Override
	 protected void onPreExecute() 
	 {
	 	super.onPreExecute();

		progressDialog.setMessage(Objects.requireNonNull(act.get()).getResources().getText(R.string.audio_message_preparing_to_play));
		progressDialog.setCancelable(true); // set true for enabling Back button
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //ProgressDialog.STYLE_HORIZONTAL

		// only for Page play mode
		// show dialog will affect full screen at Note play mode
        if( mAudio_manager.getAudioPlayMode() == mAudio_manager.PAGE_PLAY_MODE)
        {
	        if(!Objects.requireNonNull(act.get()).isFinishing() && !Objects.requireNonNull(act.get()).isDestroyed())
		        progressDialog.show();
        }

        BackgroundAudioService.mIsPrepared = false;
	 } 
	 
	 @Override
	 protected String doInBackground(String... params) 
	 {
//         System.out.println("Async_audioPrepare / doInBackground / params[0] = " + params[0] );

		 boolean isTimeOut = false;
		 int progress = 0;
		 int count = 0;

		 while(	(!isTimeOut) &&
				    (!BackgroundAudioService.mIsPrepared) &&
				    ( (mMediaPlayer != null) &&
				      (!mMediaPlayer.isPlaying()) ) )
		 {
			 System.out.println("Async_audioPrepare / doInBackground / count = " + count);
			 count++;
			 
			 if(count >= 40) // 10 seconds, 1/4 * 40
				 isTimeOut = true;
			 
			 publishProgress(progress);
			 
			 progress += 20;
			 if(progress >= 100)
				 progress = 0;
			 
			 try {
				Thread.sleep(Util.oneSecond/4);
			 } catch (InterruptedException e) {
				e.printStackTrace();
			 } 
		 }
		 
		 if(isTimeOut)
			return "timeout";
		 else
			return "ok";
	 }
	
	 @Override
	 protected void onProgressUpdate(Integer... progress) 
	 { 
//		 System.out.println("Async_audioPrepare / OnProgressUpdate / progress[0] " + progress[0] );
	     super.onProgressUpdate(progress);
	     
	     if((progressDialog != null) && progressDialog.isShowing())
	    	 progressDialog.setProgress(progress[0]);
	 }
	 
	 // This is executed in the context of the main GUI thread
	 @Override
	 protected void onPostExecute(String result)
	 {
//	 	System.out.println("Async_audioPrepare / _onPostExecute / result = " + result);
	 	
	 	// dialog off
		if((progressDialog != null) && progressDialog.isShowing())
			progressDialog.dismiss();

		// show time out
		if(result.equalsIgnoreCase("timeout"))
		{
			Toast toast = Toast.makeText(Objects.requireNonNull(act.get()).getApplicationContext(), R.string.audio_message_preparing_time_out, Toast.LENGTH_SHORT);
			toast.show();
		} else {
			// Prepare is ready, start Audio Runnable
			audio7Player.startAudioRunnable();
		}

		// unlock orientation
		Util.unlockOrientation(Objects.requireNonNull(act.get()));
	 }
	 
}
