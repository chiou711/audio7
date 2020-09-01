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

package com.cw.audio7.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.cw.audio7.R;
import com.cw.audio7.config.About;
import com.cw.audio7.config.Config;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.drawer.Drawer;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.note_add.Add_note_option;
import com.cw.audio7.note_add.add_audio.Add_audio_all;
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.operation.audio.AudioPlayer_page;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.operation.delete.DeleteFolders;
import com.cw.audio7.operation.delete.DeletePages;
import com.cw.audio7.page.Checked_notes_option;
import com.cw.audio7.page.PageUi;
import com.cw.audio7.page.Page_recycler;
import com.cw.audio7.tabs.AudioUi_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.operation.import_export.Export_toSDCardFragment;
import com.cw.audio7.operation.import_export.Import_filesList;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.util.Dialog_EULA;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.image.UtilImage;
import com.cw.audio7.define.Define;
import com.cw.audio7.util.OnBackPressedListener;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;
import com.mobeta.android.dslv.DragSortListView;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.StrictMode;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static android.os.Build.VERSION_CODES.M;

public class MainAct extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener
{
    public static CharSequence mFolderTitle;
    public static CharSequence mAppTitle;
    public Context mContext;
    public Config mConfigFragment;
    public About mAboutFragment;
    public static Menu mMenu;
    public static List<String> mFolderTitles;
    public static AppCompatActivity mAct;//TODO static issue
    public FragmentManager mFragmentManager;
    public static FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;
    public static int mLastOkTabId = 1;
    public static SharedPreferences mPref_show_note_attribute;
    OnBackPressedListener onBackPressedListener;
    public Drawer drawer;
    public static Folder mFolder;
    public static MainUi mMainUi;
    public static Toolbar mToolbar;

    public static MediaBrowserCompat mMediaBrowserCompat;
    public static MediaControllerCompat mMediaControllerCompat;
    public static int mCurrentState;
    public final static int STATE_PAUSED = 0;
    public final static int STATE_PLAYING = 1;
    public boolean bEULA_accepted;

	// Main Act onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Define.setAppBuildMode();

        // Release mode: no debug message
        if (Define.CODE_MODE == Define.RELEASE_MODE) {
            OutputStream nullDev = new OutputStream() {
                public void close() {}
                public void flush() {}
                public void write(byte[] b) {}
                public void write(byte[] b, int off, int len) {}
                public void write(int b) {}
            };
            System.setOut(new PrintStream(nullDev));
        }

        System.out.println("================start application ==================");
        System.out.println("MainAct / _onCreate");

        mAct = this;
        mAppTitle = getTitle();
        mMainUi = new MainUi();

        // File provider implementation is needed after Android version 24
        // if not, will encounter android.os.FileUriExposedException
        // cf. https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed

        // add the following to disable this requirement
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                // method 1
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);

                // method 2
