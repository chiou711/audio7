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

package com.cw.audio7.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

import com.cw.audio7.R;
import com.cw.audio7.audio.Audio7Player;
import com.cw.audio7.audio.AudioUi_page;
import com.cw.audio7.config.About;
import com.cw.audio7.config.Config;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.db.DatabaseHelper;
import com.cw.audio7.drawer.Drawer;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.note.NoteAct;
import com.cw.audio7.note_add.Add_note_option;
import com.cw.audio7.note_add.add_audio.Add_audio_all;
import com.cw.audio7.audio.BackgroundAudioService;
import com.cw.audio7.operation.delete.DeleteFolders;
import com.cw.audio7.operation.delete.DeletePages;
import com.cw.audio7.page.Checked_notes_option;
import com.cw.audio7.operation.import_export.Export_toSDCardFragment;
import com.cw.audio7.operation.import_export.Import_filesList;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Dialog_EULA;
import com.cw.audio7.util.image.UtilImage;
import com.cw.audio7.define.Define;
import com.cw.audio7.util.OnBackPressedListener;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;
import com.mobeta.android.dslv.DragSortListView;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StrictMode;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static android.os.Build.VERSION_CODES.M;
import static com.cw.audio7.audio.BackgroundAudioService.mAudio_manager;
import static com.cw.audio7.audio.BackgroundAudioService.mMediaBrowserCompat;
import static com.cw.audio7.audio.BackgroundAudioService.mMediaControllerCompat;
import static com.cw.audio7.audio.BackgroundAudioService.mMediaPlayer;
import static com.cw.audio7.define.Define.ENABLE_MEDIA_CONTROLLER;

public class MainAct extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener
{
    public Toolbar toolbar;
    public CharSequence appTitle;
    public Config configFragment;
    public About aboutFragment;
    public Menu menu;
    public FragmentManager fragmentManager;
    public FragmentManager.OnBackStackChangedListener onBackStackChangedListener;
    OnBackPressedListener onBackPressedListener;
    public boolean bEULA_accepted;

    public Drawer drawer;
    public Folder folder;
    public static final int STORAGE_MANAGER_PERMISSION = 99;

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

        appTitle = getTitle();

        // todo application-specific directories
        // /storage/emulated/0/Android/data/com.cw.audio7/files/Pictures
        //File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //System.out.println("-- path = " + path);

        // File provider implementation is needed after Android version 24
        // if not, will encounter android.os.FileUriExposedException
        // cf. https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed

        // add the following to disable this requirement
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                // method 1
//                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
//                m.invoke(null);

                // method 2
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Show Api version
        if (Define.CODE_MODE == Define.DEBUG_MODE)
            Toast.makeText(this, appTitle + " " + "API_" + Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, appTitle, Toast.LENGTH_SHORT).show();

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

