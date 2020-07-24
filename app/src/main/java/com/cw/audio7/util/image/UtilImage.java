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

package com.cw.audio7.util.image;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.cw.audio7.R;
import com.cw.audio7.util.Util;

public class UtilImage 
{
    public static boolean bShowExpandedImage = false;

    public UtilImage(){};

    public static int getScreenWidth(Context context)
	{
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.x;
		}
		else
		{
			return display.getWidth();
		}
	}
	
    public static int getScreenHeight(Context context)
	{
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	    Display display = wm.getDefaultDisplay();
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.y;
		}
		else
		{
			return display.getHeight();
		}
	}    
    
	public static int getScreenWidth(Activity activity)
	{
	    Display display = activity.getWindowManager().getDefaultDisplay();
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.x;
		}
		else
		{
			return display.getWidth();
		}
	}
	
	public static int getScreenHeight(Activity activity)
	{
	    Display display = activity.getWindowManager().getDefaultDisplay();
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.y;
		}
		else
		{
			return display.getHeight();
		}
	}
	
    /**
    * "Zooms" in a thumbnail view by assigning the high resolution image to a hidden "zoomed-in"
    * image view and animating its bounds to fit the entire activity content area. More
    * specifically:
    *
    * <ol>
    *   <li>Assign the high-res image to the hidden "zoomed-in" (expanded) image view.</li>
    *   <li>Calculate the starting and ending bounds for the expanded view.</li>
    *   <li>Animate each of four positioning/sizing properties (X, Y, SCALE_X, SCALE_Y)
    *       simultaneously, from the starting bounds to the ending bounds.</li>
    *   <li>Zoom back out by running the reverse animation on click.</li>
    * </ol>
    *
    * @param thumbView  The thumbnail view to zoom in.
    * @param imageResId The high-resolution version of the image represented by the thumbnail.
    * @throws IOException 
    */
	static Animator mCurrentAnimator;
	private static int mShortAnimationDuration;
	public static ImageView mExpandedImageView;
	static View mThumbView;
	static Rect mStartBounds;
	static float mStartScaleFinal;
	static Dialog mDialog;
	static Activity mAddNewAct;
	static boolean bEnableContinue;
	
