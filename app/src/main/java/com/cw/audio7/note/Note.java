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

package com.cw.audio7.note;

import com.cw.audio7.audio.AudioUi_note;
import com.cw.audio7.main.MenuId;
import com.cw.audio7.note_edit.Note_edit;
import com.cw.audio7.R;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.audio.Audio_manager;
import com.cw.audio7.audio.BackgroundAudioService;
import com.cw.audio7.page.PageAdapter;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;
import com.cw.audio7.util.uil.UilCommon;
import com.cw.audio7.util.Util;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static com.cw.audio7.main.MainAct.audio_manager;
import static com.cw.audio7.main.MainAct.mFolderUi;

public class Note extends Fragment
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public ViewPager viewPager;
    public static boolean isPagerActive;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    public static PagerAdapter mPagerAdapter;

    // DB
    public DB_page mDb_page;
    public static Long mNoteId;
    int mEntryPosition;
    int EDIT_CURRENT_VIEW = 5;
    static int mStyle;
    
    static SharedPreferences mPref_show_note_attribute;

	public static String mAudioUriInDB;

    public AppCompatActivity act;
    public AudioUi_note audioUi_note;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		System.out.println("Note/ _onCreate");
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		mEntryPosition = arguments.getInt("POSITION");
		NoteUi.setFocus_notePos(mEntryPosition);

		DB_page db_page = new DB_page(getActivity(),mFolderUi.tabsHost.getCurrentPageTableId());
		NoteUi.setNotesCnt(db_page.getNotesCount(true));

		act = (AppCompatActivity) getActivity();
		setHasOptionsMenu(true);

		// force stop audio whenever user touch page mode thumb nail
		audio_manager.stopAudioPlayer(act);

		mFolderUi.tabsHost.audio7Player = null;
	}

	public View rootView;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(Util.isLandscapeOrientation(act))
			rootView = inflater.inflate(R.layout.note_view_landscape, container, false);
		else
			rootView = inflater.inflate(R.layout.note_view_portrait, container, false);

		return rootView;
	}

	Menu mMenu;
	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater) {
		System.out.println("Note / _onCreateOptionsMenu");
		menu.clear();

		inflater.inflate(R.menu.pager_menu, menu);
		mMenu = menu;

		// menu item: checked status
		// get checked or not
		int isChecked = mDb_page.getNoteMarking(NoteUi.getFocus_notePos(),true);
		if( isChecked == 0)
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
		else
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		mMenu.findItem(R.id.VIEW_NOTE_CHECK).setVisible(true);
		super.onPrepareOptionsMenu(menu);
	}

	void setLayoutView()
	{
        System.out.println("Note / _setLayoutView");

		mPref_show_note_attribute = act.getSharedPreferences("show_note_attribute", 0);

		UilCommon.init();

		// DB
		DB_folder dbFolder = new DB_folder(act,Pref.getPref_focusView_folder_tableId(act));

		if(mFolderUi.tabsHost != null) {
			mStyle = dbFolder.getPageStyle(mFolderUi.tabsHost.getFocus_tabPos(), true);
			mDb_page = new DB_page(act, mFolderUi.tabsHost.getCurrentPageTableId());

			if (mDb_page != null) {
				mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
				mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
			}
		}

		if (UtilAudio.hasAudioExtension(mAudioUriInDB) ||
				UtilAudio.hasAudioExtension(Util.getDisplayNameByUriString(mAudioUriInDB, act)[0])) {
			// create new instance after rotation
			audioUi_note = new AudioUi_note(act, rootView, mAudioUriInDB);
		}

		// Instantiate a ViewPager and a PagerAdapter.

		if(viewPager == null) {
			viewPager = (ViewPager) rootView.findViewById(R.id.tabs_pager);
			mPagerAdapter = new Note_adapter(viewPager, audioUi_note, act);
			viewPager.setAdapter(mPagerAdapter);
			viewPager.setCurrentItem(NoteUi.getFocus_notePos());

			// Note: if viewPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
			//       be called again after rotation
			viewPager.addOnPageChangeListener(onPageChangeListener);
		}

	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{
			if(audio_manager.getAudioPlayMode()  == audio_manager.NOTE_PLAY_MODE) {
				System.out.println("Note / onPageSelected / stop audio" );
				audio_manager.stopAudioPlayer(act);
			}

			NoteUi.setFocus_notePos(viewPager.getCurrentItem());
			System.out.println("Note / _onPageSelected");
//			System.out.println("    NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());
//			System.out.println("    nextPosition = " + nextPosition);

			// show audio name
			mNoteId = mDb_page.getNoteId(nextPosition,true);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
			System.out.println("Note / _onPageSelected /  mNoteId = " + mNoteId
					+ ", mAudioUriInDB = " + mAudioUriInDB);

			if(UtilAudio.hasAudioExtension(mAudioUriInDB)) {
				if(audioUi_note == null)
                    audioUi_note = new AudioUi_note(act, rootView,mAudioUriInDB);
            }

			setOutline(act);
		}
	};

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode,resultCode,data);
		System.out.println("Note / _onActivityResult ");
        if((requestCode==EDIT_CURRENT_VIEW) )
        {
			stopNoteAudio();
        }

		getActivity().recreate();
	}

    /** Set outline for selected view mode
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public void setOutline(AppCompatActivity act)
	{
        // Set full screen or not, and action bar
		Util.setFullScreen_noImmersive(act);
        if(act.getSupportActionBar() != null)
		    act.getSupportActionBar().show();

        // renew pager
        showSelectedView();

		TextView audioTitle = (TextView) rootView.findViewById(R.id.audio_title);
        // audio title
        if(!Util.isEmptyString(audioTitle.getText().toString()) )
            audioTitle.setVisibility(View.VISIBLE);
        else
            audioTitle.setVisibility(View.GONE);

        // renew options menu
        act.invalidateOptionsMenu();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note / _onConfigurationChanged");

	    // case 1: restart current fragment to apply orientation change
//		getFragmentManager()
//				.beginTransaction()
//				.detach(Note.this)
//				.attach(Note.this)
//				.commit();

		// case 2: just layout view change
		setLayoutView();

	}

	@Override
	public void onResume() {
		super.onResume();
		System.out.println("Note / _onResume");

		setLayoutView();

		isPagerActive = true;

		Note.setViewAllMode();

		setOutline(act);

		// Register Bluetooth device receiver
		if(Build.VERSION.SDK_INT < 21)
		{
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
			act.registerReceiver(mReceiver, filter);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		System.out.println("Note / _onPause");

		isPagerActive = false;

		// disable full screen
		Util.setNormalScreen(act);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("Note / _onDestroy");
	}

    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
				stopNoteAudio();
                return true;

	        case MenuId.VIEW_NOTE_CHECK:
		        int markingNow = PageAdapter.toggleNoteMarking(act,NoteUi.getFocus_notePos());

		        // update marking
		        if(markingNow == 1)
			        mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
		        else {
			        mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
			        stopNoteAudio();
			        mFolderUi.tabsHost.audio7Player.setAudioPanel(audioUi_note.audioPanel);
			        mFolderUi.tabsHost.audio7Player.initAudioBlock(mAudioUriInDB);
			        mFolderUi.tabsHost.audio7Player.updateAudioPanel(act);
		        }
		        return true;

	        case MenuId.VIEW_NOTE_EDIT:
		        Intent intent = new Intent(act, Note_edit.class);
		        intent.putExtra(DB_page.KEY_NOTE_ID, mNoteId);
		        intent.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(mNoteId));
		        intent.putExtra(DB_page.KEY_NOTE_AUDIO_URI , mDb_page.getNoteAudioUri_byId(mNoteId));
		        intent.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(mNoteId));
		        startActivityForResult(intent, EDIT_CURRENT_VIEW);
		        return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Show selected view
    static void showSelectedView()
    {
   		Note_adapter.mLastPosition = -1;

    	if(mPagerAdapter != null)
    		mPagerAdapter.notifyDataSetChanged(); // will call Note_adapter / _setPrimaryItem
    }
    
    static void setViewAllMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","ALL")
		   						  .apply();
    }
    
	public void stopNoteAudio()
	{
		if(audio_manager.getAudioPlayMode() == audio_manager.NOTE_PLAY_MODE)
            audio_manager.stopAudioPlayer(act);
	}

	//The BroadcastReceiver that listens for bluetooth broadcasts
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			System.out.println("MainAct / _BroadcastReceiver / onReceive");
			String action = intent.getAction();
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				//Device is now connected
				Toast.makeText(act.getApplicationContext(), "ACTION_ACL_CONNECTED: device is " + device, Toast.LENGTH_LONG).show();
			}

			Intent intentReceive = intent;
			KeyEvent keyEvent = (KeyEvent) intentReceive.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if(keyEvent != null)
				onKeyDown( keyEvent.getKeyCode(),keyEvent);
		}
	};

	// key event: 1 from bluetooth device 2 when notification bar dose not shown
//	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int newPos;
		System.out.println("Note / _onKeyDown / keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
				if(viewPager.getCurrentItem() == 0)
					newPos = mPagerAdapter.getCount() - 1;//back to last one
				else
					newPos = NoteUi.getFocus_notePos()-1;

				NoteUi.setFocus_notePos(newPos);
				audio_manager.stopAudioPlayer(act);
				viewPager.setCurrentItem(newPos);

				BackgroundAudioService.mIsPrepared = false;
				BackgroundAudioService.mMediaPlayer = null;
				return true;

			case KeyEvent.KEYCODE_MEDIA_NEXT: //87
				if(viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))
					newPos = 0;
				else
					newPos = NoteUi.getFocus_notePos() + 1;

				NoteUi.setFocus_notePos(newPos);
				audio_manager.stopAudioPlayer(act);
				viewPager.setCurrentItem(newPos);

				BackgroundAudioService.mIsPrepared = false;
				BackgroundAudioService.mMediaPlayer = null;
				return true;

			case KeyEvent.KEYCODE_MEDIA_PLAY: //126
			case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
				audioUi_note.audio_play_btn.performClick();
				return true;

			case KeyEvent.KEYCODE_BACK:
//                onBackPressed();
				return true;

			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return true;

			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return true;

			case KeyEvent.KEYCODE_MEDIA_STOP:
				return true;
		}
		return false;
	}

}