/*
 * Copyright (C) 2021 CW Chiu
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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.audio.AudioUi_note;
import com.cw.audio7.audio.BackgroundAudioService;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.main.MenuId;
import com.cw.audio7.note_edit.Note_edit;
import com.cw.audio7.page.PageAdapter;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.image.ImageCache;
import com.cw.audio7.util.image.ImageFetcher;
import com.cw.audio7.util.preferences.Pref;
//import com.cw.audio7.util.uil.UilCommon;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static com.cw.audio7.audio.BackgroundAudioService.mAudio_manager;
import static com.cw.audio7.audio.BackgroundAudioService.mMediaPlayer;

public class NoteAct extends AppCompatActivity
{
	public static final int VIEW_CURRENT_NOTE = 6;
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
    static int mStyle;
    
    static SharedPreferences mPref_show_note_attribute;

	public static String mAudioUriInDB;

    public AppCompatActivity act;
    public AudioUi_note audioUi_note;
	public FragmentManager mFragmentManager;
	// for Image Cache
	private static final String IMAGE_CACHE_DIR = "images";
	private ImageFetcher mImageFetcher;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		System.out.println("NoteAct / _onCreate");
		super.onCreate(savedInstanceState);

		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

		Toolbar toolbar = findViewById(R.id.recorder_toolbar);
		toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
		setSupportActionBar(toolbar);

		Bundle arguments = getIntent().getExtras();
		mEntryPosition = arguments.getInt("POSITION");
		NoteUi.setFocus_notePos(mEntryPosition);

		DB_page db_page = new DB_page(TabsHost.getCurrentPageTableId());
		NoteUi.setNotesCnt(db_page.getNotesCount(true));

		act = this;

		// force stop audio whenever user touch new thumb nail at page mode
		if((mAudio_manager !=null) && (mAudio_manager.mAudioPos != mEntryPosition)) {
			mAudio_manager.stopAudioPlayer();
			mAudio_manager.audio7Player = null;
		}

		// add on back stack changed listener
		mFragmentManager = getSupportFragmentManager();

		// for Image Cache
		// Fetch screen height and width, to use as our max size when loading images as this
		// activity runs full screen
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		final int height = displayMetrics.heightPixels;
		final int width = displayMetrics.widthPixels;

		// For this sample we'll use half of the longest width to resize our images. As the
		// image scaling ensures the image is larger than this, we should be left with a
		// resolution that is appropriate for both portrait and landscape. For best image quality
		// we shouldn't divide by 2, but this will use more memory and require a larger memory
		// cache.
		final int longest = (Math.max(height, width)) / 2;

		ImageCache.ImageCacheParams cacheParams =
				new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

		// The ImageFetcher takes care of loading images into our ImageView children asynchronously
		mImageFetcher = new ImageFetcher(this, longest);
		mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
		mImageFetcher.setImageFadeIn(false);
	}

	// for Image Cache
	public ImageFetcher getImageFetcher() {
		return mImageFetcher;
	}

	@Override
	public void onBackPressed() {
		System.out.println("NoteAct / _onBackPressed" );
		// add for avoiding exception: The application's PagerAdapter changed the adapter's contents without calling PagerAdapter#notifyDataSetChanged!
		viewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

		finish();
	}

	Menu mMenu;
	@Override
	public boolean onCreateOptionsMenu( Menu menu) {
		System.out.println("NoteAct / _onCreateOptionsMenu");
		menu.clear();
		getMenuInflater().inflate(R.menu.pager_menu, menu);
		mMenu = menu;

		// menu item: checked status
		// get checked or not
		int isChecked = mDb_page.getNoteMarking(NoteUi.getFocus_notePos(),true);
		if( isChecked == 0)
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
		else
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);

		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mMenu.findItem(R.id.VIEW_NOTE_CHECK).setVisible(true);
		return super.onPrepareOptionsMenu(menu);
	}

	void setLayoutView()
	{
        System.out.println("NoteAct / _setLayoutView");

		mPref_show_note_attribute = act.getSharedPreferences("show_note_attribute", 0);

//		UilCommon.init();

		// DB
		DB_folder dbFolder = new DB_folder(Pref.getPref_focusView_folder_tableId(act));

		{
			mStyle = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);
			mDb_page = new DB_page(TabsHost.getCurrentPageTableId());

			if(mNoteId == null)
				mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
		}

		if (UtilAudio.hasAudioExtension(mAudioUriInDB) ||
				UtilAudio.hasAudioExtension(Util.getDisplayNameByUriString(mAudioUriInDB, act)[0])) {
			// create new instance after rotation
			audioUi_note = new AudioUi_note(act, null, mAudioUriInDB);
		}

		// Instantiate a ViewPager and a PagerAdapter.

		if(viewPager == null) {
			viewPager = (ViewPager) findViewById(R.id.tabs_pager);
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
			if(mAudio_manager.getAudioPlayMode()  == mAudio_manager.NOTE_PLAY_MODE) {
				System.out.println("NoteAct / onPageSelected / stop audio" );
				mAudio_manager.stopAudioPlayer();
			}

			NoteUi.setFocus_notePos(viewPager.getCurrentItem());
			System.out.println("NoteAct / _onPageSelected");
//			System.out.println("    NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());
//			System.out.println("    nextPosition = " + nextPosition);

			// show audio name
			mNoteId = mDb_page.getNoteId(nextPosition,true);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
			System.out.println("NoteAct / _onPageSelected /  mNoteId = " + mNoteId
					+ ", mAudioUriInDB = " + mAudioUriInDB);

			if(UtilAudio.hasAudioExtension(mAudioUriInDB)) {
	            audioUi_note = new AudioUi_note(act, null, mAudioUriInDB);
				/** Entry: Note play */
				audioUi_note.playAudioInNotePager(act,mAudioUriInDB);
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

    /** Set outline for selected view mode
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public void setOutline(AppCompatActivity act)
	{
		System.out.println("NoteAct / _setOutline");

        // Set full screen or not, and action bar
//		Util.setFullScreen_noImmersive(act);
//        if(act.getSupportActionBar() != null)
//		    act.getSupportActionBar().show();

        // renew pager
        showSelectedView();

		TextView audioTitle = (TextView) findViewById(R.id.audio_title);
        // audio title
        if(!Util.isEmptyString(audioTitle.getText().toString()) )
            audioTitle.setVisibility(View.VISIBLE);
        else
            audioTitle.setVisibility(View.GONE);

        // renew options menu
        act.invalidateOptionsMenu();

		// Set up activity to go full screen
		act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		ActionBar actionBar = act.getSupportActionBar();
		// immersive photo viewing experience
		// Hide title text and set home as up
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);//false
			actionBar.setDisplayHomeAsUpEnabled(true);

			// Hide and show the ActionBar as the visibility changes
			viewPager.setOnSystemUiVisibilityChangeListener(
					new View.OnSystemUiVisibilityChangeListener() {
						@Override
						public void onSystemUiVisibilityChange(int vis) {
							if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
								actionBar.hide();
							} else {
								actionBar.show();
							}
						}
					});

			// Start low profile mode and hide ActionBar
			viewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			actionBar.hide();
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("NoteAct / _onConfigurationChanged");
		setLayoutView();
	}

	@Override
	public void onResume() {
		super.onResume();
		System.out.println("NoteAct / _onResume");

		setLayoutView();

		isPagerActive = true;

		NoteAct.setViewAllMode();

		setOutline(act);

		// Register Bluetooth device receiver
		if(Build.VERSION.SDK_INT < 21)
		{
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
			act.registerReceiver(mReceiver, filter);
		}

		// for Image Cache
		mImageFetcher.setExitTasksEarly(false);
	}

	@Override
	public void onPause() {
		super.onPause();
		System.out.println("NoteAct / _onPause");

		isPagerActive = false;

		// disable full screen
		Util.setNormalScreen(act);

		// for Image Cache
		mImageFetcher.setExitTasksEarly(true);
		mImageFetcher.flushCache();
	}

	@Override
	public void onStop() {
		super.onStop();
		System.out.println("Note/ _onStop");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("NoteAct / _onDestroy");

		// for Image Cache
		mImageFetcher.closeCache();
	}

    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
				finish();
                return true;

	        case MenuId.VIEW_NOTE_CHECK:
		        int markingNow = PageAdapter.toggleNoteMarking(NoteUi.getFocus_notePos());

		        // update marking
		        if(markingNow == 1)
			        mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
		        else {
			        mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
			        stopNoteAudio();
			        mAudio_manager.audio7Player.setAudioPanel(audioUi_note.audioPanel);
			        mAudio_manager.audio7Player.initAudioBlock(mAudioUriInDB);
			        mAudio_manager.audio7Player.updateAudioPanel(act);
		        }
		        return true;

	        case MenuId.VIEW_NOTE_EDIT:
		        Intent intent = new Intent(act, Note_edit.class);
		        intent.putExtra(DB_page.KEY_NOTE_ID, mNoteId);
		        intent.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(mNoteId));
		        intent.putExtra(DB_page.KEY_NOTE_AUDIO_URI , mDb_page.getNoteAudioUri_byId(mNoteId));
		        intent.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(mNoteId));
		        startActivityForResult(intent, VIEW_CURRENT_NOTE);
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
		if(mAudio_manager.getAudioPlayMode() == mAudio_manager.NOTE_PLAY_MODE)
            mAudio_manager.stopAudioPlayer();
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
		System.out.println("NoteAct / _onKeyDown / keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
				if(viewPager.getCurrentItem() == 0)
					newPos = mPagerAdapter.getCount() - 1;//back to last one
				else
					newPos = NoteUi.getFocus_notePos()-1;

				NoteUi.setFocus_notePos(newPos);
				mAudio_manager.stopAudioPlayer();
				viewPager.setCurrentItem(newPos);

				BackgroundAudioService.mIsPrepared = false;
				mMediaPlayer = null;
				return true;

			case KeyEvent.KEYCODE_MEDIA_NEXT: //87
				if(viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))
					newPos = 0;
				else
					newPos = NoteUi.getFocus_notePos() + 1;

				NoteUi.setFocus_notePos(newPos);
				mAudio_manager.stopAudioPlayer();
				viewPager.setCurrentItem(newPos);

				BackgroundAudioService.mIsPrepared = false;
				mMediaPlayer = null;
				return true;

			case KeyEvent.KEYCODE_MEDIA_PLAY: //126
			case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
				audioUi_note.audio_play_btn.performClick();
				return true;

			case KeyEvent.KEYCODE_BACK:
                onBackPressed();
				return true;

			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
			case KeyEvent.KEYCODE_MEDIA_REWIND:
			case KeyEvent.KEYCODE_MEDIA_STOP:
				return true;
		}
		return false;
	}

}