//	static void zoomImageFromThumb(final View thumbView,
//    		 						String pictureUri,
//    		 						final Activity act) throws IOException
//    {
//        // Load the high-resolution "zoomed-in" image.
//        mExpandedImageView = (ImageView) act.findViewById(R.id.expanded_image);
//    	
//        mThumbView = thumbView;
//
//        // If there's an animation in progress, cancel it immediately and proceed with this one.
//        if (mCurrentAnimator != null) {
//            mCurrentAnimator.cancel();
//        }
//        
//        // Retrieve and cache the system's default "short" animation time.
//        mShortAnimationDuration = act.getResources().getInteger(android.R.integer.config_shortAnimTime);
//
//        Uri imageUri = Uri.parse(pictureUri);
//        System.out.println("zoomImageFromThumb / imageUri = " + imageUri);
//		
//		// for different scheme
//		String filePath = null;
//		String scheme = Util.getUriScheme(pictureUri);	
//        if ((imageUri != null) && scheme.equals("content")) 
//        {
//        	String[] projection = new String[] { android.provider.MediaStore.Images.ImageColumns.DATA };
//            Cursor cursor = act.getContentResolver().query(imageUri, projection, null, null, null);
//            cursor.moveToFirst();   
//            filePath = cursor.getString(0);
//            cursor.close();
//        }
//        else 
//        {
//            filePath = imageUri.getPath();
//        }
//        
////		System.out.println("filePath = " + filePath);
//		ExifInterface exif = new ExifInterface(filePath);
//		String length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
//		String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
//		
////		System.out.println(String.format(Locale.US, "width = %s length = %s", width, length));
//		
//		// makes sure EXIF data
//		if( (Integer.valueOf(length) > 0 ) && (Integer.valueOf(length) > 0))
//		{		// set picture
//			mExpandedImageView.setVisibility(View.VISIBLE );
//			bShowExpandedImage = true;
//
//			mExpandedImageView.setImageBitmap(decodeSampledBitmapFromUri(imageUri,
//																		 Integer.valueOf(width)/5,
//																		 Integer.valueOf(length)/5,
//																		 act));
//		}
//		else 
//		{
//			// image is not seen
//			mExpandedImageView.setVisibility(View.INVISIBLE);
//			bShowExpandedImage = false;
//			return;
//		}
//									
//        // Calculate the starting and ending bounds for the zoomed-in image. This step
//        // involves lots of math. Yay, math.
//        mStartBounds = new Rect();
//        final Rect finalBounds = new Rect();
//        final Point globalOffset = new Point();
//
//        // The start bounds are the global visible rectangle of the thumbnail, and the
//        // final bounds are the global visible rectangle of the container view. Also
//        // set the container view's offset as the origin for the bounds, since that's
//        // the origin for the positioning animation properties (X, Y).
//        thumbView.getGlobalVisibleRect(mStartBounds);
//        act.findViewById(R.id.container).getGlobalVisibleRect(finalBounds, globalOffset);
//
//        mStartBounds.offset(-globalOffset.x, -globalOffset.y);
//        finalBounds.offset(-globalOffset.x, -globalOffset.y);
//
//        // Adjust the start bounds to be the same aspect ratio as the final bounds using the
//        // "center crop" technique. This prevents undesirable stretching during the animation.
//        // Also calculate the start scaling factor (the end scaling factor is always 1.0).
//        float startScale;
//        if ((float) finalBounds.width() / finalBounds.height()
//                > (float) mStartBounds.width() / mStartBounds.height()) {
//            // Extend start bounds horizontally
//            startScale = (float) mStartBounds.height() / finalBounds.height();
//            float startWidth = startScale * finalBounds.width();
//            float deltaWidth = (startWidth - mStartBounds.width()) / 2;
//            mStartBounds.left -= deltaWidth;
//            mStartBounds.right += deltaWidth;
//        } else {
//            // Extend start bounds vertically
//            startScale = (float) mStartBounds.width() / finalBounds.width();
//            float startHeight = startScale * finalBounds.height();
//            float deltaHeight = (startHeight - mStartBounds.height()) / 2;
//            mStartBounds.top -= deltaHeight;
//            mStartBounds.bottom += deltaHeight;
//        }
//
//        // Hide the thumbnail and show the zoomed-in view. When the animation begins,
//        // it will position the zoomed-in view in the place of the thumbnail.
//        thumbView.setAlpha(0f);
//        mExpandedImageView.setVisibility(View.VISIBLE);
//
//        // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of
//        // the zoomed-in view (the default is the center of the view).
//        mExpandedImageView.setPivotX(0f);
//        mExpandedImageView.setPivotY(0f);
//
//        // Construct and run the parallel animation of the four translation and scale properties
//        // (X, Y, SCALE_X, and SCALE_Y).
//        
//        // Construct and run the parallel animation of the four translation and scale properties
//        // (X, Y, SCALE_X, and SCALE_Y).
//        AnimatorSet set = new AnimatorSet();
//        set.play(ObjectAnimator.ofFloat(mExpandedImageView, View.X, mStartBounds.left,finalBounds.left))
//           .with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y, mStartBounds.top,finalBounds.top))
//           .with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_X, startScale, 1f))
//           .with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_Y, startScale, 1f));
//        set.setDuration(mShortAnimationDuration);
//        set.setInterpolator(new DecelerateInterpolator());
//        set.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                mCurrentAnimator = null;
//            }
//
//            @Override
//            public void onAnimationCancel(Animator animation) {
//                mCurrentAnimator = null;
//            }
//        });
//        set.start();
//        mCurrentAnimator = set;
//
//        // Upon clicking the zoomed-in image, it should zoom back down to the original bounds
//        // and show the thumbnail instead of the expanded image.
//        mStartScaleFinal = startScale;
//        mExpandedImageView.setOnClickListener(new View.OnClickListener() 
//        {
//            @Override
//            public void onClick(View view) 
//            {
//            	closeExpandImage();
//            }
//        });
//        
//    }

		

	public static void showImage(ImageView mPicImageView,
						  String pictureUri,
						  final Activity mAct) throws IOException 
   {
		// show dialog for confirmation Continue or not
		mAddNewAct = mAct;

		// retrieve display dimensions
		Rect displayRectangle = new Rect();
		Window window = mAct.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

		mExpandedImageView = mPicImageView;

		// If there's an animation in progress, cancel it immediately and
		// proceed with this one.
		if (mCurrentAnimator != null) {
			mCurrentAnimator.cancel();
		}

		// Retrieve and cache the system's default "short" animation time.
		mShortAnimationDuration = mAct.getResources().getInteger(
				android.R.integer.config_shortAnimTime);

		Uri imageUri = Uri.parse(pictureUri);
		String scheme = Util.getUriScheme(pictureUri);
		System.out.println(" UtilImage / showImage / imageUri = " + imageUri);

		// for different scheme
		String filePath = null;
		if (imageUri != null && scheme.equals("content")) 
		{
			String[] projection = new String[] { android.provider.MediaStore.Images.ImageColumns.DATA };
			Cursor cursor = mAct.getContentResolver().query(imageUri,
					projection, null, null, null);
			cursor.moveToFirst();
			filePath = cursor.getString(0);
			cursor.close();
		} else 
		{
			filePath = imageUri.getPath();
		}

		// System.out.println("filePath = " + filePath);
		ExifInterface exif = new ExifInterface(filePath);
		String length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
		String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);

