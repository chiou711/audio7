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

package com.cw.audio7.util.image;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.cw.audio7.R;
import com.cw.audio7.util.ColorSet;

import androidx.core.graphics.drawable.DrawableCompat;

//Audio bitmap Async Task for applying MediaMetadataRetriever
//Note: setDataSource could hang up system for a long time when accessing remote content
public class AsyncTaskAudioBitmap extends AsyncTask<String,Integer,String>
{
	final ThreadLocal<Activity> act = new ThreadLocal<>();
	String audioUri;
	final ThreadLocal<ImageView> imageView = new ThreadLocal<>();
	MediaMetadataRetriever mmr;
	Bitmap bitmap;
	final ThreadLocal<ProgressBar> progressBar = new ThreadLocal<>();
    boolean enRounded;
    int inSampleSize;

	public AsyncTaskAudioBitmap(Activity act, String audioString, ImageView view, ProgressBar progressBar, boolean enableRounded, int in_sample_size)
	{
		this.act.set(act);
		audioUri = audioString;
		imageView.set(view);
		this.progressBar.set(progressBar);
        enRounded = enableRounded;
		inSampleSize = in_sample_size;
	}
	 
	@Override
	protected void onPreExecute()
	{
		 super.onPreExecute();

         // Set this will cause image view blank at Portrait pager, so set it null as a workaround
         if(null != progressBar.get()) {
	         Objects.requireNonNull(progressBar.get()).setProgress(0);

             // set progress bar color
//	         DrawableCompat.setTint(mProgressBar.getIndeterminateDrawable(),Color.parseColor("#ff176f77"));
	         DrawableCompat.setTint(Objects.requireNonNull(progressBar.get()).getIndeterminateDrawable(), ColorSet.getHighlightColor(Objects.requireNonNull(act.get())));

	         Objects.requireNonNull(progressBar.get()).setVisibility(View.VISIBLE);
        }
	}

	 @Override
	protected String doInBackground(String... params)
	{
		 mmr = new MediaMetadataRetriever();
		 try
		 {
			 mmr.setDataSource(act.get(),Uri.parse(audioUri));

			 byte[] artBytes =  mmr.getEmbeddedPicture();
			 if(artBytes != null)
			 {
				 //https://stackoverflow.com/questions/11820266/android-bitmapfactory-decodestream-out-of-memory-with-a-400kb-file-with-2mb-f
				 InputStream is = new ByteArrayInputStream(mmr.getEmbeddedPicture());
//				 bitmap = BitmapFactory.decodeStream(is); // with this will cause OOM issue
				 BitmapFactory.Options options = new BitmapFactory.Options();
				 options.inJustDecodeBounds = false;
				 options.inSampleSize = inSampleSize;
				 bitmap = BitmapFactory.decodeStream(is, null, options);
			 }
			 mmr.release();
		 }
		 catch(Exception e)
		 {
			 Log.e("AsyncTaskAudioBitmap", "setDataSource / illegal argument");
		 }

		 return null;
	}

	@Override
	protected void onProgressUpdate(Integer... progress)
	{
	     super.onProgressUpdate(progress);
	}

	// This is executed in the context of the main GUI thread
	protected void onPostExecute(String result)
	{
         if(null != progressBar.get())
		    Objects.requireNonNull(progressBar.get()).setVisibility(View.GONE);

		 if(bitmap != null)
		 {
			 ((ViewGroup) Objects.requireNonNull(imageView.get()).getParent()).setVisibility(View.VISIBLE);
             if(enRounded)
				 Objects.requireNonNull(imageView.get()).setImageBitmap(UtilImage.getRoundedCornerBitmap(bitmap, 10));
             else
				 Objects.requireNonNull(imageView.get()).setImageBitmap(bitmap);

			 Objects.requireNonNull(imageView.get()).setVisibility(View.VISIBLE);
		 }
		 else
		 {
		 	 // It’s best practice to place your app icons in mipmap- folders (not the drawable- folders)
			 // https://android-developers.googleblog.com/2014/10/getting-your-apps-ready-for-nexus-6-and.html
//			 Bitmap bitmap = BitmapFactory.decodeResource(mAct.getResources(), R.mipmap.ic_launcher); //todo Is null?
			 Bitmap bitmap = BitmapFactory.decodeResource(Objects.requireNonNull(act.get()).getResources(), R.drawable.ic_launcher);
			 bitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
			 if(bitmap != null)
				 Objects.requireNonNull(imageView.get()).setImageBitmap(bitmap);

			 Objects.requireNonNull(imageView.get()).setVisibility(View.VISIBLE);
			 ((ViewGroup) Objects.requireNonNull(imageView.get()).getParent()).setVisibility(View.VISIBLE);
		 }

	}
}