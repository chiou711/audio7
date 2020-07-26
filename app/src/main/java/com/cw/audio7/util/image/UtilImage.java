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

package com.cw.audio7.util.image;

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;

import com.cw.audio7.util.Util;

public class UtilImage 
{
    public UtilImage(){};

    // Get default scale in percent
    public static int getDefaultScaleInPercent(Activity act)
    {
        // px = dp * (dpi / 160),
        // px:pixel, scale in percent here 
        // dp:density-independent pixels
        // dpi:dots per inch
        int dpi = (int)act.getResources().getDisplayMetrics().densityDpi;
        switch (dpi) 
        {
	        case DisplayMetrics.DENSITY_LOW:
	            System.out.println("DENSITY_LOW");
	            break;
	        case DisplayMetrics.DENSITY_MEDIUM:
	            System.out.println("DENSITY_MEDIUM");
	            break;
	        case DisplayMetrics.DENSITY_HIGH:
	            System.out.println("DENSITY_HIGH");
	            break;
	        case DisplayMetrics.DENSITY_XHIGH:
	            System.out.println("DENSITY_XHIGH");
	            break;
	        case DisplayMetrics.DENSITY_XXHIGH:
	            System.out.println("DENSITY_XXHIGH");
	            break;
	        case DisplayMetrics.DENSITY_XXXHIGH:
	            System.out.println("DENSITY_XXXHIGH");
	            break;
        } 
        
        System.out.println("densityDpi = " + dpi);
        int dp = 100;
        int px = (int)(dp*(dpi/160.0f));
        System.out.println("Default Sacle In Percent = " + px);
        return px;
    }
    
    // check if file has image extension
    // refer to http://developer.android.com/intl/zh-tw/guide/appendix/media-formats.html
    public static boolean hasImageExtension(File file)
    {
    	boolean has = false;
    	String fn = file.getName().toLowerCase(Locale.getDefault());
    	if(	fn.endsWith("jpg") || fn.endsWith("gif") ||
    		fn.endsWith("png") || fn.endsWith("bmp") || fn.endsWith("webp") ) 
	    	has = true;
	    
    	return has;
    } 
    
    // check if string has image extension
    public static boolean hasImageExtension(String string, Activity act)
    {
    	boolean has = false;
    	if(!Util.isEmptyString(string))
    	{
	    	String fn = string.toLowerCase(Locale.getDefault());
	    	if(	fn.endsWith("jpg") || fn.endsWith("gif") ||
	    		fn.endsWith("png") || fn.endsWith("bmp") || fn.endsWith("webp") ) 
		    	has = true;
    	}
		else
			return has;
    	
    	if(!has)
    	{
    		String fn = Util.getDisplayNameByUriString(string, act)[0];

    		if(!Util.isEmptyString(fn)) {
				fn = fn.toLowerCase(Locale.getDefault());
				if (fn.endsWith("jpg") || fn.endsWith("gif") ||
						fn.endsWith("png") || fn.endsWith("bmp") || fn.endsWith("webp"))
					has = true;
			}
    	}    	
    	return has;
    }        
    
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int percent)
	{
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final int roundPx = Math.round((bitmap.getHeight()*percent/10)/10);

//        System.out.println("UtilImage / getRoundedCornerBitmap / bitmap.getHeight() = " + bitmap.getHeight());
//        System.out.println("UtilImage / getRoundedCornerBitmap / bitmap.getWidth() = " + bitmap.getWidth());
//        System.out.println("UtilImage / getRoundedCornerBitmap / roundPx = " + roundPx);

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

}