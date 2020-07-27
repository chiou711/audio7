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

package com.cw.audio7.util.uil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import com.cw.audio7.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class UilCommon 
{
	public static ImageLoader imageLoader;
	public static DisplayImageOptions options;
	public static DisplayImageOptions optionsForRounded_light;
	public static DisplayImageOptions optionsForRounded_dark;
	public static DisplayImageOptions optionsForFadeIn;
	public static DisplayImageOptions optionsForRounded_light_playIcon;
	public static DisplayImageOptions optionsForRounded_dark_playIcon;

	UilCommon(){};

	public static void init()
	{
		imageLoader = ImageLoader.getInstance();
		
		options = new DisplayImageOptions.Builder()
			.showImageOnLoading(R.drawable.ic_stub)
			.showImageForEmptyUri(R.drawable.btn_radio_off_holo_light)//R.drawable.ic_empty
			.showImageOnFail(R.drawable.ic_not_found)// R.drawable.ic_error
			.cacheInMemory(true)
	//		.cacheOnDisk(true)
			.cacheOnDisk(false)
			.imageScaleType(ImageScaleType.EXACTLY)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.considerExifParams(true)
			.build();
		
		optionsForFadeIn = new DisplayImageOptions.Builder()
			.showImageForEmptyUri(R.drawable.btn_radio_off_holo_light)
			.showImageOnFail(R.drawable.ic_not_found)
			.resetViewBeforeLoading(true)
			.cacheOnDisk(false)
			.imageScaleType(ImageScaleType.EXACTLY)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.considerExifParams(true)
			.displayer(new FadeInBitmapDisplayer(300))
			.build();	
		
		DisplayImageOptions.Builder optionsForRounded = new DisplayImageOptions.Builder()
			.resetViewBeforeLoading(true)
			.cacheInMemory(true)
			.cacheOnDisk(false)
			.imageScaleType(ImageScaleType.EXACTLY)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.considerExifParams(true)
			.displayer(new RoundedBitmapDisplayer(0));//ori 20

		optionsForRounded_light = optionsForRounded
			.showImageForEmptyUri(R.drawable.btn_radio_off_holo_light)
			.showImageOnFail(R.drawable.ic_not_found)
			.build();
		
		optionsForRounded_dark = optionsForRounded
			.showImageForEmptyUri(R.drawable.btn_radio_off_holo_dark)
			.showImageOnFail(R.drawable.ic_not_found) //R.drawable.ic_media_play
			.build();				
	}
	
    public static ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
    
	private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener
	{

		static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) 
		{
			if (loadedImage != null) 
			{
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) 
				{
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}
}
