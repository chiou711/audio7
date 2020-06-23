package com.cw.audio7.page;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

// used for removing Accessibility warning: has setOnTouchListener called on it but does not override performClick
public class ImageViewCustom extends AppCompatImageView
{
    public ImageViewCustom(Context context){
        super(context);
    }

    public ImageViewCustom(Context context,AttributeSet attrs){
        super(context,attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                return true;

            case MotionEvent.ACTION_UP:
                performClick();
                return true;
        }
        return false;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