//		System.out.println(String.format(Locale.US, "width = %s length = %s",width, length));

		// makes sure EXIF data
		if( (Integer.valueOf(length) > 0 ) && (Integer.valueOf(length) > 0))
		{
			// set picture
			mExpandedImageView.setVisibility(View.VISIBLE);
			bShowExpandedImage = true;			
			mExpandedImageView.setImageBitmap(decodeSampledBitmapFromUri(imageUri,
																		 Integer.valueOf(width) / 5,
																		 Integer.valueOf(length) / 5,
																		 mAct));
		}
		else
		{
			mExpandedImageView.setVisibility(View.INVISIBLE);
			bShowExpandedImage = false;
			return;
		}

		// Calculate the starting and ending bounds for the zoomed-in image.
		// This step
		// involves lots of math. Yay, math.
		mStartBounds = new Rect();
		final Rect finalBounds = new Rect();
		final Point globalOffset = new Point();

		// The start bounds are the global visible rectangle of the thumbnail,
		// and the
		// final bounds are the global visible rectangle of the container view.
		// Also
		// set the container view's offset as the origin for the bounds, since
		// that's
		// the origin for the positioning animation properties (X, Y).

		mAct.findViewById(R.id.container).getGlobalVisibleRect(finalBounds,
				globalOffset);


		mStartBounds.offset(-globalOffset.x, -globalOffset.y);
		finalBounds.offset(-globalOffset.x, -globalOffset.y);

		// Adjust the start bounds to be the same aspect ratio as the final
		// bounds using the
		// "center crop" technique. This prevents undesirable stretching during
		// the animation.
		// Also calculate the start scaling factor (the end scaling factor is
		// always 1.0).
		float startScale;
		if ((float) finalBounds.width() / finalBounds.height() > (float) mStartBounds
				.width() / mStartBounds.height()) 
		{
			// Extend start bounds horizontally
			startScale = (float) mStartBounds.height() / finalBounds.height();
			float startWidth = startScale * finalBounds.width();
			float deltaWidth = (startWidth - mStartBounds.width()) / 2;
			mStartBounds.left -= deltaWidth;
			mStartBounds.right += deltaWidth;
		} else 
		{
			// Extend start bounds vertically
			startScale = (float) mStartBounds.width() / finalBounds.width();
			float startHeight = startScale * finalBounds.height();
			float deltaHeight = (startHeight - mStartBounds.height()) / 2;
			mStartBounds.top -= deltaHeight;
			mStartBounds.bottom += deltaHeight;
		}

		// Hide the thumbnail and show the zoomed-in view. When the animation
		// begins,
		// it will position the zoomed-in view in the place of the thumbnail.
