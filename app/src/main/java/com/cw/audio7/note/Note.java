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

import com.cw.audio7.note_edit.Note_edit;
import com.cw.audio7.R;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.page.PageAdapter_recycler;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;
import com.cw.audio7.util.uil.UilCommon;
import com.cw.audio7.util.Util;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class Note extends AppCompatActivity
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

    Button editButton;
    Button backButton;

	public static String mAudioUriInDB;

    public AppCompatActivity act;
    public AudioUi_note audioUi_note;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note / _onCreate");

		// set current selection
		mEntryPosition = getIntent().getExtras().getInt("POSITION");
		NoteUi.setFocus_notePos(mEntryPosition);

		Audio_manager.isRunnableOn_note = false;

		act = this;

//        MainAct.mMediaBrowserCompat = null;

	} //onCreate end

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		System.out.println("Note / _onWindowFocusChanged");
	}

	// key event: 1 from bluetooth device 2 when notification bar dose not shown
	@Override
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
				viewPager.setCurrentItem(newPos);

				BackgroundAudioService.mIsPrepared = false;
				BackgroundAudioService.mMediaPlayer = null;
				Audio_manager.isRunnableOn_page = false;
				return true;

			case KeyEvent.KEYCODE_MEDIA_NEXT: //87
				if(viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))
					newPos = 0;
				else
					newPos = NoteUi.getFocus_notePos() + 1;

				NoteUi.setFocus_notePos(newPos);
				viewPager.setCurrentItem(newPos);

				BackgroundAudioService.mIsPrepared = false;
				BackgroundAudioService.mMediaPlayer = null;
				Audio_manager.isRunnableOn_page = false;
				return true;

			case KeyEvent.KEYCODE_MEDIA_PLAY: //126
			case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
				AudioUi_note.mPager_audio_play_button.performClick();
				return true;

			case KeyEvent.KEYCODE_BACK:
                onBackPressed();
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



	void setLayoutView()
	{
        System.out.println("Note / _setLayoutView");

		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

		UilCommon.init();

		// DB
		DB_folder dbFolder = new DB_folder(act,Pref.getPref_focusView_folder_tableId(act));
		mStyle = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		mDb_page = new DB_page(act, TabsHost.getCurrentPageTableId());

		// Instantiate a ViewPager and a PagerAdapter.
		viewPager = (ViewPager) findViewById(R.id.tabs_pager);
		mPagerAdapter = new Note_adapter(viewPager,this);
		viewPager.setAdapter(mPagerAdapter);
		viewPager.setCurrentItem(NoteUi.getFocus_notePos());

		if(mDb_page != null) {
			mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
		}

        if(UtilAudio.hasAudioExtension(mAudioUriInDB) ||
		   UtilAudio.hasAudioExtension(Util.getDisplayNameByUriString(mAudioUriInDB, act)[0])) {
            audioUi_note = new AudioUi_note(this, mAudioUriInDB);
            audioUi_note.init_audio_block();
        }


		// Note: if viewPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
		//       be called again after rotation
		viewPager.setOnPageChangeListener(onPageChangeListener);//todo deprecated

		// edit note button
		editButton = (Button) findViewById(R.id.view_edit);
		editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
		editButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(Note.this, Note_edit.class);
				intent.putExtra(DB_page.KEY_NOTE_ID, mNoteId);
				intent.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_AUDIO_URI , mDb_page.getNoteAudioUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(mNoteId));
				startActivityForResult(intent, EDIT_CURRENT_VIEW);
			}
		});

		// back button
		backButton = (Button) findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
		backButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view) {
				stopNoteAudio();
				finish();
			}
		});
	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{
			if(Audio_manager.getAudioPlayMode()  == Audio_manager.NOTE_PLAY_MODE)
                Audio_manager.stopAudioPlayer();

			NoteUi.setFocus_notePos(viewPager.getCurrentItem());
			System.out.println("Note / _onPageSelected");
//			System.out.println("    NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());
//			System.out.println("    nextPosition = " + nextPosition);

			// show audio name
			mNoteId = mDb_page.getNoteId(nextPosition,true);
			System.out.println("Note / _onPageSelected / mNoteId = " + mNoteId);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
			System.out.println("Note / _onPageSelected / mAudioUriInDB = " + mAudioUriInDB);

			if(UtilAudio.hasAudioExtension(mAudioUriInDB)) {
                audioUi_note = new AudioUi_note(Note.this, mAudioUriInDB);
                audioUi_note.init_audio_block();
                audioUi_note.showAudioBlock();
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

	    // check if there is one note at least in the pager
		if( viewPager.getAdapter().getCount() > 0 )
			setOutline(act);
		else
			finish();
	}

    /** Set outline for selected view mode
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public static void setOutline(AppCompatActivity act)
	{
        // Set full screen or not, and action bar
		Util.setFullScreen_noImmersive(act);
        if(act.getSupportActionBar() != null)
		    act.getSupportActionBar().show();

        // renew pager
        showSelectedView();

		LinearLayout buttonGroup = (LinearLayout) act.findViewById(R.id.view_button_group);
        // button group
		buttonGroup.setVisibility(View.VISIBLE);

		TextView audioTitle = (TextView) act.findViewById(R.id.pager_audio_title);
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

		// dismiss popup menu
		if(NoteUi.popup != null)
		{
			NoteUi.popup.dismiss();
			NoteUi.popup = null;
		}

        setLayoutView();

        Note.setViewAllMode();

        // Set outline of view mode
        setOutline(act);
	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note / _onStart");
	}
	
	@Override
	protected void onResume() {
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
			this.registerReceiver(mReceiver, filter);
		}
		else // Build.VERSION.SDK_INT >= 21
		{
			// Media session: to receive media button event of bluetooth device
			// new media browser instance and create BackgroundAudioService instance: support notification

//				if (MainAct.mMediaBrowserCompat == null) {
//					MainAct.mMediaBrowserCompat = new MediaBrowserCompat(act,
//							new ComponentName(act, BackgroundAudioService.class),
//							MainAct.mMediaBrowserCompatConnectionCallback,
//							act.getIntent().getExtras());
//				}
//
//				if (  (MainAct.mMediaBrowserCompat != null) &&
//						!MainAct.mMediaBrowserCompat.isConnected()) {
//					MainAct.mMediaBrowserCompat.connect();//???
//				}
//					MainAct.mCurrentState = MainAct.STATE_PAUSED;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note / _onPause");

		isPagerActive = false;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("Note / _onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note / _onDestroy");

		if(Audio_manager.isRunnableOn_note) {
			BackgroundAudioService.mIsPrepared = false;
			BackgroundAudioService.mMediaPlayer = null;
			Audio_manager.isRunnableOn_note = false;
		}

        // disconnect MediaBrowserCompat
//		if(Audio_manager.getAudioPlayMode() != Audio_manager.PAGE_PLAY_MODE) {
//			if (Build.VERSION.SDK_INT >= 21) {
//				if( (MainAct.mMediaBrowserCompat != null) &&
//					 MainAct.mMediaBrowserCompat.isConnected() )
//					MainAct.mMediaBrowserCompat.disconnect();
//			}
//		}
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		System.out.println("Note / _finish");

		ViewGroup view = (ViewGroup) getWindow().getDecorView();
	    view.setBackgroundColor(getResources().getColor(R.color.bar_color)); // avoid white flash
	    view.removeAllViews();

		super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note / _onSaveInstanceState");
	}

	Menu mMenu;
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);

		// inflate menu
		getMenuInflater().inflate(R.menu.pager_menu, menu);
		mMenu = menu;

		// menu item: checked status
		// get checked or not
		int isChecked = mDb_page.getNoteMarking(NoteUi.getFocus_notePos(),true);
		if( isChecked == 0)
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
		else
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
				stopNoteAudio();
                finish();
                return true;

			case R.id.VIEW_NOTE_CHECK:
				int markingNow = PageAdapter_recycler.toggleNoteMarking(this,NoteUi.getFocus_notePos());

				// update marking
				if(markingNow == 1)
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
				else
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);

				return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note / _onBackPressed");
		stopNoteAudio();
        finish();
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
    
	public static void stopNoteAudio()
	{
		if(Audio_manager.getAudioPlayMode() == Audio_manager.NOTE_PLAY_MODE)
            Audio_manager.stopAudioPlayer();
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
				Toast.makeText(getApplicationContext(), "ACTION_ACL_CONNECTED: device is " + device, Toast.LENGTH_LONG).show();
			}

			Intent intentReceive = intent;
			KeyEvent keyEvent = (KeyEvent) intentReceive.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if(keyEvent != null)
				onKeyDown( keyEvent.getKeyCode(),keyEvent);
		}
	};

}