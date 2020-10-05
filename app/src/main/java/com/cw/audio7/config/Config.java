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

package com.cw.audio7.config;


import java.io.File;
import java.util.Objects;

import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.audio.Audio_manager;
import com.cw.audio7.audio.BackgroundAudioService;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.BaseBackPressedListener;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;

import androidx.fragment.app.Fragment;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Config extends Fragment
{
	// style
	TextView mNewPageTVStyle;
	private int mStyle = 0;

	// vibration
	SharedPreferences mPref_vibration;
	TextView mTextViewVibration;

	private AlertDialog dialog;
	private Context mContext;
	private LayoutInflater mInflater;
	String[] mItemArray = new String[]{"1","2","3","4","5","6","7","8","9","10"};
	
	public Config(){}
	static View mRootView;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		System.out.println("================ Config / onCreateView ==================");

		mRootView = inflater.inflate(R.layout.config, container, false);

	    //Set text style
		setNewPageTextStyle();
		
		//Set vibration time length
		setVibrationTimeLength();

		//delete DB
		deleteDB_button();

		//recover all settings to default
		recover_all_settings_button();

		// set Back pressed listener
		((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(MainAct.mAct));

		return mRootView;
	}   	

	/**
	 *  select style
	 *  
	 */
	void setNewPageTextStyle()
	{
		// Get current style
		mNewPageTVStyle = (TextView)mRootView.findViewById(R.id.TextViewStyleSetting);
		View mViewStyle = mRootView.findViewById(R.id.setStyle);
		int iBtnId = Util.getNewPageStyle(getActivity());
		
		// set background color with current style 
		mNewPageTVStyle.setBackgroundColor(ColorSet.mBG_ColorArray[iBtnId]);
		mNewPageTVStyle.setText(mItemArray[iBtnId]);
		mNewPageTVStyle.setTextColor(ColorSet.mText_ColorArray[iBtnId]);
		
		mViewStyle.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectStyleDialog(v);
			}
		});
	}
	
	
	void selectStyleDialog(View view)
	{
		mContext = getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		
		builder.setTitle(R.string.config_set_style_title)
			   .setPositiveButton(R.string.btn_OK, listener_ok)
			   .setNegativeButton(R.string.btn_Cancel, null);
		
		// inflate select style layout
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = mInflater.inflate(R.layout.select_style, null);
		RadioGroup RG_view = (RadioGroup)view.findViewById(R.id.radioGroup1);
		
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio0),0);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio1),1);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio2),2);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio3),3);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio4),4);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio5),5);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio6),6);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio7),7);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio8),8);
		setButtonColor((RadioButton)RG_view.findViewById(R.id.radio9),9);
		
		builder.setView(view);

		RadioGroup radioGroup = (RadioGroup) RG_view.findViewById(R.id.radioGroup1);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) {
				mStyle = RG.indexOfChild(RG.findViewById(id));
		}});
		
		dialog = builder.create();
		dialog.show();
	}
	
    private void setButtonColor(RadioButton rBtn,int iBtnId)
    {
		rBtn.setBackgroundColor(ColorSet.mBG_ColorArray[iBtnId]);
		rBtn.setText(mItemArray[iBtnId]);
		rBtn.setTextColor(ColorSet.mText_ColorArray[iBtnId]);
		
		//set checked item
		if(iBtnId == Util.getNewPageStyle(mContext))
			rBtn.setChecked(true);
		else
			rBtn.setChecked(false);
    }
		   
    DialogInterface.OnClickListener listener_ok = new DialogInterface.OnClickListener()
   {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			SharedPreferences mPref_style = getActivity().getSharedPreferences("style", 0);
			mPref_style.edit().putInt("KEY_STYLE",mStyle).apply();
			// update the style selection directly
			mNewPageTVStyle.setBackgroundColor(ColorSet.mBG_ColorArray[mStyle]);
			mNewPageTVStyle.setText(mItemArray[mStyle]);
			mNewPageTVStyle.setTextColor(ColorSet.mText_ColorArray[mStyle]);
			//end
			dialog.dismiss();
		}
   };

	/**
	 *  select vibration time length
	 *  
	 */
	void setVibrationTimeLength()
	{
		//  set current
		mPref_vibration = getActivity().getSharedPreferences("vibration", 0);
		View viewVibration = mRootView.findViewById(R.id.vibrationSetting);
		mTextViewVibration = (TextView)mRootView.findViewById(R.id.TextViewVibrationSetting);
	    String strVibTime = mPref_vibration.getString("KEY_VIBRATION_TIME","25");
		if(strVibTime.equalsIgnoreCase("00"))
			mTextViewVibration.setText(getResources().getText(R.string.config_status_disabled).toString());
		else
			mTextViewVibration.setText(strVibTime +"ms");

		// Select new 
		viewVibration.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				selectVibrationLengthDialog();
			}
		});
	}

	void selectVibrationLengthDialog()
	{
		   final String[] items = new String[]{getResources().getText(R.string.config_status_disabled).toString(),
				   		    				"15ms","25ms","35ms","45ms"};
		   AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		   
		   String strVibTime = mPref_vibration.getString("KEY_VIBRATION_TIME","25");
		   
		   if(strVibTime.equalsIgnoreCase("00"))
		   {
			   items[0] = getResources().getText(R.string.config_status_disabled).toString() + " *";
		   }
		   else 
		   {
			   for(int i=1;i< items.length;i++)
			   {
				   if(strVibTime.equalsIgnoreCase((String) items[i].subSequence(0,2)))
					   items[i] += " *";
			   }
		   }
		   
		   DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
		   {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String len = null;
					
					if(which ==0)
						len = "00";
					else
						len = (String) items[which].subSequence(0,2);
					mPref_vibration.edit().putString("KEY_VIBRATION_TIME",len).apply();
					// change the length directly
					if(len.equalsIgnoreCase("00"))
						mTextViewVibration.setText(getResources().getText(R.string.config_status_disabled).toString());
					else
						mTextViewVibration.setText(len + "ms");					
					
					//end
					dialog.dismiss();
				}
		   };
		   builder.setTitle(R.string.config_set_vibration_title)
				  .setSingleChoiceItems(items, -1, listener)
				  .setNegativeButton(R.string.btn_Cancel, null)
				  .show();
	}

   
    /**
     * Delete DB
     *
     */
    public void deleteDB_button(){
	    View tvDelDB = mRootView.findViewById(R.id.SetDeleteDB);
	    tvDelDB.setOnClickListener(new OnClickListener() {
		   @Override
		   public void onClick(View v) {
			   confirmDeleteDB(v);
		   }
	   });
    }

	private void confirmDeleteDB(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.config_delete_DB_confirm_content)
			   .setPositiveButton(R.string.btn_OK, listener_delete_DB)
			   .setNegativeButton(R.string.btn_Cancel, null)
			   .show();
	}

    DialogInterface.OnClickListener listener_delete_DB = new DialogInterface.OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) {
			DB_drawer db_drawer = new DB_drawer(getActivity());
			db_drawer.deleteDB();

			// stop audio player
			if(BackgroundAudioService.mMediaPlayer != null)
				Audio_manager.stopAudioPlayer();

			//set last tab Id to 0, otherwise TabId will not start from 0 when deleting all
			//reset tab Index to 0
			//fix: select tab over next import amount => clean all => import => export => error
			TabsHost.setFocus_tabPos(0);
			FolderUi.setFocus_folderPos(0);

			// remove focus view folder table Id key
			Pref.removePref_focusView_folder_tableId_key(getActivity());

			//todo Add initial condition?

			dialog.dismiss();

			getActivity().finish();
			Intent intent  = new Intent(getActivity(),MainAct.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			getActivity().startActivity(intent);
		}
    };

	/**
	 * recover all settings to default
	 */
	public void recover_all_settings_button(){
		View recoverDefault = mRootView.findViewById(R.id.RecoverAllSettings);
		recoverDefault.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				confirmRecoverDefault(v);
			}
		});
	}


	private void confirmRecoverDefault(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.confirm_dialog_title)
				.setMessage(R.string.config_recover_all_settings)
				.setPositiveButton(R.string.btn_OK, listener_recover_default)
				.setNegativeButton(R.string.btn_Cancel, null)
				.show();
	}

	private DialogInterface.OnClickListener listener_recover_default = new DialogInterface.OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) {

			// stop audio player
			if(BackgroundAudioService.mMediaPlayer != null)
				Audio_manager.stopAudioPlayer();

			//remove preference
			clearSharedPreferencesForSettings(Objects.requireNonNull(getActivity()));

			dialog.dismiss();

            ((MainAct)getActivity()).getSupportFragmentManager()//??? warning
                    .beginTransaction()
                    .detach(Config.this)
//                    .attach(Config.this)
                    .commit();

			Intent new_intent = new Intent(getActivity(), MainAct.class);
			new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
			new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);

			getActivity().startActivity(new_intent);
		}
	};

    private static void clearSharedPreferencesForSettings(Context context)
	{
		File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");

		String[] children = dir.list();

		for (String child : children) {
			System.out.println("original: " + child);

			// EULA is using PreferenceManager.getDefaultSharedPreferences(MainAct.mAct)
			// it will create packageName_preferences.xml

			// clear each preferences XML file content, except DEFAULT shared preferences file
			if (!child.contains("preferences")) {
				context.getSharedPreferences(child.replace(".xml", ""), Context.MODE_PRIVATE)
						.edit().clear().apply();
				System.out.println("clear: " + child);
			}
		}

		// Make sure it has enough time to save all the committed changes
		try { Thread.sleep(1000); } catch (InterruptedException e) {}

		for (String child : children) {
			// delete the files
			if (!child.contains("preferences")) {
				new File(dir, child).delete();
				System.out.println("delete:" + " " + child);
			}
		}
    }
    
}