        if(Define.ENABLE_EULA) {
            // EULA
            Dialog_EULA dialog_EULA = new Dialog_EULA(this);
            bEULA_accepted = dialog_EULA.isEulaAlreadyAccepted();

            // Show dialog of EULA
            if (!bEULA_accepted) {
                //deleteDatabase(Define.DB_FILE_NAME);

                // Ok button listener
                dialog_EULA.clickListener_Ok = (DialogInterface dialog, int i) -> {

                    dialog_EULA.applyPreference(); // set true

                    bEULA_accepted = true;

                    // dialog: with default content
                    if ((Define.DEFAULT_CONTENT == Define.BY_INITIAL_TABLES) && (Define.INITIAL_FOLDERS_COUNT > 0)) {
                        if (Build.VERSION.SDK_INT >= 30)
                            checkStorageManagerPermission();
                        else if (Build.VERSION.SDK_INT >= 23)
                            checkPermission();
                        else {
                            Pref.setPref_will_create_default_content(this, true);
                            recreate();
                        }
                        // Close dialog
                        dialog.dismiss();
                    }
                };

                // Read agreement button listener
                dialog_EULA.clickListener_ReadAgreement = (DialogInterface dialog, int i) ->
                        dialog_EULA.show_read_agreement();

                // No button listener
                dialog_EULA.clickListener_No = (DialogInterface dialog, int which) -> {
                    // Close the activity as they have declined
                    // the EULA
                    dialog.dismiss();
                    finish();
                };

                // back button listener
                dialog_EULA.clickListener_back = (DialogInterface dialog, int which) -> {
                    // Close the activity as they have declined
                    // the EULA
                    dialog.dismiss();
                    dialog_EULA.show();
                };

                dialog_EULA.show();
            } else
                doCreate();
        }
        else // Disable EULA
        {
            bEULA_accepted = true;

            if ((Define.DEFAULT_CONTENT == Define.BY_INITIAL_TABLES) && (Define.INITIAL_FOLDERS_COUNT > 0)) {
                if (Build.VERSION.SDK_INT >= 30)
                    checkStorageManagerPermission();
                else if (Build.VERSION.SDK_INT >= 23)
                    checkPermission();
                else {
                    Pref.setPref_will_create_default_content(this, true);
                    recreate();
                }
            }
        }
    }

    // check permission dialog
    void checkPermission()
    {
        // check permission first time, request all necessary permissions
        if( !Util.willRequest_permission_WRITE_EXTERNAL_STORAGE(this,
                    Util.PERMISSIONS_REQUEST_STORAGE)) {
            Pref.setPref_will_create_default_content(this, false);
            recreate();
        } else
            doCreate();
    }


    public void checkStorageManagerPermission() {
        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //set this will go to _onActivityResult soon
            startActivityForResult(intent,STORAGE_MANAGER_PERMISSION);

            // flow of this query:
            // MainAct / _onPause / _onStop
            // this query UI
            // onActivityResult
            // MainAct / _onStart / _onResume
        } else
            doCreate();
    }

    // Do major create operation
    void doCreate()
    {
        System.out.println("MainAct / _doCreate");

//		mContext = getApplicationContext();
//      mContext = getBaseContext();

        //todo if (ENABLE_MEDIA_CONTROLLER)
        // Register Bluetooth device receiver
        if (ENABLE_MEDIA_CONTROLLER && Build.VERSION.SDK_INT >= 21) {
            // Media session: to receive media button event of bluetooth device
            // new media browser instance and create BackgroundAudioService instance: support notification
            if (mMediaBrowserCompat == null) {
                mMediaBrowserCompat = new MediaBrowserCompat(this,
                        new ComponentName(this, BackgroundAudioService.class),
                        mMediaBrowserCompatConnectionCallback,
                        getIntent().getExtras());

                if (!mMediaBrowserCompat.isConnected())
                    mMediaBrowserCompat.connect();//cf: https://stackoverflow.com/questions/43169875/mediabrowser-subscribe-doesnt-work-after-i-get-back-to-activity-1-from-activity
            }
        } else {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            this.registerReceiver(bluetooth_device_receiver, filter);
        }

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
            KeyEvent keyEvent = intentReceive.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
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
                if(folder.tabsHost.audioUi_page != null)
                    folder.tabsHost.audioUi_page.audio_previous_btn.performClick();
                return true;

            case KeyEvent.KEYCODE_MEDIA_NEXT: //87
                if(folder.tabsHost.audioUi_page != null)
                    folder.tabsHost.audioUi_page.audio_next_btn.performClick();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY: //126
                if(folder.tabsHost.audioUi_page != null)
                    folder.tabsHost.audioUi_page.audio_play_btn.performClick();
                else
                    playFirstAudio();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
                if(folder.tabsHost.audioUi_page != null)
                    folder.tabsHost.audioUi_page.audio_play_btn.performClick();
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

    private boolean isStorageRequestedImport = false;
    private boolean isStorageRequestedExport = false;

    // callback of granted permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults)
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
                    addNewNote();
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
                    if((grantResults[0] == PackageManager.PERMISSION_DENIED) ||
                       (grantResults[1] == PackageManager.PERMISSION_DENIED)    ) {
                        Toast.makeText(this,R.string.toast_no_SD_permission,Toast.LENGTH_LONG).show();
                    }
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
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_drawer);
            toolbar.setNavigationOnClickListener(v -> drawer.drawerLayout.openDrawer(GravityCompat.START));
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

        toolbar.setNavigationIcon(R.drawable.ic_menu_back);
        toolbar.getChildAt(1).setContentDescription(getResources().getString(R.string.btn_back));
        toolbar.setNavigationOnClickListener(v -> {
            System.out.println("MainAct / _initActionBar_home / click to popBackStack");

            // check if DB is empty
            DB_drawer db_drawer = new DB_drawer(this);
            int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(this);
            DB_folder db_folder = new DB_folder(focusFolder_tableId);
            if((db_drawer.getFoldersCount(true) == 0) ||
               (db_folder.getPagesCount(true) == 0)      )
            {
                finish();
                Intent intent  = new Intent(this,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else
                getSupportFragmentManager().popBackStack();
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
        System.out.println("MainAct / _onPause");

        // background play
        if(!Pref.getPref_background_play_enable(this)) {
            // stop audio when screen off
            mAudio_manager.stopAudioPlayer();
        }
//        else {
            // continue playing, do nothing
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("MainAct / _onResume");

        // add on back stack changed listener
        fragmentManager = getSupportFragmentManager();
        onBackStackChangedListener = this;
        fragmentManager.addOnBackStackChangedListener(onBackStackChangedListener);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(bEULA_accepted) {

            // Will create default contents: by assets or by initial tables
//        if(Pref.getPref_will_create_default_content(this))
//        {
//            if ((Define.DEFAULT_CONTENT == Define.BY_INITIAL_TABLES) && (Define.INITIAL_FOLDERS_COUNT > 0))
//                createDefaultContent_byInitialTables();
//        }

            // check DB
//            final boolean ENABLE_DB_CHECK = false;//true;//false
//            if (ENABLE_DB_CHECK) {
//                // list all folder tables
//                FolderUi.listAllFolderTables(mAct);
//
//                // recover focus
//                DB_folder.setFocusFolder_tableId(Pref.getPref_focusView_folder_tableId(this));
//                DB_page.setFocusPage_tableId(Pref.getPref_focusView_page_tableId(this));
//            }//if(ENABLE_DB_CHECK)

            BackgroundAudioService.dbHelper = new DatabaseHelper(this);

            if(bEULA_accepted)
                configLayoutView(); //createAssetsFile inside

            if(drawer != null)
                drawer.drawerToggle.syncState();

        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        System.out.println("MainAct / _onAttachedToWindow");

        // do Add all
        if(Pref.getPref_will_create_default_content(this)) {

            Objects.requireNonNull(getSupportActionBar()).hide();

            Add_audio_all add_audio_all = new Add_audio_all(drawer); // todo Check more
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
            menu.setGroupVisible(R.id.group_notes, false);
            menu.setGroupVisible(R.id.group_pages_and_more, false);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if(isStorageRequestedImport) {
                // replace fragment
                Import_filesList importFragment = new Import_filesList(this);
                transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                transaction.replace(R.id.content_frame, importFragment, "import").addToBackStack(null).commit();

                isStorageRequestedImport = false;
            }

            if(isStorageRequestedExport)
            {
                DB_folder dB_folder = new DB_folder(Pref.getPref_focusView_folder_tableId(this));
                if (dB_folder.getPagesCount(true) > 0) {
                    Export_toSDCardFragment exportFragment = new Export_toSDCardFragment(this,folder);
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                } else {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
                isStorageRequestedExport = false;
            }

            if (folder.mHandler != null)
                folder.mHandler.removeCallbacks(folder.mTabsHostRun);
        }
        // fix: home button failed after power off/on in Config fragment
        else {
            if (bEULA_accepted) {
                if(fragmentManager != null)
                    fragmentManager.popBackStack();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("MainAct / _onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("MainAct / _onStop");
    }

    @Override
    protected void onDestroy()
    {
        if (ENABLE_MEDIA_CONTROLLER) {
            //hide notification //todo ??? User stops all tasks could fail to hide
            NotificationManagerCompat.from(BackgroundAudioService.mAudioBgService).cancel(BackgroundAudioService.mNotification_id);

//            BackgroundAudioService.mMediaSessionCompat.release();
            // disconnect MediaBrowserCompat
            if ((mMediaBrowserCompat != null) && mMediaBrowserCompat.isConnected())
                mMediaBrowserCompat.disconnect();
            mMediaBrowserCompat = null;
        }
        System.out.println("MainAct / _onDestroy");

        if(bluetooth_device_receiver != null)
        {
            try {
                unregisterReceiver(bluetooth_device_receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            bluetooth_device_receiver = null;
        }

        // stop audio player
        if(mMediaPlayer != null)
            mAudio_manager.stopAudioPlayer();

        super.onDestroy();
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("MainAct / _onConfigurationChanged");

        // keep fragment when Rotation
        if(fragmentManager.getBackStackEntryCount() != 0)
            return;

        configLayoutView();

        // Pass any configuration change to the drawer toggles
        drawer.drawerToggle.onConfigurationChanged(newConfig);

		drawer.drawerToggle.syncState();
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
            if((drawer != null) && drawer.isDrawerOpen()) {
                drawer.closeDrawer();
            }
            else {
                super.onBackPressed();
                finish();
            }
        }

    }


    @Override
    public void onBackStackChanged() {
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();
        System.out.println("MainAct / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1) // fragment
        {
            System.out.println("MainAct / _onBackStackChanged / fragment");
            initActionBar_home();
        }
        else if(backStackEntryCount == 0) // init
        {
            System.out.println("MainAct / _onBackStackChanged / init");
            onBackPressedListener = null;


            doCreate();//add for making sure audio playing after Do add all
            configLayoutView(); //todo Add this will make playing highlight gone after Add folder?

            // new drawer
            drawer = new Drawer(this, toolbar);
            drawer.initDrawer();

            if(folder.adapter!=null)
                folder.adapter.notifyDataSetChanged();

            drawer.drawerToggle.syncState(); // make sure toggle icon state is correct
        }
    }

//    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
//        this.onBackPressedListener = onBackPressedListener;
//    }

    /**
     * on Activity Result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        System.out.println("MainAct / _onActivityResult ");

        if(requestCode == NoteAct.VIEW_CURRENT_NOTE)
            System.out.println("MainAct / _onActivityResult / NoteAct.VIEW_CURRENT_NOTE");

        if(requestCode == STORAGE_MANAGER_PERMISSION){
            if(Environment.isExternalStorageManager()){
                Pref.setPref_will_create_default_content(this, true);
//                recreate();

                // make sure Do add all
                finish();
                Intent intent  = new Intent(this,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    }

    /*=======================================================

                                               Menu

     ========================================================*/

    /*==================================
     *  On Prepare Option menu :
     *  Called whenever we call invalidateOptionsMenu()
     ===================================*/
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        System.out.println("MainAct / _onPrepareOptionsMenu");

        if( (drawer == null) ||
            (drawer.drawerLayout == null) ||
            (!bEULA_accepted) ||
            (fragmentManager.getBackStackEntryCount() != 0) ) {
            return false;
        }

        DB_drawer db_drawer = new DB_drawer(this);
        int foldersCnt = db_drawer.getFoldersCount(true);

        /*
         * Folder group
         */
        // If the navigation drawer is open, hide action items related to the content view
        if(drawer.isDrawerOpen())
        {
            // for landscape: the layout file contains folder menu
            if(Util.isLandscapeOrientation(this)) {
                this.menu.setGroupVisible(R.id.group_folders, true);
                // set icon for folder draggable: landscape
                if(drawer.mPref_show_note_attribute != null)
                {
                    if (Objects.requireNonNull(drawer.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no"))
                            .equalsIgnoreCase("yes")) {
                        this.menu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_on_holo_light);
                    } else
                        this.menu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_off_holo_light);
                }
            }

//            mMenu.findItem(R.id.DELETE_FOLDERS).setVisible(foldersCnt >0);
//            mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setVisible(foldersCnt >1);
            this.menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
            this.menu.setGroupVisible(R.id.group_notes, false);
            this.menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
            this.menu.setGroupVisible(R.id.group_pages_and_more, false);
        }
        else if(!drawer.isDrawerOpen())
        {
            if(Util.isLandscapeOrientation(this))
                this.menu.setGroupVisible(R.id.group_folders, false);

            this.menu.findItem(R.id.ADD_NEW_NOTE).setVisible(true);
            this.menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(true);

            /*
             * Page group and more
             */
            this.menu.setGroupVisible(R.id.group_pages_and_more, foldersCnt >0);

            // group of notes
            this.menu.setGroupVisible(R.id.group_notes, true);

            if(foldersCnt>0)
            {
                Objects.requireNonNull(getSupportActionBar()).setTitle(folder.mFolderTitle);

                // pages count
                int pgsCnt = folder.getFolder_pagesCount(this, Folder.getFocus_folderPos());

                // notes count
                int notesCnt = 0;
                int pageTableId = Pref.getPref_focusView_page_tableId(this);

                if(pageTableId > 0) {
                    DB_page dB_page = new DB_page(pageTableId);
                    try {
                        notesCnt = dB_page.getNotesCount(true);
                    } catch (Exception e) {
                        System.out.println("MainAct / _onPrepareOptionsMenu / dB_page.getNotesCount() error");
                        notesCnt = 0;
                    }
                }

                // change page color
                this.menu.findItem(R.id.CHANGE_PAGE_COLOR).setVisible(pgsCnt >0);

                // pages order
                this.menu.findItem(R.id.SHIFT_PAGE).setVisible(pgsCnt >1);

                // delete pages
                this.menu.findItem(R.id.DELETE_PAGES).setVisible(pgsCnt >0);

                // note operation
                this.menu.findItem(R.id.note_operation).setVisible( (pgsCnt >0) && (notesCnt>0) );

                // EXPORT TO SD CARD
                this.menu.findItem(R.id.EXPORT_TO_SD_CARD).setVisible(pgsCnt >0);

                /*
                 *  Note group
                 */

                // play icon
                boolean playIconIsVisible= false;
                if( ((pgsCnt >0) && (notesCnt>0)) ||
                    (mAudio_manager.getPlayerState() != mAudio_manager.PLAYER_AT_STOP) ) {
                    playIconIsVisible = true;
                }
                this.menu.findItem(R.id.PLAY).setVisible( playIconIsVisible );
                this.menu.findItem(R.id.PLAY_CYCLIC).setVisible( playIconIsVisible );

                // HANDLE CHECKED NOTES
	            if(Pref.getPref_card_view_enable_select(this))
                    this.menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( (pgsCnt >0) && (notesCnt>0) );
                else
                    this.menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( false );
            }
            else if(foldersCnt==0)
            {
                this.menu.setGroupVisible(R.id.group_notes, false);
                this.menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( false );
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /*************************
     * onCreate Options Menu
     *
     *************************/
    MenuItem playOrStopMusicButton;
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
//		System.out.println("MainAct / _onCreateOptionsMenu");
        this.menu = menu;

        // inflate menu
        getMenuInflater().inflate(R.menu.main_menu, menu);

        playOrStopMusicButton = menu.findItem(R.id.PLAY_OR_STOP_MUSIC);
        drawer.mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

        // show cyclic play setting
        if(Pref.getPref_cyclic_play_enable(this))
            menu.findItem(R.id.PLAY_CYCLIC)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.menu_button_cyclic_play);
        else
            menu.findItem(R.id.PLAY_CYCLIC)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.menu_button_cyclic_play);

        // show background play setting
        if(Pref.getPref_background_play_enable(this))
            menu.findItem(R.id.PLAY_BACKGROUND)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.menu_button_background_play);
        else
            menu.findItem(R.id.PLAY_BACKGROUND)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.menu_button_background_play);

        // enable larger view
        if(Pref.getPref_card_view_enable_large_view(this))
            menu.findItem(R.id.ENABLE_NOTE_LARGE_VIEW)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.large_view);
        else
            menu.findItem(R.id.ENABLE_NOTE_LARGE_VIEW)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.large_view);

        // enable drag note
        if(Pref.getPref_card_view_enable_draggable(this))
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_on_holo_light)
                    .setTitle(R.string.drag_note) ;
        else
            menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
                    .setIcon(R.drawable.btn_check_off_holo_light)
                    .setTitle(R.string.drag_note) ;

        // enable select note
	    if(Pref.getPref_card_view_enable_select(this))
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
    public FragmentTransaction mFragmentTransaction;
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
        DB_folder dB_folder = new DB_folder(Pref.getPref_focusView_folder_tableId(this));
//        DB_page dB_page = new DB_page(this,Pref.getPref_focusView_page_tableId(this));

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Go back: check if Configure fragment now
        if( (item.getItemId() == android.R.id.home ))
        {

            System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / mFragmentManager.getBackStackEntryCount() =" +
            fragmentManager.getBackStackEntryCount());

            if(fragmentManager.getBackStackEntryCount() > 0 )
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
                    fragmentManager.popBackStack();

                    initActionBar();

                    folder.mFolderTitle = dB_drawer.getFolderTitle(Folder.getFocus_folderPos(),true);
                    setTitle(folder.mFolderTitle);
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
                folder.renewFirstAndLast_folderId();
                folder.addNewFolder(this, folder.mLastExist_folderTableId +1, folder.getAdapter());
                return true;

            case MenuId.ENABLE_FOLDER_DRAG_AND_DROP:
                if(Objects.requireNonNull(drawer.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no"))
                        .equalsIgnoreCase("yes"))
                {
                    drawer.mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","no")
                            .apply();
                    DragSortListView listView = this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(false);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else
                {
                    drawer.mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","yes")
                            .apply();
                    DragSortListView listView = this.findViewById(R.id.drawer_listview);
                    listView.setDragEnabled(true);
                    Toast.makeText(this,getResources().getString(R.string.drag_folder) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                folder.getAdapter().notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                return true;

            case MenuId.DELETE_FOLDERS:
                menu.setGroupVisible(R.id.group_folders, false);

                if(dB_drawer.getFoldersCount(true)>0)
                {
                    drawer.closeDrawer();
                    menu.setGroupVisible(R.id.group_notes, false); //hide the menu
                    DeleteFolders delFoldersFragment = new DeleteFolders();
                    mFragmentTransaction = fragmentManager.beginTransaction();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delFoldersFragment).addToBackStack("delete_folders").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
                }
                return true;

            case MenuId.ADD_NEW_NOTE:
                if(Build.VERSION.SDK_INT >= 30) {
                    checkStorageManagerPermission();
                    if(Environment.isExternalStorageManager())
                        addNewNote();
                }
                else if( (Build.VERSION.SDK_INT < 23) ||
                         !Util.willRequest_permission_WRITE_EXTERNAL_STORAGE(this,
                              Util.PERMISSIONS_REQUEST_STORAGE_ADD_NEW) ) {
                        addNewNote();
                }
                return true;

            case MenuId.OPEN_PLAY_SUBMENU:
                // new play instance: stop button is off
                if( (mMediaPlayer != null) &&
                    (mAudio_manager.getPlayerState() != mAudio_manager.PLAYER_AT_STOP))
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
                if( (mMediaPlayer != null) &&
                    (mAudio_manager.getPlayerState() != mAudio_manager.PLAYER_AT_STOP))
                {
                    mAudio_manager.stopAudioPlayer();

                    if(mAudio_manager.audio7Player != null)
                        mAudio_manager.audio7Player.showAudioPanel(false);

                    // refresh
                    folder.tabsHost.reloadCurrentPage();
                    return true; // just stop playing, wait for user action
                }
                else // play first audio
                {
                    playFirstAudio();
                }
                return true;

            case MenuId.PLAY_CYCLIC:
                if(Pref.getPref_cyclic_play_enable(this)) {
                    Pref.setPref_cyclic_play_enable(this,false);
                    Toast.makeText(this,getResources().getString(R.string.menu_button_cyclic_play)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    Pref.setPref_cyclic_play_enable(this,true);
                    Toast.makeText(this,getResources().getString(R.string.menu_button_cyclic_play) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                return true;

            case MenuId.PLAY_BACKGROUND:
                if(Pref.getPref_background_play_enable(this)) {
                    Pref.setPref_background_play_enable(this,false);
                    Toast.makeText(this,getResources().getString(R.string.menu_button_background_play)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    Pref.setPref_background_play_enable(this,true);
                    Toast.makeText(this,getResources().getString(R.string.menu_button_background_play) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                return true;

            case MenuId.CHECKED_OPERATION:
                Checked_notes_option op = new Checked_notes_option(this,drawer,folder);
                op.open_option_grid(this);
                return true;

            case MenuId.ADD_NEW_PAGE:

                // get current Max page table Id
                int currentMaxPageTableId = 0;
                int pgCnt = folder.getFolder_pagesCount(this, Folder.getFocus_folderPos());
                DB_folder db_folder = new DB_folder(DB_folder.getFocusFolder_tableId());

                for(int i=0;i< pgCnt;i++)
                {
                    int id = db_folder.getPageTableId(i,true);
                    if(id >currentMaxPageTableId)
                        currentMaxPageTableId = id;
                }

                folder.tabsHost.mPageUi.addNewPage(this, currentMaxPageTableId + 1);
                return true;

            case MenuId.CHANGE_PAGE_COLOR:
                folder.tabsHost.mPageUi.changePageColor(this);
                return true;

            case MenuId.SHIFT_PAGE:
                folder.tabsHost.mPageUi.shiftPage(this);
            return true;

            case MenuId.DELETE_PAGES:
                //hide the menu
                menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                menu.setGroupVisible(R.id.group_notes, false);
                menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                menu.setGroupVisible(R.id.group_pages_and_more, false);

                if(dB_folder.getPagesCount(true)>0)
                {
                    DeletePages delPgsFragment = new DeletePages(this,folder);
                    mFragmentTransaction = fragmentManager.beginTransaction();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delPgsFragment).addToBackStack("delete_pages").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                }
            return true;

            case MenuId.ENABLE_NOTE_LARGE_VIEW:
                if(Pref.getPref_card_view_enable_large_view(this)) {
                    Pref.setPref_card_view_enable_large_view(this,false);
                    Toast.makeText(this,getResources().getString(R.string.large_view)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    Pref.setPref_card_view_enable_large_view(this,true);
                    Toast.makeText(this,getResources().getString(R.string.large_view) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                folder.tabsHost.reloadCurrentPage();//todo Need more to avoid system hang up?
//                recreate();
                return true;

            case MenuId.ENABLE_NOTE_DRAG_AND_DROP:
                  if(Pref.getPref_card_view_enable_draggable(this)) {
                    Pref.setPref_card_view_enable_draggable(this,false);
                    Toast.makeText(this,getResources().getString(R.string.drag_note)+
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                   Toast.LENGTH_SHORT).show();
                }
                else {
                    Pref.setPref_card_view_enable_draggable(this,true);
                    Toast.makeText(this,getResources().getString(R.string.drag_note) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                folder.tabsHost.reloadCurrentPage();
                return true;

            case MenuId.ENABLE_NOTE_SELECT:
	            if(Pref.getPref_card_view_enable_select(this)) {
		            Pref.setPref_card_view_enable_select(this,false);
                    Toast.makeText(this,getResources().getString(R.string.select_note)+
                                    ": " +
                                    getResources().getString(R.string.set_disable),
                            Toast.LENGTH_SHORT).show();
                }
                else {
	                Pref.setPref_card_view_enable_select(this,true);
                    Toast.makeText(this,getResources().getString(R.string.select_note) +
                                    ": " +
                                    getResources().getString(R.string.set_enable),
                            Toast.LENGTH_SHORT).show();
                }
                invalidateOptionsMenu();
                folder.tabsHost.reloadCurrentPage();
                return true;

            // sub menu for backup
            case MenuId.IMPORT_FROM_SD_CARD:
                if( ( Build.VERSION.SDK_INT < M /*API23*/ ) ||
                    !Util.willRequest_permission_WRITE_EXTERNAL_STORAGE(this,
                                Util.PERMISSIONS_REQUEST_STORAGE_IMPORT)             ) {
                    //hide the menu
                    menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                    menu.setGroupVisible(R.id.group_notes, false);
                    menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                    menu.setGroupVisible(R.id.group_pages_and_more, false);
                    // replace fragment
                    Import_filesList importFragment = new Import_filesList(this);
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.content_frame, importFragment, "import").addToBackStack(null).commit();
                }
                return true;

            case MenuId.EXPORT_TO_SD_CARD:
                if( ( Build.VERSION.SDK_INT <= 23) ||
                    !Util.willRequest_permission_WRITE_EXTERNAL_STORAGE(this,
                                Util.PERMISSIONS_REQUEST_STORAGE_EXPORT)           ) {
                    //hide the menu
                    menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                    menu.setGroupVisible(R.id.group_notes, false);
                    menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                    menu.setGroupVisible(R.id.group_pages_and_more, false);

                    if (dB_folder.getPagesCount(true) > 0) {
                        Export_toSDCardFragment exportFragment = new Export_toSDCardFragment(this,folder);
                        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                        transaction.replace(R.id.content_frame, exportFragment, "export").addToBackStack(null).commit();
                    } else {
                        Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;

            case MenuId.CONFIG:
                menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                menu.setGroupVisible(R.id.group_notes, false); //hide the menu
                menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                menu.setGroupVisible(R.id.group_pages_and_more, false);

                setTitle(R.string.settings);

                configFragment = new Config(this, folder);
                mFragmentTransaction = fragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, configFragment).addToBackStack("config").commit();
                return true;

            case MenuId.ABOUT:
                menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                menu.setGroupVisible(R.id.group_notes, false); //hide the menu
                menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                menu.setGroupVisible(R.id.group_pages_and_more, false);

                setTitle(R.string.about_title);

                aboutFragment = new About();
                mFragmentTransaction = fragmentManager.beginTransaction();
                mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, aboutFragment).addToBackStack("about").commit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // addNewNote
    void addNewNote(){
        System.out.println("MainAct / _addNewNote ");
        Add_note_option add_note_option = new Add_note_option(this, menu, drawer);
        add_note_option.createSelection(this);
    }

    /** Entry: Page play */
    void playFirstAudio()
    {
        mAudio_manager.setPlayerState(mAudio_manager.PLAYER_AT_PLAY);
        mAudio_manager.mAudioPos = 0;

        DB_page db_page = new DB_page(TabsHost.getCurrentPageTableId());

        mAudio_manager.stopAudioPlayer();
        mAudio_manager.setupAudioList();

        String audioUriStr = db_page.getNoteAudioUri(0,true);
        mAudio_manager.mAudioUri = audioUriStr;

        View panelView = folder.tabsHost.audio_panel;
        if(mAudio_manager.audio7Player == null)
            mAudio_manager.audio7Player = new Audio7Player(this, folder.tabsHost,panelView,audioUriStr);
        else {
            mAudio_manager.audio7Player.setAudioPanel(panelView);
            mAudio_manager.audio7Player.initAudioBlock(audioUriStr);
        }

        folder.tabsHost.audioUi_page = new AudioUi_page(this, folder.tabsHost, mAudio_manager.audio7Player,panelView,audioUriStr);

        mAudio_manager.audio7Player.runAudioState();

        folder.tabsHost.showPlayingTab();

        // update audio play position
        MainAct.mPlaying_pagePos = TabsHost.getFocus_tabPos();
        folder.tabsHost.mTabsPagerAdapter.notifyDataSetChanged();

        // update playing page position
        mPlaying_pagePos = TabsHost.getFocus_tabPos();

        // update playing page table Id
        mPlaying_pageTableId = TabsHost.getCurrentPageTableId();

        // update playing folder position
        mPlaying_folderPos = Folder.getFocus_folderPos();

        DB_drawer dB_drawer = new DB_drawer(this);
        MainAct.mPlaying_folderTableId = dB_drawer.getFolderTableId(MainAct.mPlaying_folderPos,true);
    }

    // configure layout view
    void configLayoutView()
    {
        System.out.println("MainAct / _configLayoutView");

        setContentView(R.layout.drawer);

        initActionBar();

        // new drawer
        drawer = new Drawer(this, toolbar);
        drawer.initDrawer();

        // new folder
        folder = drawer.folder;
        folder.openFolder(drawer);
    }

    // if (ENABLE_MEDIA_CONTROLLER)
    // callback: media browser connection
    public MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();

            System.out.println("MainAct / MediaBrowserCompat.Callback / _onConnected");
            try {
                if(mMediaBrowserCompat != null) {
                    mMediaControllerCompat = new MediaControllerCompat(MainAct.this, mMediaBrowserCompat.getSessionToken());
                    mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                    MediaControllerCompat.setMediaController(MainAct.this, mMediaControllerCompat);
                }
            } catch( RemoteException e ) {
                System.out.println("MainAct / MediaBrowserCompat.Callback / RemoteException");
            }
        }
    };

    //if (ENABLE_MEDIA_CONTROLLER)
    // callback: media controller
    public final MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
//            System.out.println("MainAct / _MediaControllerCompat.Callback / _onPlaybackStateChanged / state = " + state);
        }
    };

}