//		thumbView.setAlpha(0f);
		mExpandedImageView.setVisibility(View.VISIBLE);

		// Set the pivot point for SCALE_X and SCALE_Y transformations to the
		// top-left corner of
		// the zoomed-in view (the default is the center of the view).
		mExpandedImageView.setPivotX(0f);
		mExpandedImageView.setPivotY(0f);

		// Construct and run the parallel animation of the four translation and
		// scale properties
		// (X, Y, SCALE_X, and SCALE_Y).

		// Construct and run the parallel animation of the four translation and
		// scale properties
		// (X, Y, SCALE_X, and SCALE_Y).
		AnimatorSet set = new AnimatorSet();
		set.play(
				ObjectAnimator.ofFloat(mExpandedImageView, View.X,
						mStartBounds.left, finalBounds.left))
				.with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y,
						mStartBounds.top, finalBounds.top))
				.with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_X,
						startScale, 1f))
				.with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_Y,
						startScale, 1f));
		set.setDuration(mShortAnimationDuration);
		set.setInterpolator(new DecelerateInterpolator());
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mCurrentAnimator = null;
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				mCurrentAnimator = null;
			}
		});
		set.start();
		mCurrentAnimator = set;

		// Upon clicking the zoomed-in image, it should zoom back down to the
		// original bounds
		// and show the thumbnail instead of the expanded image.
		mStartScaleFinal = startScale;
		mExpandedImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
//				closeExpandedImage();
			}
		});

   }


    public static void closeExpandedImage()
    {
    	System.out.println("closeExpandImage");
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Animate the four positioning/sizing properties in parallel, back to their
        // original values.
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(mExpandedImageView, View.X, mStartBounds.left))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y, mStartBounds.top))
                .with(ObjectAnimator
                        .ofFloat(mExpandedImageView, View.SCALE_X, mStartScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(mExpandedImageView, View.SCALE_Y, mStartScaleFinal));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            	if(mThumbView != null)
            		mThumbView.setAlpha(1f);
                mExpandedImageView.setVisibility(View.GONE);
            	bShowExpandedImage = false;
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if(mThumbView != null)
                	mThumbView.setAlpha(1f);
                mExpandedImageView.setVisibility(View.GONE);
            	bShowExpandedImage = false;
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
    }
    
    public static Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight, Activity mAct) throws IOException 
    {
    	Bitmap thumbNail;
    	ContentResolver cr = mAct.getContentResolver();
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
		thumbNail = BitmapFactory.decodeStream(cr.openInputStream(uri), null, options);
		
		if(thumbNail != null)
		{
			// scaled
			thumbNail = Bitmap.createScaledBitmap(thumbNail, reqWidth,reqHeight, true);
			// rotate bitmap
			thumbNail = Bitmap.createBitmap(thumbNail, 0, 0, reqWidth, reqHeight, getMatrix(uri), true);
		}

		return thumbNail;
    }
    
    public static Matrix getMatrix(Uri imageUri) throws IOException
    {
		Matrix matrix = new Matrix();

		ExifInterface exif = new ExifInterface(imageUri.getPath());
		int rotSetting = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
		int rotInDeg;
		
	    if (rotSetting == ExifInterface.ORIENTATION_ROTATE_90) 
	      rotInDeg = 90;  
	    else if(rotSetting == ExifInterface.ORIENTATION_ROTATE_180) 
	      rotInDeg = 180;  
	    else if (rotSetting == ExifInterface.ORIENTATION_ROTATE_270) 
	      rotInDeg = 270;
	    else
	      rotInDeg =0; 
		
	    System.out.println("rotInDeg = " + rotInDeg);
	    
		if (rotSetting != 0f) {matrix.preRotate(rotInDeg);}
    	
		return matrix;
    }
    
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int width = options.outWidth;
        final int height = options.outHeight;
        
