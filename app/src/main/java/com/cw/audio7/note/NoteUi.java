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

package com.cw.audio7.note;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.image.UtilImage;
import com.cw.audio7.util.Util;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

public class NoteUi
{
    public static boolean showSeekBarProgress;
    private ViewPager pager;
    private AppCompatActivity act;

    public static int getNotesCnt() {
        return notesCnt;
    }

    public static void setNotesCnt(int notesCnt) {
        NoteUi.notesCnt = notesCnt;
    }

    public static int notesCnt;

    // getter and setter of focus note position
    public static int mFocus_notePos;

    public static int getFocus_notePos() {
        return mFocus_notePos;
    }

    public static void setFocus_notePos(int Pos) {
        mFocus_notePos = Pos;
    }


    // constructor
    public NoteUi(AppCompatActivity activity, ViewPager viewPager, int position)
    {

        System.out.println("NoteUi / constructor");
        pager = viewPager;
        act = activity;

	    DB_page db_page = new DB_page(act,TabsHost.getCurrentPageTableId());
        setNotesCnt(db_page.getNotesCount(true));
        String pictureUri = db_page.getNotePictureUri(position,true);
        String linkUri = db_page.getNoteLinkUri(position,true);

        String tagStr = "current"+ position +"pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);

        if((pictureGroup != null))
        {
            TextView picView_footer = (TextView) (pictureGroup.findViewById(R.id.image_footer));

            Button picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
            Button picView_viewMode_button = (Button) (pictureGroup.findViewById(R.id.image_view_mode));

            // show back button
            if(Note.isPictureMode())
                picView_back_button.setVisibility(View.VISIBLE);
            else
                picView_back_button.setVisibility(View.GONE);

            // Show picture title
            TextView picView_title;
            picView_title = (TextView) (pictureGroup.findViewById(R.id.image_title));
            String pictureName;
            if(!Util.isEmptyString(pictureUri))
                pictureName = Util.getDisplayNameByUriString(pictureUri, act);
            else
                pictureName = "";

            if(!Util.isEmptyString(pictureName))
            {
                picView_title.setVisibility(View.VISIBLE);
                picView_title.setText(pictureName);
            }
            else
                picView_title.setVisibility(View.INVISIBLE);

            // show footer
            if(Note.isPictureMode()) {
                picView_footer.setVisibility(View.VISIBLE);
                picView_footer.setText((pager.getCurrentItem()+1) +
                        "/" + pager.getAdapter().getCount());
            }
            else
                picView_footer.setVisibility(View.GONE);

            // set image view buttons (View Mode, Previous, Next) visibility
            if(Note.isPictureMode() )
            {
                picView_viewMode_button.setVisibility(View.VISIBLE);
            }
            else
            {
                show_picViewUI_previous_next(false,0);
                picView_viewMode_button.setVisibility(View.GONE);
            }

            showSeekBarProgress = true;
        }
    } //Note_view_UI constructor

    static PopupMenu popup;
    private static String mPictureString;

	public void tempShow_picViewUI(long delayTime, String pictureStr)
	{
		System.out.println("NoteUi / _tempShow_picViewUI / delayTime = " + delayTime);
        mPictureString = pictureStr;
        handler = new Handler();
        handler.postDelayed(runnableHideUi,delayTime);
    }

    Handler handler;
    public Runnable runnableHideUi = new Runnable()
    {
        public void run()
        {
            System.out.println("NoteUi / _runnableHideUi ");
            if(pager != null)
            {
                int position =  pager.getCurrentItem();
                String tagImageStr = "current"+ position +"pictureView";
                System.out.println("NoteUi / _runnableHideUi / position = " + position);
                ViewGroup imageGroup = (ViewGroup) pager.findViewWithTag(tagImageStr);

                if(imageGroup != null)
                {
                    // to distinguish image and video, does not show video play icon
                    // only when video is playing
                    if(!Util.isEmptyString(mPictureString))
                        hide_picViewUI(mPictureString);
                }
                showSeekBarProgress = false;
            }
        }
    };

	void hide_picViewUI(String pictureStr)
	{
        String tagStr = "current"+ pager.getCurrentItem() +"pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);

        System.out.println("NoteUi / _hide_picViewUI / tagStr = " + tagStr);

        if((pictureGroup != null))
        {
            // image view
            TextView picView_title = (TextView) (pictureGroup.findViewById(R.id.image_title));
            TextView picView_footer = (TextView) (pictureGroup.findViewById(R.id.image_footer));

            Button picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
            Button picView_viewMode_button = (Button) (pictureGroup.findViewById(R.id.image_view_mode));
            Button picView_previous_button = (Button) (pictureGroup.findViewById(R.id.image_view_previous));
            Button picView_next_button = (Button) (pictureGroup.findViewById(R.id.image_view_next));

            picView_title.setVisibility(View.GONE);
            picView_footer.setVisibility(View.GONE);

            picView_back_button.setVisibility(View.GONE);

            // view mode button visibility affects pop up menu ON/OFF
            picView_viewMode_button.setVisibility(View.GONE);

            if(Note.isPictureMode() && UtilImage.hasImageExtension(pictureStr, act))
            {
                if(picView_previous_button != null) {
                    picView_previous_button.setVisibility(View.GONE);
                    picView_next_button.setVisibility(View.GONE);
                }
            }

        }

        cancel_UI_callbacks();
	}
	
    static void cancel_UI_callbacks()
    {
        if(Note.picUI_touch != null) {
            if(Note.picUI_touch.handler != null)
                Note.picUI_touch.handler.removeCallbacks(Note.picUI_touch.runnableHideUi);
            Note.picUI_touch = null;
        }

        if(Note_adapter.picUI_primary != null) {
            if(Note_adapter.picUI_primary.handler != null)
                Note_adapter.picUI_primary.handler.removeCallbacks(Note_adapter.picUI_primary.runnableHideUi);
            Note_adapter.picUI_primary = null;
        }
    }

    private void show_picViewUI_previous_next(boolean show, int position) {
        String tagStr = "current" + position + "pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);
//        System.out.println("NoteUi / _show_PicViewUI_previous_next / tagStr = " + tagStr);

        Button picView_previous_button;
        Button picView_next_button;

        if (pictureGroup != null) {
            picView_previous_button = (Button) (pictureGroup.findViewById(R.id.image_view_previous));
            picView_next_button = (Button) (pictureGroup.findViewById(R.id.image_view_next));

            if (show) {
                picView_previous_button.setVisibility(View.VISIBLE);
                picView_previous_button.setEnabled(position != 0);
                picView_previous_button.setAlpha(position == 0 ? 0.1f : 1f);

                picView_next_button.setVisibility(View.VISIBLE);
                picView_next_button.setAlpha(position == (Note.mPagerAdapter.getCount() - 1) ? 0.1f : 1f);
                picView_next_button.setEnabled(position != (Note.mPagerAdapter.getCount() - 1));
            } else {
                picView_previous_button.setVisibility(View.GONE);
                picView_next_button.setVisibility(View.GONE);
            }
        }
    }

}
