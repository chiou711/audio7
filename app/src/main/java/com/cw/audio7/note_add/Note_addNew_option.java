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

package com.cw.audio7.note_add;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.RadioGroup;

import com.cw.audio7.R;

public class Note_addNew_option
{
	private RadioGroup mRadioGroup0;
    private CheckedTextView check_add_folder_if_exists;
    private AlertDialog mDialog = null;
    private SharedPreferences mPref_add_new_note_location;
    private boolean bAddToTop, bAddFolder;

	public Note_addNew_option(final Activity activity)
	{
		mPref_add_new_note_location = activity.getSharedPreferences("add_new_note_option", 0);
  		// inflate select style layout
  		LayoutInflater inflater;
  		inflater= (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  		View view = inflater.inflate(R.layout.note_add_new_option, null);

		mRadioGroup0 = (RadioGroup)view.findViewById(R.id.radioGroup_new_at);
		check_add_folder_if_exists = (CheckedTextView)view.findViewById(R.id.check_add_folder_if_exists);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	
		builder.setTitle(R.string.dialog_add_new_option_title)
			.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
	        {	@Override
	    		public void onClick(DialogInterface dialog, int which) {
	    			//cancel
	    		}
	        });
		
		builder.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener()
        {	@Override
    		public void onClick(DialogInterface dialog, int which)
        	{
        		respondToSelection();
			}
        });

		// add to top: init
		if(mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top"))
		{
			mRadioGroup0.check(mRadioGroup0.getChildAt(0).getId());
			bAddToTop = true;
		}
		else if (mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("bottom"))
		{
			mRadioGroup0.check(mRadioGroup0.getChildAt(1).getId());
			bAddToTop = false;
		}

        // add to top: listener
        mRadioGroup0.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup RG, int id) {
                bAddToTop = (mRadioGroup0.indexOfChild(mRadioGroup0.findViewById(id))==0)?true:false;
            }
        });

		// add all directory: init
		if (mPref_add_new_note_location.getString("KEY_ADD_DIRECTORY","no").equalsIgnoreCase("yes") )
		{
			check_add_folder_if_exists.setChecked(true);
			bAddFolder = true;
		}
		else if (mPref_add_new_note_location.getString("KEY_ADD_DIRECTORY","no").equalsIgnoreCase("no") )
		{
			check_add_folder_if_exists.setChecked(false);
			bAddFolder = false;
		}

		//  add all directory: listener
        check_add_folder_if_exists.setOnClickListener(new View.OnClickListener()
        {	@Override
        public void onClick(View view)
        {
            boolean currentCheck = ((CheckedTextView)view).isChecked();
            ((CheckedTextView)view).setChecked(!currentCheck);

            if(((CheckedTextView)view).isChecked())
                bAddFolder = true;
            else
                bAddFolder = false;
        }
        });


		builder.setView(view);
  		mDialog = builder.create();
  		mDialog.show();
	}
	
	// respond to selection
	void respondToSelection()
	{
		if(bAddToTop)
			mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_NOTE_TO", "top").apply();
		else
			mPref_add_new_note_location.edit().putString("KEY_ADD_NEW_NOTE_TO", "bottom").apply();

		if(bAddFolder)
			mPref_add_new_note_location.edit().putString("KEY_ADD_DIRECTORY", "yes").apply();
		else
			mPref_add_new_note_location.edit().putString("KEY_ADD_DIRECTORY", "no").apply();

	}
}