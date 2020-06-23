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

package com.cw.audio7.main;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.operation.import_export.Import_fileView;
import com.cw.audio7.operation.import_export.ParseXmlToDB;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Download task
 * - a class that will show progress bar in the main GUI context
 */

class Async_default_byDownload extends AsyncTask<String,Integer,String>
{
	ProgressDialog downloadDialog;
	private AppCompatActivity act;
	String srcUrl;

	Async_default_byDownload(AppCompatActivity act, String srcUrl)
	{
	    this.act = act;
	    this.srcUrl = srcUrl;
	}
	 
	@Override
	protected void onPreExecute()
	{
	    super.onPreExecute();
	 	 // lock orientation
	 	 Util.lockOrientation(act);

	 	 // disable rotation
//	 	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	 	
		System.out.println("Async_default_byDownload / onPreExecute" );

        downloadDialog = new ProgressDialog(act);

		downloadDialog.setMessage("Downloading");
		downloadDialog.setCancelable(true); // set true for enabling Back button
		downloadDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //ProgressDialog.STYLE_HORIZONTAL

		downloadDialog.show();

	}
	 
	@Override
	protected String doInBackground(String... params)
	{
	    int mProgress;
	    System.out.println("Async_default_byDownload / doInBackground / params[0] = " + params[0] );
	    mProgress =0;


//		String srcUrl =   "https://drive.google.com/uc?authuser=0&id=1qAfMUJ9DMsciVkb7hEQAwLrmcyfN95sF&export=download";

		String targetUrl = "file://" + "/storage/emulated/0/audio7" + "/default_content_by_download.xml";

		DownloadManager downloadmanager = (DownloadManager) act.getSystemService(Context.DOWNLOAD_SERVICE);
		Uri uri = Uri.parse(srcUrl);

		DownloadManager.Request request = new DownloadManager.Request(uri);
		request.setTitle("audio7 download");
		request.setDescription("Downloading");
//                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                    request.setVisibleInDownloadsUi(true);
		request.setDestinationUri(Uri.parse(targetUrl));

		downloadmanager.enqueue(request);

		String dirString = Environment.getExternalStorageDirectory().toString() +
				"/" +
				Util.getStorageDirName(MainAct.mAct);
		File storageRoot = new File(dirString);
		File downloadedFile = new File(storageRoot, "default_content_by_download.xml");

		// waiting until time out
		int timeOutCount = 20;
		while( (!downloadedFile.exists()) && (timeOutCount !=0) )
		{
			try {
				Thread.sleep(Util.oneSecond/4);

				publishProgress(Integer.valueOf(mProgress));
				mProgress =+ 20;
				if(mProgress >= 100)
					mProgress = 0;
			} catch (Exception e) {
				e.printStackTrace();
			}
			timeOutCount--;
		}

		// downloaded file is ready
		DB_drawer dB_drawer = new DB_drawer(act);

		// import content
		if(downloadedFile.exists()) {

//				downloadDialog.setMessage("Importing");//todo Why not add this?
//				downloadDialog.show();
			Import_fileView.importDefaultContentByXml(act, downloadedFile);
			timeOutCount = 20;
			while(ParseXmlToDB.isParsing && (timeOutCount!=0));
			{
				// waiting until time out
				try {
					Thread.sleep(Util.oneSecond/4);
				} catch (Exception e) {
					e.printStackTrace();
				}

				mProgress =+ 20;
				if(mProgress >= 100)
					mProgress = 0;

				publishProgress(mProgress);

				timeOutCount--;
			}

			//set default position to 0
			int folderTableId = dB_drawer.getFolderTableId(0, true);
			Pref.setPref_focusView_folder_tableId(act, folderTableId);
			DB_folder.setFocusFolder_tableId(folderTableId);
		}

	    return "OK";
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress)
	{
	    System.out.println("Async_default_byDownload / OnProgressUpdate / progress[0] " + progress[0] );
	    super.onProgressUpdate(progress);
	    if(downloadDialog != null)
	        downloadDialog.setProgress(progress[0]);
	}

	// This is executed in the context of the main GUI thread
    @Override
	protected void onPostExecute(String result)
	{
	    System.out.println("Async_default_byDownload / onPostExecute / result = " + result);
		
	 	// dialog off
		if((downloadDialog != null) && downloadDialog.isShowing() )
			downloadDialog.dismiss();

 		downloadDialog = null;

		Pref.setPref_will_create_default_content(act, true);
		act.recreate();
	 }
}