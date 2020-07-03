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

package com.cw.audio7.util;

import java.lang.reflect.InvocationTargetException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    float scale = 0f;
    float oldScale = 0f;
    int displayHeight;
    
    SharedPreferences mPref_web_view;
    Context mContext;
    public CustomWebView(Context context) {
        super(context);
        mContext = context;
    }    
    
    public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

	@Override
	public boolean performClick() {
		return super.performClick();
	}

	@Override
    public boolean onTouchEvent(MotionEvent ev) 
    {
        boolean consumed = super.onTouchEvent(ev);

        if (isClickable()) {
	        performClick();

	        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		        case MotionEvent.ACTION_DOWN:
			        start.set(ev.getX(), ev.getY());
			        mode = DRAG;
			        break;
		        case MotionEvent.ACTION_UP:
		        case MotionEvent.ACTION_POINTER_UP:
			        mode = NONE;
			        break;
		        case MotionEvent.ACTION_POINTER_DOWN:
			        oldDist = spacing(ev);
			        if (oldDist > 5f) {
				        midPoint(mid, ev);
				        mode = ZOOM;
			        }
			        break;

		        case MotionEvent.ACTION_MOVE:
			        if (mode == DRAG) {
			        } else if (mode == ZOOM) {
				        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
					        float newDist = spacing(ev);
					        if (newDist > 5f) {
						        scale = newDist / oldDist;
						        if (scale > 1) {
							        if (Math.abs(oldScale - scale) > 0.3) {
								        zoomIn();
								        oldScale = scale;
							        }
						        }
						        if (scale < 1) {
							        if (getContentHeight() * getScale() > displayHeight)
								        zoomOut();
						        }

						        int newDefaultScale = (int) (getScale() * 100);
						        mPref_web_view = mContext.getSharedPreferences("web_view", 0);
						        mPref_web_view.edit().putInt("KEY_WEB_VIEW_SCALE", newDefaultScale).apply();
					        }
				        }
			        }
			        break;
	        }// switch
        }// if
        return consumed;
    }

    private float spacing(MotionEvent event) {
           float x = event.getX(0) - event.getX(1);
           float y = event.getY(0) - event.getY(1);
           return (float) Math.sqrt(x * x + y * y);
        }

    private void midPoint(PointF point, MotionEvent event) {
       float x = event.getX(0) + event.getX(1);
       float y = event.getY(0) + event.getY(1);
       point.set(x / 2, y / 2);
    }

	// Pause Web view
	public static void pauseWebView(CustomWebView webView)
	{
//		System.out.println("CustomWebView / _closeWebView");
		if( webView!= null)
		{
			try {
		        Class.forName("android.webkit.WebView")
		             .getMethod("onPause", (Class[]) null)
		             .invoke(webView, (Object[]) null);
		    } catch(ClassNotFoundException cnfe) {
		        System.out.println("ClassNotFoundException");
		    } catch(NoSuchMethodException nsme) {
		        System.out.println("NoSuchMethodException");
		    } catch(InvocationTargetException ite) {
		        System.out.println("InvocationTargetException");
		    } catch (IllegalAccessException iae) {
		        System.out.println("IllegalAccessException");
		    }	
			
			webView.onPause();
			webView.onResume();
			webView.clearCache(true);
		}  
	}

	public static void blankWebView(CustomWebView webView)
	{
		if(webView != null)
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
				webView.loadUrl("about:blank");
			else
				webView.clearView();
			webView.setVisibility(View.GONE);
		}
	}
}