//        System.out.println("bitmap height = " + height);
//        System.out.println("bitmap width = " + width);
        
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }
        
        return inSampleSize;
    }
    
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
    
    // Get default scale
    public static float getDefaultSacle(Activity act)
    {
        // scale = (dpi / 160),
        // dpi:dots per inch
        int dpi = (int)act.getResources().getDisplayMetrics().densityDpi;
//        System.out.println("= densityDpi = " + dpi);
        float scale = dpi/160.0f;
//        System.out.println("= default scale = " + scale);
        return scale;
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
    
    // set icon bitmap on a thumbnail bitmap 
    public static Bitmap setIconOnThumbnail(Bitmap bmpThumb, Bitmap bmpIcon, int iconSize)
    {
		Bitmap bmpOverlay = null;
		if(bmpThumb != null)
		{
			// draw base bitmap
	        bmpOverlay = Bitmap.createBitmap(bmpThumb.getWidth(), bmpThumb.getHeight(), bmpThumb.getConfig());
	        Canvas canvas = new Canvas(bmpOverlay);
	        canvas.drawBitmap(bmpThumb, new Matrix(), null);
			
	        // prepare icon bitmap
	        bmpIcon = Bitmap.createScaledBitmap(bmpIcon, iconSize, iconSize, true);
	        float hCenter =(float)((canvas.getWidth() - bmpIcon.getWidth())/2);
	        float vCenter =(float)((canvas.getHeight() - bmpIcon.getHeight())/2);
	        
	        // draw overlay bitmap
	        canvas.drawBitmap(bmpIcon, hCenter, vCenter, null);
		}
        return bmpOverlay;
    }
    
    // Get image directory
    public static File getImageDirectoryByContentId(Context context,int id)
    {
    	Cursor imageCursor = getImageContentCursorByContentId(context,id);
	    String path = null;
	    if (imageCursor.getCount() > 0) 
	    {
	    	imageCursor.moveToFirst(); // newest one
	    	path = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
	    }
	    System.out.println("UtilImage / _getImageDirectoryByContentId / image dir path = " + path);
	    
	    File dir = new File(path);
	    dir = new File(dir.getParent());
	    return dir;
    }
   
    
    public static Cursor getImageContentCursorByContentId(Context context,int id)
    {
	    final String[] projection = {MediaStore.Images.ImageColumns.DATA,
									 MediaStore.Images.ImageColumns.DATE_TAKEN,
									 MediaStore.Images.ImageColumns.SIZE,
									 MediaStore.Images.ImageColumns._ID};
	    final String imageWhere = MediaStore.Images.Media._ID + "=?";
	    final String[] imageArguments = {Integer.toString(id)};
	    final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";

	    Cursor imageCursor = context.getContentResolver()
								 	.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
										projection,
										imageWhere,
										imageArguments,
										imageOrderBy);
	    return imageCursor;
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

	public static void setImageBitmapTest(Activity act,ImageView imageView)
	{
		 Bitmap bitmap = BitmapFactory.decodeResource(act.getResources(), R.drawable.ic_media_play);
		 if(bitmap != null)
			 imageView.setImageBitmap(bitmap);
	}

	// Some cameras can add rotation data to EXIF, but others just add normal setting to EXIF
	public static boolean isLandscapePicture(String path)
	{
//		System.out.println("UtilImage / isLandscapePicture / path = " +path);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path.replace("file://",""));
        } catch (IOException e) {
            e.printStackTrace();
        }

        exif = null;

        boolean  isLandscape = true; //default setting if no exif

        if(exif != null) {
			int rotSetting = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			System.out.println("UtilImage / isLandscapePicture / rotSetting = " +rotSetting);

			if ( (rotSetting == ExifInterface.ORIENTATION_ROTATE_90) ||
				 (rotSetting == ExifInterface.ORIENTATION_ROTATE_270)   )
			{
				isLandscape = false;
			}
			else if( (rotSetting == ExifInterface.ORIENTATION_ROTATE_180) ||
					(rotSetting == ExifInterface.ORIENTATION_NORMAL)        )
			{
				// some device can not use width and height to determine landscape
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;

				BitmapFactory.decodeFile(path.replace("file://", ""), options);
				int width = options.outWidth;
				System.out.println("UtilImage / isLandscapePicture / width = " + width);
				int height = options.outHeight;
				System.out.println("UtilImage / isLandscapePicture / height = " + height);

				isLandscape=(width > height)?true:false;
			}
		}

		return isLandscape;
	}
}