//                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//                StrictMode.setVmPolicy(builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Show Api version
        if (Define.CODE_MODE == Define.DEBUG_MODE)
            Toast.makeText(this, mAppTitle + " " + "API_" + Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, mAppTitle, Toast.LENGTH_SHORT).show();

        //Log.d below can be disabled by applying proguard
        //1. enable proguard-android-optimize.txt in project.properties
        //2. be sure to use newest version to avoid build error
        //3. add the following in proguard-project.txt
        /*-assumenosideeffects class android.util.Log {
        public static boolean isLoggable(java.lang.String, int);
        public static int v(...);
        public static int i(...);
        public static int w(...);
        public static int d(...);
        public static int e(...);
        }
        */
        UtilImage.getDefaultScaleInPercent(MainAct.this);

        // EULA
        Dialog_EULA dialog_EULA = new Dialog_EULA(this);
        bEULA_accepted = dialog_EULA.isEulaAlreadyAccepted();

        // Show dialog of EULA
        if (!bEULA_accepted)
        {
            //deleteDatabase(Define.DB_FILE_NAME);

            // Ok button listener
            dialog_EULA.clickListener_Ok = (DialogInterface dialog, int i) -> {

                dialog_EULA.applyPreference();

                // dialog: with default content
                if((Define.DEFAULT_CONTENT == Define.BY_INITIAL_TABLES) && (Define.INITIAL_FOLDERS_COUNT > 0))
                {
                    if(Build.VERSION.SDK_INT >= 23)
                        checkPermission(savedInstanceState, Util.PERMISSIONS_REQUEST_STORAGE);
                    else
                    {
                        Pref.setPref_will_create_default_content(this, true);
                        recreate();
                    }
                    // Close dialog
                    dialog.dismiss();
                }
            };

            // Read agreement button listener
            dialog_EULA.clickListener_ReadAgreement = (DialogInterface dialog, int i) -> {
                dialog_EULA.show_read_agreement();
            };

            // No button listener
            dialog_EULA.clickListener_No = (DialogInterface dialog, int which) -> {
                    // Close the activity as they have declined
                    // the EULA
                    dialog.dismiss();
                    mAct.finish();
            };

            // back button listener
            dialog_EULA.clickListener_back = (DialogInterface dialog, int which) -> {
                // Close the activity as they have declined
                // the EULA
                dialog.dismiss();
                dialog_EULA.show();
            };

            dialog_EULA.show();
        }
        else
            doCreate();
    }

    // check permission dialog
    void checkPermission(Bundle savedInstanceState,int permissions_request)
    {
        // check permission first time, request all necessary permissions
        if(Build.VERSION.SDK_INT >= M)//API23
        {
            int permissionWriteExtStorage = ActivityCompat.checkSelfPermission(mAct, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(permissionWriteExtStorage != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(mAct,
                                                  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                               Manifest.permission.READ_EXTERNAL_STORAGE },
                                                  permissions_request);
            }
            else {
                Pref.setPref_will_create_default_content(this, false);
                recreate();
            }
        }
        else {
            Pref.setPref_will_create_default_content(this, false);
            recreate();
        }
    }

    // Do major create operation
    void doCreate()
    {
        System.out.println("MainAct / _doCreate");

//		Context context = getApplicationContext();

        // enable ActionBar app icon to behave as action to toggle nav drawer
//	        getActionBar().setDisplayHomeAsUpEnabled(true);
//	        getActionBar().setHomeButtonEnabled(true);
//			getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(mAct)));

        mContext = getBaseContext();

        // add on back stack changed listener
        mFragmentManager = getSupportFragmentManager();
        mOnBackStackChangedListener = this;
        mFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);

        // Register Bluetooth device receiver
        if (Build.VERSION.SDK_INT < 21) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            this.registerReceiver(bluetooth_device_receiver, filter);
        } else // if(Build.VERSION.SDK_INT >= 21)
        {
            // Media session: to receive media button event of bluetooth device
            // new media browser instance and create BackgroundAudioService instance: support notification
            if (mMediaBrowserCompat == null) {
                mMediaBrowserCompat = new MediaBrowserCompat(mAct,
                        new ComponentName(mAct, BackgroundAudioService.class),
                        mMediaBrowserCompatConnectionCallback,
                        mAct.getIntent().getExtras());

                if (!mMediaBrowserCompat.isConnected())
                    mMediaBrowserCompat.connect();//cf: https://stackoverflow.com/questions/43169875/mediabrowser-subscribe-doesnt-work-after-i-get-back-to-activity-1-from-activity

                mCurrentState = STATE_PAUSED;
            }
        }

        // init audio parameters
        MainAct.mPlaying_folderPos = -1;
        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
        TabsHost.audioPlayTabPos = -1;
    }


    /**
     * Create initial tables
     */
    void createDefaultContent_byInitialTables()
    {
        DB_drawer dB_drawer = new DB_drawer(this);

        for(int i = 1; i<= Define.INITIAL_FOLDERS_COUNT; i++)
        {
            // Create initial folder tables
            System.out.println("MainAct / _createInitialTables / folder id = " + i);
            String folderTitle = getResources().getString(R.string.default_folder_name).concat(String.valueOf(i));
            dB_drawer.insertFolder(i, folderTitle, true); // Note: must set false for DB creation stage
            dB_drawer.insertFolderTable( i, true);

            // Create initial page tables
            if(Define.INITIAL_PAGES_COUNT > 0)
            {
                // page tables
                for(int j = 1; j<= Define.INITIAL_PAGES_COUNT; j++)
                {
                    System.out.println("MainAct / _createInitialTables / page id = " + j);
                    DB_folder db_folder = new DB_folder(this,i);
                    db_folder.insertPageTable(db_folder, i, j, true);

                    String DB_FOLDER_TABLE_PREFIX = "Folder";
                    String folder_table = DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(i));
                    db_folder.open();
                    db_folder.insertPage(db_folder.mSqlDb ,
                                                        folder_table,
                                                        Define.getTabTitle(this,j),
                                                        1,
                                                        Define.STYLE_DEFAULT);
                    db_folder.close();
                    //db_folder.insertPage(sqlDb,folder_table,"N2",2,1);
                }
            }
        }

        Pref.setPref_will_create_default_content(this,false);
        recreate();
    }

    Intent intentReceive;
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private BroadcastReceiver bluetooth_device_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("MainAct / _BroadcastReceiver / onReceive");
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                Toast.makeText(getApplicationContext(), "ACTION_ACL_CONNECTED: device is " + device, Toast.LENGTH_LONG).show();
            }

            intentReceive = intent;
            KeyEvent keyEvent = (KeyEvent) intentReceive.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if(keyEvent != null)
                onKeyDown( keyEvent.getKeyCode(),keyEvent);
        }
    };


    // key event: 1 from bluetooth device 2 when notification bar dose not shown
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        System.out.println("MainAct / _onKeyDown / keyCode = " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_previous_btn.performClick();
                return true;

            case KeyEvent.KEYCODE_MEDIA_NEXT: //87
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_next_btn.performClick();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY: //126
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_play_button.performClick();
                else
                    playFirstAudio();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
                if(TabsHost.audioUi_page != null)
                    TabsHost.audioUi_page.audioPanel_play_button.performClick();
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

    private boolean isStorageRequestedImport = false;
    private boolean isStorageRequestedExport = false;

    // callback of granted permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        System.out.println("MainAct / _onRequestPermissionsResult / grantResults.length =" + grantResults.length);

        if ( (grantResults.length > 0) &&
             ( (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
               (grantResults[1] == PackageManager.PERMISSION_GRANTED)   ) )
        {
            switch (requestCode)
            {
                case Util.PERMISSIONS_REQUEST_STORAGE:
                    Pref.setPref_will_create_default_content(this, true);
                    recreate();
                    break;

                case Util.PERMISSIONS_REQUEST_STORAGE_ADD_NEW:
                    Add_note_option.createSelection(this,true);
                    break;

                case Util.PERMISSIONS_REQUEST_STORAGE_IMPORT:
                    isStorageRequestedImport = true;
                    break;

                case Util.PERMISSIONS_REQUEST_STORAGE_EXPORT:
                    isStorageRequestedExport = true;
                    break;
            }
        }
        else
        {
            switch (requestCode) {
                case Util.PERMISSIONS_REQUEST_STORAGE_ADD_NEW:
                    Add_note_option.createSelection(this, false);
                    break;

                case Util.PERMISSIONS_REQUEST_STORAGE:
                    Pref.setPref_will_create_default_content(this, true); //also create as granted
                    recreate();
                    break;
            }
        }

        //normally, will go to _resume
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
    }

    /**
     * initialize action bar
     */
//    void initActionBar(Menu mMenu,Drawer drawer)
//    {
//        mMenu.setGroupVisible(R.id.group_notes, true);
//        getActionBar().setDisplayShowHomeEnabled(true);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
//        drawer.drawerToggle.setDrawerIndicatorEnabled(true);
//    }

    void initActionBar()
    {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            mToolbar.setNavigationIcon(R.drawable.ic_drawer);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Drawer.drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    // set action bar for fragment
    void initActionBar_home()
    {
        drawer.drawerToggle.setDrawerIndicatorEnabled(false);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayShowHomeEnabled(false);//false: no launcher icon
        }

        mToolbar.setNavigationIcon(R.drawable.ic_menu_back);
        mToolbar.getChildAt(1).setContentDescription(getResources().getString(R.string.btn_back));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("MainAct / _initActionBar_home / click to popBackStack");

                // check if DB is empty
                DB_drawer db_drawer = new DB_drawer(mAct);
                int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(mAct);
                DB_folder db_folder = new DB_folder(mAct,focusFolder_tableId);
                if((db_drawer.getFoldersCount(true) == 0) ||
                   (db_folder.getPagesCount(true) == 0)      )
                {
                    finish();
                    Intent intent  = new Intent(mAct,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                    getSupportFragmentManager().popBackStack();
            }
        });

    }


    /*********************************************************************************
     *
     *                                      Life cycle
     *
     *********************************************************************************/

    @Override
    protected void onPause() {
        super.onPause();
//        bluetooth_device_receiver.abortBroadcast();//todo better place?
        System.out.println("MainAct / _onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
//    	System.out.println("MainAct / _onResume");
        mAct = this;

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(bEULA_accepted) {

            // Will create default contents: by assets or by initial tables
//        if(Pref.getPref_will_create_default_content(this))
//        {
//            if ((Define.DEFAULT_CONTENT == Define.BY_INITIAL_TABLES) && (Define.INITIAL_FOLDERS_COUNT > 0))
//                createDefaultContent_byInitialTables();
//        }

            mFolderTitles = new ArrayList<>();

            // check DB
            final boolean ENABLE_DB_CHECK = false;//true;//false
            if (ENABLE_DB_CHECK) {
                // list all folder tables
                FolderUi.listAllFolderTables(mAct);

                // recover focus
                DB_folder.setFocusFolder_tableId(Pref.getPref_focusView_folder_tableId(this));
                DB_page.setFocusPage_tableId(Pref.getPref_focusView_page_tableId(this));
            }//if(ENABLE_DB_CHECK)

            if(bEULA_accepted)
                configLayoutView(); //createAssetsFile inside

            if(drawer != null)
                drawer.drawerToggle.syncState();

            // get focus folder table Id, default folder table Id: 1
            DB_drawer dB_drawer = new DB_drawer(this);
            dB_drawer.open();
            for (int i = 0; i < dB_drawer.getFoldersCount(false); i++) {
                if (dB_drawer.getFolderTableId(i, false) == Pref.getPref_focusView_folder_tableId(this)) {
                    FolderUi.setFocus_folderPos(i);
                    System.out.println("MainAct / _mainAction / FolderUi.getFocus_folderPos() = " + FolderUi.getFocus_folderPos());
                }
            }
            dB_drawer.close();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // do Add all
        if(Pref.getPref_will_create_default_content(this)) {

            getSupportActionBar().hide();

            Add_audio_all add_audio_all = new Add_audio_all();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
            transaction.add(R.id.content_frame, add_audio_all, "add_audio").addToBackStack(null).commit();
        }

    }

    @Override
    protected void onResumeFragments() {
//        System.out.println("MainAct / _onResumeFragments ");
        super.onResumeFragments();

        if( isStorageRequestedImport ||
            isStorageRequestedExport   )
        {
            //hide the menu
            mMenu.setGroupVisible(R.id.group_notes, false);
            mMenu.setGroupVisible(R.id.group_pages_and_more, false);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if(isStorageRequestedImport) {
                // replace fragment
                Import_filesList importFragment = new Import_filesList();
                transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                transaction.replace(R.id.content_frame, importFragment, "import").addToBackStack(null).commit();

                isStorageRequestedImport = false;
            }

            if(isStorageRequestedExport)
            {
                DB_folder dB_folder = new DB_folder(this, Pref.getPref_focusView_folder_tableId(this));
                if (dB_folder.getPagesCount(true) > 0) {
                    Export_toSDCardFragment exportFragment = new Export_toSDCardFragment();
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                } else {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
                isStorageRequestedExport = false;
            }

            if (FolderUi.mHandler != null)
                FolderUi.mHandler.removeCallbacks(FolderUi.mTabsHostRun);
        }
        // fix: home button failed after power off/on in Config fragment
        else {
            if (bEULA_accepted) {
                if(mFragmentManager != null)
                    mFragmentManager.popBackStack();

                if (!mAct.isDestroyed()) {
//                    System.out.println("MainAct / _onResumeFragments / mAct is not Destroyed()");
                    openFolder();
                }
//                else
//                    System.out.println("MainAct / _onResumeFragments / mAct is Destroyed()");
            }
        }
    }

    // open folder
    public static void openFolder()
    {
//        System.out.println("MainAct / _openFolder");

        DB_drawer dB_drawer = new DB_drawer(mAct);
        int folders_count = dB_drawer.getFoldersCount(true);

        if (folders_count > 0) {
            int pref_focus_table_id = Pref.getPref_focusView_folder_tableId(MainAct.mAct);
            for(int folder_pos=0; folder_pos<folders_count; folder_pos++)
            {
                if(dB_drawer.getFolderTableId(folder_pos,true) == pref_focus_table_id) {
                    // select folder
                    FolderUi.selectFolder(mAct, folder_pos);

                    // set focus folder position
                    FolderUi.setFocus_folderPos(folder_pos);
                }
            }
            // set focus table Id
            DB_folder.setFocusFolder_tableId(pref_focus_table_id);

            if (mAct.getSupportActionBar() != null)
                mAct.getSupportActionBar().setTitle(mFolderTitle);
        }
    }


    @Override
    protected void onDestroy()
    {
        System.out.println("MainAct / onDestroy");

        if(bluetooth_device_receiver != null)
        {
            try
            {
                unregisterReceiver(bluetooth_device_receiver);
            }
            catch (Exception e)
            {
            }
            bluetooth_device_receiver = null;
        }

        // stop audio player
        if(BackgroundAudioService.mMediaPlayer != null)
            Audio_manager.stopAudioPlayer();

        // disconnect MediaBrowserCompat
        if( (mMediaBrowserCompat != null) && mMediaBrowserCompat.isConnected())
            mMediaBrowserCompat.disconnect();

        //hide notification
        NotificationManagerCompat.from(MainAct.mAct).cancel(BackgroundAudioService.id);

        mMediaBrowserCompat = null;

        super.onDestroy();
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("MainAct / _onConfigurationChanged");

        configLayoutView();

        // Pass any configuration change to the drawer toggles
        drawer.drawerToggle.onConfigurationChanged(newConfig);

		drawer.drawerToggle.syncState();

        FolderUi.startTabsHostRun();
    }


    /**
     *  on Back button pressed
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("MainAct / _onBackPressed");
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (f instanceof Add_audio_all)
            Toast.makeText(this, R.string.toast_not_working,Toast.LENGTH_SHORT).show();
        else
            doBackKeyEvent();
    }

    void doBackKeyEvent()
    {
        if (onBackPressedListener != null)
        {
            DB_drawer dbDrawer = new DB_drawer(this);
            int foldersCnt = dbDrawer.getFoldersCount(true);

            if(foldersCnt == 0)
            {
                finish();
                Intent intent  = new Intent(this,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else {
                onBackPressedListener.doBack();
            }
        }
        else
        {
            if((drawer != null) && drawer.isDrawerOpen())
                drawer.closeDrawer();
            else
                super.onBackPressed();
        }

    }


    @Override
    public void onBackStackChanged() {
        int backStackEntryCount = mFragmentManager.getBackStackEntryCount();
//        System.out.println("MainAct+ / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1) // fragment
        {
//            System.out.println("MainAct / _onBackStackChanged / fragment");
            initActionBar_home();
        }
        else if(backStackEntryCount == 0) // init
        {
//            System.out.println("MainAct / _onBackStackChanged / init");
            onBackPressedListener = null;

            if(mFolder.adapter!=null)
                mFolder.adapter.notifyDataSetChanged();

            configLayoutView();

            drawer.drawerToggle.syncState(); // make sure toggle icon state is correct
        }
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    /**
     * on Activity Result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        System.out.println("MainAct / _onActivityResult ");
    }

    /***********************************************************************************
     *
     *                                          Menu
     *
     ***********************************************************************************/

    /****************************************************
     *  On Prepare Option menu :
     *  Called whenever we call invalidateOptionsMenu()
     ****************************************************/
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        System.out.println("MainAct / _onPrepareOptionsMenu");

        if((drawer == null) || (drawer.drawerLayout == null) || (!bEULA_accepted))
            return false;

        DB_drawer db_drawer = new DB_drawer(this);
        int foldersCnt = db_drawer.getFoldersCount(true);

        /**
         * Folder group
         */
        // If the navigation drawer is open, hide action items related to the content view
        if(drawer.isDrawerOpen())
        {
            // for landscape: the layout file contains folder menu
            if(Util.isLandscapeOrientation(mAct)) {
                mMenu.setGroupVisible(R.id.group_folders, true);
                // set icon for folder draggable: landscape
                if(MainAct.mPref_show_note_attribute != null)
                {
                    if (MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                            .equalsIgnoreCase("yes"))
                        mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_on_holo_light);
                    else
                        mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_off_holo_light);
                }
            }

//            mMenu.findItem(R.id.DELETE_FOLDERS).setVisible(foldersCnt >0);
//            mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setVisible(foldersCnt >1);
            mMenu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
            mMenu.setGroupVisible(R.id.group_notes, false);
            mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
            mMenu.setGroupVisible(R.id.group_pages_and_more, false);
        }
        else if(!drawer.isDrawerOpen())
        {
            if(Util.isLandscapeOrientation(mAct))
                mMenu.setGroupVisible(R.id.group_folders, false);

            mMenu.findItem(R.id.ADD_NEW_NOTE).setVisible(true);
            mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(true);

            /**
             * Page group and more
             */
            mMenu.setGroupVisible(R.id.group_pages_and_more, foldersCnt >0);

            // group of notes
            mMenu.setGroupVisible(R.id.group_notes, true);

            if(foldersCnt>0)
            {
                getSupportActionBar().setTitle(mFolderTitle);

                // pages count
                int pgsCnt = FolderUi.getFolder_pagesCount(this,FolderUi.getFocus_folderPos());

                // notes count
                int notesCnt = 0;
                int pageTableId = Pref.getPref_focusView_page_tableId(this);

                if(pageTableId > 0) {
                    DB_page dB_page = new DB_page(this, pageTableId);
                    if (dB_page != null) {
                        try {
                            notesCnt = dB_page.getNotesCount(true);
                        } catch (Exception e) {
                            System.out.println("MainAct / _onPrepareOptionsMenu / dB_page.getNotesCount() error");
                            notesCnt = 0;
                        }
                    }
                }

                // change page color
                mMenu.findItem(R.id.CHANGE_PAGE_COLOR).setVisible(pgsCnt >0);

                // pages order
                mMenu.findItem(R.id.SHIFT_PAGE).setVisible(pgsCnt >1);

                // delete pages
                mMenu.findItem(R.id.DELETE_PAGES).setVisible(pgsCnt >0);

                // note operation
                mMenu.findItem(R.id.note_operation).setVisible( (pgsCnt >0) && (notesCnt>0) );

                // EXPORT TO SD CARD
                mMenu.findItem(R.id.EXPORT_TO_SD_CARD).setVisible(pgsCnt >0);

                /**
                 *  Note group
                 */

                // play
                mMenu.findItem(R.id.PLAY).setVisible( (pgsCnt >0) && (notesCnt>0) );

                // HANDLE CHECKED NOTES
	            if(Pref.getPref_card_view_enable_select(mAct))
                    mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( (pgsCnt >0) && (notesCnt>0) );
                else
                    mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( false );
            }
            else if(foldersCnt==0)
            {
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( false );
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /*************************
     * onCreate Options Menu
     *
     *************************/
    public static MenuItem mSubMenuItemAudio;
    MenuItem playOrStopMusicButton;
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
//		System.out.println("MainAct / _onCreateOptionsMenu");
        mMenu = menu;

        // inflate menu
        getMenuInflater().inflate(R.menu.main_menu, menu);

        playOrStopMusicButton = menu.findItem(R.id.PLAY_OR_STOP_MUSIC);
        mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

        // enable larger view
        if(Pref.getPref_card_view_enable_large_view(mAct))
            menu.findItem(R.id.ENABLE_NOTE_LARGE_VIEW)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.large_view);
        else
            menu.findItem(R.id.ENABLE_NOTE_LARGE_VIEW)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.large_view);

        // enable drag note
        if(Pref.getPref_card_view_enable_draggable(mAct))
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.drag_note) ;
        else
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.drag_note) ;

        // enable select note
	    if(Pref.getPref_card_view_enable_select(mAct))
            menu.findItem(R.id.ENABLE_NOTE_SELECT)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.select_note) ;
        else
            menu.findItem(R.id.ENABLE_NOTE_SELECT)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.select_note) ;

        return super.onCreateOptionsMenu(menu);
    }

    /******************************
     * on options item selected
     *
     ******************************/
    public static FragmentTransaction mFragmentTransaction;
    public static int mPlaying_pageTableId;
    public static int mPlaying_pagePos;
    public static int mPlaying_folderPos;
    public static int mPlaying_folderTableId;

    static int mMenuUiState;

    public static void setMenuUiState(int mMenuState) {
        mMenuUiState = mMenuState;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //System.out.println("MainAct / _onOptionsItemSelected");
        setMenuUiState(item.getItemId());
        DB_drawer dB_drawer = new DB_drawer(this);
        DB_folder dB_folder = new DB_folder(this, Pref.getPref_focusView_folder_tableId(this));
//        DB_page dB_page = new DB_page(this,Pref.getPref_focusView_page_tableId(this));

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Go back: check if Configure fragment now
        if( (item.getItemId() == android.R.id.home ))
        {

            System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / mFragmentManager.getBackStackEntryCount() =" +
            mFragmentManager.getBackStackEntryCount());

            if(mFragmentManager.getBackStackEntryCount() > 0 )
            {
                int foldersCnt = dB_drawer.getFoldersCount(true);
                System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / foldersCnt = " + foldersCnt);

                if(foldersCnt == 0)
                {
                    finish();
                    Intent intent  = new Intent(this,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                {
                    mFragmentManager.popBackStack();

                    initActionBar();

                    mFolderTitle = dB_drawer.getFolderTitle(FolderUi.getFocus_folderPos(),true);
                    setTitle(mFolderTitle);
                    drawer.closeDrawer();
                }
                return true;
            }
        }


        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawer.drawerToggle.onOptionsItemSelected(item))
        {
            System.out.println("MainAct / _onOptionsItemSelected / drawerToggle.onOptionsItemSelected(item) == true ");
            return true;
        }

        switch (item.getItemId())
        {
            case MenuId.ADD_NEW_FOLDER:
                FolderUi.renewFirstAndLast_folderId();
                FolderUi.addNewFolder(this, FolderUi.mLastExist_folderTableId +1, mFolder.getAdapter());
                return true;

            case MenuId.ENABLE_FOLDER_DRAG_AND_DROP:
                if(MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                        .equalsIgnoreCase("yes"))
                {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","no")
                            .apply();
                    DragSortListView listView = (DragSortListView) this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(false);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","yes")
                            .apply();
                    DragSortListView listView = (DragSortListView) this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(true);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                mFolder.getAdapter().notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                return true;

            case MenuId.DELETE_FOLDERS:
                mMenu.setGroupVisible(R.id.group_folders, false);

                if(dB_drawer.getFoldersCount(true)>0)
                {
                    drawer.closeDrawer();
                    mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                    DeleteFolders delFoldersFragment = new DeleteFolders();
                    mFragmentTransaction = mFragmentManager.beginTransaction();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delFoldersFragment).addToBackStack("delete_folders").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
                }
                return true;

            case MenuId.ADD_NEW_NOTE:
                if(Build.VERSION.SDK_INT >= M)//api23
                {
                    // check permission
                    int permissionWriteExtStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if(permissionWriteExtStorage != PackageManager.PERMISSION_GRANTED ) {
                        ActivityCompat.requestPermissions(mAct,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE },
                                   Util.PERMISSIONS_REQUEST_STORAGE_ADD_NEW);
                    }
                    else {
                            Add_note_option.createSelection(this,true);
                    }

                }
                else
                    Add_note_option.createSelection(this,true);
                return true;

            case MenuId.OPEN_PLAY_SUBMENU:
                // new play instance: stop button is off
                if( (BackgroundAudioService.mMediaPlayer != null) &&
                    (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP))
                {
                    // show Stop
                    playOrStopMusicButton.setTitle(R.string.menu_button_stop_audio);
                    playOrStopMusicButton.setIcon(R.drawable.ic_media_stop);
                }
                else
                {
                    // show Play
                    playOrStopMusicButton.setTitle(R.string.menu_button_play_audio);
                    playOrStopMusicButton.setIcon(R.drawable.ic_media_play);
                }
                return true;

            case MenuId.PLAY_OR_STOP_AUDIO:
                if( (BackgroundAudioService.mMediaPlayer != null) &&
                    (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP))
                {
                    Audio_manager.stopAudioPlayer();

                    // remove audio panel
                    TabsHost.audioPlayer_page.page_runnable.run();

                    // refresh
                    TabsHost.reloadCurrentPage();

                    return true; // just stop playing, wait for user action
                }
                else // play first audio
                {
                    playFirstAudio();
                }
                return true;

            case MenuId.CHECKED_OPERATION:
                Checked_notes_option op = new Checked_notes_option(this);
                op.open_option_grid(this);
                return true;

            case MenuId.ADD_NEW_PAGE:

                // get current Max page table Id
                int currentMaxPageTableId = 0;
                int pgCnt = FolderUi.getFolder_pagesCount(this,FolderUi.getFocus_folderPos());
                DB_folder db_folder = new DB_folder(this,DB_folder.getFocusFolder_tableId());

                for(int i=0;i< pgCnt;i++)
                {
                    int id = db_folder.getPageTableId(i,true);
                    if(id >currentMaxPageTableId)
                        currentMaxPageTableId = id;
                }

                PageUi.addNewPage(this, currentMaxPageTableId + 1);
                return true;

            case MenuId.CHANGE_PAGE_COLOR:
                PageUi.changePageColor(this);
                return true;

            case MenuId.SHIFT_PAGE:
                PageUi.shiftPage(this);
            return true;

            case MenuId.DELETE_PAGES:
                //hide the menu
                mMenu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                mMenu.setGroupVisible(R.id.group_notes, false);
                mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                if(dB_folder.getPagesCount(true)>0)
                {
                    DeletePages delPgsFragment = new DeletePages();
                    mFragmentTransaction = mFragmentManager.beginTransaction();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delPgsFragment).addToBackStack("delete_pages").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
            return true;

            case MenuId.ENABLE_NOTE_LARGE_VIEW:
                if(Pref.getPref_card_view_enable_large_view(mAct)) {
                    Pref.setPref_card_view_enable_large_view(mAct,false);
//                    Toast.makeText(this,getResources().getString(R.string.large_view)+
//                                    ": " +
//                                    getResources().getString(R.string.set_disable),
//                            Toast.LENGTH_SHORT).show();
                }
                else {
                    Pref.setPref_card_view_enable_large_view(mAct,true);
//                    Toast.makeText(this,getResources().getString(R.string.large_view) +
//                                    ": " +
//                                    getResources().getString(R.string.set_enable),
//                            Toast.LENGTH_SHORT).show();
                }
//                invalidateOptionsMenu();
//                TabsHost.reloadCurrentPage();//todo Need more to avoid system hang up
                recreate();
                return true;

            case MenuId.ENABLE_NOTE_DRAG_AND_DROP:
                  if(Pref.getPref_card_view_enable_draggable(mAct)) {
                    Pref.setPref_card_view_enable_draggable(mAct,false);
                    Toast.makeText(this,getResources().getString(R.string.drag_note)+
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                   Toast.LENGTH_SHORT).show();
                }
                else {
                    Pref.setPref_card_view_enable_draggable(mAct,true);
                    Toast.makeText(this,getResources().getString(R.string.drag_note) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                TabsHost.reloadCurrentPage();
                return true;

            case MenuId.ENABLE_NOTE_SELECT:
	            if(Pref.getPref_card_view_enable_select(mAct)) {
		            Pref.setPref_card_view_enable_select(mAct,false);
                    Toast.makeText(this,getResources().getString(R.string.select_note)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else {
	                Pref.setPref_card_view_enable_select(mAct,true);
                    Toast.makeText(this,getResources().getString(R.string.select_note) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                TabsHost.reloadCurrentPage();
                return true;

            // sub menu for backup
            case MenuId.IMPORT_FROM_SD_CARD:
                if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
                                != PackageManager.PERMISSION_GRANTED))
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            Util.PERMISSIONS_REQUEST_STORAGE_IMPORT);
                }
                else {
                    //hide the menu
                    mMenu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                    mMenu.setGroupVisible(R.id.group_notes, false);
                    mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                    mMenu.setGroupVisible(R.id.group_pages_and_more, false);
                    // replace fragment
                    Import_filesList importFragment = new Import_filesList();
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.content_frame, importFragment, "import").addToBackStack(null).commit();
                }
                return true;

            case MenuId.EXPORT_TO_SD_CARD:
                if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && //API23
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) // check permission
                                != PackageManager.PERMISSION_GRANTED))
                {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            Util.PERMISSIONS_REQUEST_STORAGE_EXPORT);
                }
                else {
                    //hide the menu
                    mMenu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                    mMenu.setGroupVisible(R.id.group_notes, false);
                    mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                    mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                    if (dB_folder.getPagesCount(true) > 0) {
                        Export_toSDCardFragment exportFragment = new Export_toSDCardFragment();
                        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                        transaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                    } else {
                        Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;

            case MenuId.CONFIG:
                mMenu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                setTitle(R.string.settings);

                mConfigFragment = new Config();
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;

            case MenuId.ABOUT:
                mMenu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                mMenu.setGroupVisible(R.id.group_pages_and_more, false);

                setTitle(R.string.about_title);

                mAboutFragment = new About();
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mAboutFragment).addToBackStack("about").commit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void playFirstAudio()
    {
        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);

        Audio_manager.setAudioPlayMode(Audio_manager.PAGE_PLAY_MODE);
        Audio_manager.mAudioPos = 0;

        // cancel playing
        if(BackgroundAudioService.mMediaPlayer != null)
        {
            if(BackgroundAudioService.mMediaPlayer.isPlaying())
                BackgroundAudioService.mMediaPlayer.pause();

            if((AudioPlayer_page.mAudioHandler != null) &&
                    (TabsHost.audioPlayer_page != null)        ){
                AudioPlayer_page.mAudioHandler.removeCallbacks(TabsHost.audioPlayer_page.page_runnable);
            }
            BackgroundAudioService.mMediaPlayer.release();
            BackgroundAudioService.mMediaPlayer = null;
        }

        // initial
        BackgroundAudioService.mMediaPlayer = null;//for first

        TabsHost.audioUi_page = new AudioUi_page(this);
        TabsHost.audioUi_page.initAudioBlock(this);

        TabsHost.audioPlayer_page = new AudioPlayer_page(this,TabsHost.audioUi_page);
        TabsHost.audioPlayer_page.prepareAudioInfo();
        TabsHost.audioPlayer_page.runAudioState();

        // update audio play position
        TabsHost.audioPlayTabPos = TabsHost.getFocus_tabPos();
        TabsHost.mTabsPagerAdapter.notifyDataSetChanged();

        UtilAudio.updateAudioPanel(TabsHost.audioUi_page.audioPanel_play_button,
                TabsHost.audioUi_page.audio_panel_title_textView);

        // update playing page position
        mPlaying_pagePos = TabsHost.getFocus_tabPos();

        // update playing page table Id
        mPlaying_pageTableId = TabsHost.getCurrentPageTableId();

        // update playing folder position
        mPlaying_folderPos = FolderUi.getFocus_folderPos();

        DB_drawer dB_drawer = new DB_drawer(this);
        MainAct.mPlaying_folderTableId = dB_drawer.getFolderTableId(MainAct.mPlaying_folderPos,true);
    }

    // configure layout view
    void configLayoutView()
    {
//        System.out.println("MainAct / _configLayoutView");

        setContentView(R.layout.drawer);

        initActionBar();

        // new drawer
        drawer = new Drawer(this);
        drawer.initDrawer();

        // new folder
        mFolder = new Folder(this);

        openFolder();
    }


    // callback: media browser connection
    public static MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();

            System.out.println("MainAct / MediaBrowserCompat.Callback / _onConnected");
            try {
                if(mMediaBrowserCompat != null) {
                    mMediaControllerCompat = new MediaControllerCompat(mAct, mMediaBrowserCompat.getSessionToken());
                    mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                    MediaControllerCompat.setMediaController(mAct, mMediaControllerCompat);
                }
            } catch( RemoteException e ) {
                System.out.println("MainAct / MediaBrowserCompat.Callback / RemoteException");
            }
        }
    };

    // callback: media controller
    public static MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
//            System.out.println("MainAct / _MediaControllerCompat.Callback / _onPlaybackStateChanged / state = " + state);
            if( state == null ) {
                return;
            }

            switch( state.getState() ) {
                case STATE_PLAYING: {
                    mCurrentState = STATE_PLAYING;
                    break;
                }
                case STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    break;
                }
            }
        }
    };

}