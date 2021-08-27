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

package com.cw.audio7.folder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.define.Define;
import com.cw.audio7.drawer.Drawer;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.TouchableEditText;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;
import com.cw.audio7.util.system.SystemState;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import static com.cw.audio7.audio.BackgroundAudioService.mAudio_manager;
import static com.cw.audio7.audio.BackgroundAudioService.mMediaPlayer;

/**
 * Created by CW on 2016/8/23.
 */
public class Folder
{
    public DragSortListView listView;
    public SimpleDragSortCursorAdapter adapter;
    DragSortController controller;
    AppCompatActivity act;
    DB_drawer dB_drawer;
    SimpleDragSortCursorAdapter folderAdapter;
    public List<String> mFolderTitles;
    Drawer drawer;

    Folder(){};

    public Folder(AppCompatActivity _act,Drawer _drawer){
        act = _act;
        mFolderTitles = new ArrayList<>();
        FolderSetup(act);
        drawer = _drawer;
    };

    public void FolderSetup(AppCompatActivity act)
    {
        this.act = act;
        listView = (DragSortListView) act.findViewById(R.id.drawer_listview);
        dB_drawer = new DB_drawer(act);
        folderAdapter = initFolder();
    }

    // initialize folder list view
    SimpleDragSortCursorAdapter initFolder()
    {
        // set Folder title
        if(dB_drawer.getFoldersCount(true) == 0)
        {
            // default: add 2 new folders
//            for(int i = 0; i< Define.INITIAL_FOLDERS_COUNT; i++)
//            {
//                // insert folder
//                System.out.println("Folder/ _initFolder / insert folder "+ i) ;
//                String folderTitle = Define.getFolderTitle(act,i);
//                MainAct.mFolderTitles.add(folderTitle);
//                dB_drawer.insertFolder(i+1, folderTitle );
//            }
        }
        else
        {
            for(int i = 0; i< dB_drawer.getFoldersCount(true); i++)
            {
                mFolderTitles.add(""); // init only
                mFolderTitles.set(i, dB_drawer.getFolderTitle(i,true));
            }
        }

        // check DB
//        DB_drawer.listFolders();

        // set adapter
        dB_drawer.open();
        Cursor cursor = dB_drawer.cursor_folder;

        String[] from = new String[] { DB_drawer.KEY_FOLDER_TITLE};
        int[] to = new int[] { R.id.folderText};

        adapter = new Folder_adapter(
                act,
                R.layout.folder_row,
                cursor,
                from,
                to,
                0
        );

        dB_drawer.close();

        listView.setAdapter(adapter);

        // set up click listener
        listView.setOnItemClickListener(new Folder.FolderListener_click(act));

        // set up long click listener
        listView.setOnItemLongClickListener(new Folder.FolderListener_longClick(act,adapter));

        controller = buildController(listView);
        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);

        // init folder dragger
        SharedPreferences pref = act.getSharedPreferences("show_note_attribute", 0);
        if(pref.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                .equalsIgnoreCase("yes"))
            listView.setDragEnabled(true);
        else
            listView.setDragEnabled(false);

        listView.setDragListener(onDrag);
        listView.setDropListener(onDrop);

        return adapter;
    }

    public SimpleDragSortCursorAdapter getAdapter()
    {
        return adapter;
    }

    // list view listener: on drag
    DragSortListView.DragListener onDrag = new DragSortListView.DragListener()
    {
        @Override
        public void drag(int startPosition, int endPosition) {
        }
    };

    // list view listener: on drop
    DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
    {
        @Override
        public void drop(int startPosition, int endPosition) {
            //reorder data base storage
            int loop = Math.abs(startPosition-endPosition);
            for(int i=0;i< loop;i++)
            {
                swapFolderRows(startPosition,endPosition);
                if((startPosition-endPosition) >0)
                    endPosition++;
                else
                    endPosition--;
            }

            DB_drawer db_drawer = new DB_drawer(act);
            // update audio playing drawer index
            int drawerCount = db_drawer.getFoldersCount(true);
            for(int i=0;i<drawerCount;i++)
            {
                if(db_drawer.getFolderTableId(i,true) == MainAct.mPlaying_folderTableId)
                    MainAct.mPlaying_folderPos = i;
            }
            adapter.notifyDataSetChanged();
            updateFocus_folderPosition();
        }
    };



    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    private static DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DOWN); // click
        controller.setDragHandleId(R.id.folder_drag);// handler
        controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging

        return controller;
    }

    /**
     * Listeners for folder ListView
     *
     */
    // click
    public class FolderListener_click implements AdapterView.OnItemClickListener
    {
        AppCompatActivity act;
        FolderListener_click(AppCompatActivity act_){act = act_;}

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            System.out.println("Folder / _onItemClickListener / position = " + position);
            setFocus_folderPos(position);

            DB_drawer db_drawer = new DB_drawer(act);
            Pref.setPref_focusView_folder_tableId(act,db_drawer.getFolderTableId(position,true) );

            openFolder(drawer);
        }
    }

    // long click
    public class FolderListener_longClick implements DragSortListView.OnItemLongClickListener
    {

        AppCompatActivity act;
        SimpleDragSortCursorAdapter adapter;
        FolderListener_longClick(AppCompatActivity _act,SimpleDragSortCursorAdapter _adapter)
        {
            act = _act;
            adapter = _adapter;
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
        {
            editFolder(act,position, adapter);
            return true;
        }
    }

    // swap rows
    private static Long mFolderId1 = (long) 1;
    private static Long mFolderId2 = (long) 1;
    private int mFolderTableId1;
    private int mFolderTableId2;
    private String mFolderTitle1;
    private String mFolderTitle2;
    void swapFolderRows(int startPosition, int endPosition)
    {
        DB_drawer db_drawer = new DB_drawer(act);

        db_drawer.open();
        mFolderId1 = db_drawer.getFolderId(startPosition,false);
        mFolderTableId1 = db_drawer.getFolderTableId(startPosition,false);
        mFolderTitle1 = db_drawer.getFolderTitle(startPosition,false);

        mFolderId2 = db_drawer.getFolderId(endPosition,false);
        mFolderTableId2 = db_drawer.getFolderTableId(endPosition,false);
        mFolderTitle2 = db_drawer.getFolderTitle(endPosition,false);

        db_drawer.updateFolder(mFolderId1,
                mFolderTableId2,
                mFolderTitle2
                ,false);

        db_drawer.updateFolder(mFolderId2,
                mFolderTableId1,
                mFolderTitle1,false);
        db_drawer.close();
    }

    void editFolder(final AppCompatActivity act, final int position, final SimpleDragSortCursorAdapter folderAdapter)
    {
        DB_drawer db = new DB_drawer(act);

        // insert when table is empty, activated only for the first time
        final String folderTitle = db.getFolderTitle(position,true);

        final EditText editText = new EditText(act);
        editText.setText(folderTitle);
        editText.setSelection(folderTitle.length()); // set edit text start position

        //update tab info
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(R.string.edit_folder_title)
                .setMessage(R.string.edit_folder_message)
                .setView(editText)
                .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
                {   @Override
                public void onClick(DialogInterface dialog, int which)
                {/*cancel*/}
                })
                .setNeutralButton(R.string.edit_page_button_delete, new DialogInterface.OnClickListener()
                {   @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    // delete
                    Util util = new Util(act);
                    util.vibrate();

                    AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
                    builder1.setTitle(R.string.confirm_dialog_title)
                            .setMessage(R.string.confirm_dialog_message_folder)
                            .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog1, int which1){
                                    /*nothing to do*/}})
                            .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog1, int which1){
                                    deleteFolder(act, position,folderAdapter);
                                }})
                            .show();
                }
                })
                .setPositiveButton(R.string.edit_page_button_update, new DialogInterface.OnClickListener()
                {   @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    DB_drawer db_drawer = new DB_drawer(act);
                    // save
                    int drawerId =  (int) db_drawer.getFolderId(position,true);
                    int drawerTabInfoTableId =  db_drawer.getFolderTableId(position,true);
                    db_drawer.updateFolder(drawerId,
                            drawerTabInfoTableId,
                            editText.getText().toString()
                            ,true);
                    // update
                    folderAdapter.notifyDataSetChanged();
                    act.getSupportActionBar().setTitle(editText.getText().toString());

                }
                })
                .setIcon(android.R.drawable.ic_menu_edit);

        AlertDialog d1 = builder.create();
        d1.show();
        // android.R.id.button1 for positive: save
        ((Button)d1.findViewById(android.R.id.button1))
                .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);

        // android.R.id.button2 for negative: cancel
        ((Button)d1.findViewById(android.R.id.button2))
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        // android.R.id.button3 for neutral: delete
        ((Button)d1.findViewById(android.R.id.button3))
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
    }

    // Update focus position
    void updateFocus_folderPosition()
    {
        DB_drawer db_drawer = new DB_drawer(act);

        //update focus position
        int iLastView_folderTableId = Pref.getPref_focusView_folder_tableId(act);
        int count = db_drawer.getFoldersCount(true);
        for(int i=0;i<count;i++)
        {
            if(	db_drawer.getFolderTableId(i,true)== iLastView_folderTableId)
            {
                setFocus_folderPos(i);
                DragSortListView listView = (DragSortListView) act.findViewById(R.id.drawer_listview);

                listView.setItemChecked(getFocus_folderPos(), true);
            }
        }

    }

    public static int mFocus_folderTableId;

    // setter and getter of focus folder position
    public static int mFocus_folderPos;
    public static void setFocus_folderPos(int pos)
    {
        mFocus_folderPos = pos;
    }
    public static int getFocus_folderPos()
    {
        return mFocus_folderPos;
    }


    public void openFolder(Drawer drawer)
    {
        System.out.println("Folder / _openFolder");

        DB_drawer dB_drawer = new DB_drawer(act);
        int folders_count = dB_drawer.getFoldersCount(true);

        if (folders_count > 0) {
            int pref_focus_table_id = Pref.getPref_focusView_folder_tableId(act);
            for(int folder_pos=0; folder_pos<folders_count; folder_pos++)
            {
                if(dB_drawer.getFolderTableId(folder_pos,true) == pref_focus_table_id) {
                    // select folder
                    selectFolder(act, folder_pos);

                    // set focus folder position
                    setFocus_folderPos(folder_pos);

                    setFocus_folderTableId(pref_focus_table_id);
                }
            }
            // set focus table Id
            DB_folder.setFocusFolder_tableId(pref_focus_table_id);

            if (act.getSupportActionBar() != null)
                act.getSupportActionBar().setTitle(mFolderTitle);
        }

        if(drawer != null)
            drawer.closeDrawer();
    }

    public String mFolderTitle;
    // select folder
    public void selectFolder(AppCompatActivity act,final int position)
    {
    	System.out.println("Folder / _selectFolder / position = " + position);
        DB_drawer dB_drawer = new DB_drawer(act);
        mFolderTitle = dB_drawer.getFolderTitle(position,true);

        // will call Drawer / _onDrawerClosed
        DrawerLayout drawerLayout = (DrawerLayout) act.findViewById(R.id.drawer_layout);

        if(drawerLayout != null) {
            // update selected item and title, then close the drawer
            DragSortListView listView = (DragSortListView) act.findViewById(R.id.drawer_listview);
            listView.setItemChecked(position, true); // selected item is colored by different color
//	        drawerLayout.closeDrawer(drawer.mNavigationView); //todo Check more
            act.invalidateOptionsMenu();

            // use Runnable to make sure only one folder background is seen
            if(SystemState.isScreenOn(act))
                startTabsHostRun();
        }

    }


    public static int getFocus_folderTableId() {
        return mFocus_folderTableId;
    }

    public void setFocus_folderTableId(int mFocus_folderTableId) {
        this.mFocus_folderTableId = mFocus_folderTableId;
    }


    /**
     * delete selected folder
     *
     */
    private int mFirstExist_folderId = 0;
    public int mLastExist_folderTableId;
    private void deleteFolder( final AppCompatActivity act, int position,SimpleDragSortCursorAdapter folderAdapter) {

        System.out.println("Folder / _deleteFolder");
        // Before delete: renew first FolderId and last FolderId
        renewFirstAndLast_folderId();

        // keep one folder at least
        DB_drawer db_drawer = new DB_drawer(act);
//		int foldersCount = db_drawer.getFoldersCount();
//		if(foldersCount == 1)
//		{
//			 // show toast for only one folder
//             Toast.makeText(act, R.string.toast_keep_one_drawer , Toast.LENGTH_SHORT).show();
//             return;
//		}

        // get folder table Id
        int folderTableId = db_drawer.getFolderTableId(position,true);

        // remove focus view Key for page table Id
        Pref.removePref_focusView_page_tableId_key(act, folderTableId);

        // 1) delete related page table
        DB_folder dbFolder = new DB_folder(folderTableId);
        int pgsCnt = dbFolder.getPagesCount(true);
        for (int i = 0; i < pgsCnt; i++) {
            int pageTableId = dbFolder.getPageTableId(i, true);
            dbFolder.dropPageTable(folderTableId, pageTableId);
        }

        // get folder Id
        int folderId = (int) db_drawer.getFolderId(position,true);

        // 2) delete folder table
        db_drawer.dropFolderTable(folderTableId,true);

        // 3) delete folder Id in drawer table
        db_drawer.deleteFolderId(folderId,true);

        renewFirstAndLast_folderId();

        // After Delete
        // - update mFocus_folderPos
        // - select first existing drawer item
        int foldersCount = db_drawer.getFoldersCount(true);

        // get new focus position
        // if focus item is deleted, set focus to new first existing folder
        if (getFocus_folderPos() == position)
        {
            for (int i = 0; i < foldersCount; i++)
            {
                if (db_drawer.getFolderId(i,true) == mFirstExist_folderId)
                    setFocus_folderPos(i);
            }
        } else if (position < getFocus_folderPos())
            setFocus_folderPos(getFocus_folderPos()-1);

        // set new focus position
        DragSortListView listView = (DragSortListView) act.findViewById(R.id.drawer_listview);
        listView.setItemChecked(getFocus_folderPos(), true);

        if (foldersCount > 0) {
            int focusFolderTableId = db_drawer.getFolderTableId(getFocus_folderPos(),true);
            // update folder table Id of focus view
            Pref.setPref_focusView_folder_tableId(act, focusFolderTableId);
            // update folder table Id of new focus (error will cause first folder been deleted)
            DB_folder.setFocusFolder_tableId(focusFolderTableId);
        }

        // update audio playing highlight if needed
        if(mMediaPlayer != null)
        {
            if (MainAct.mPlaying_folderPos > position)
                MainAct.mPlaying_folderPos--;
            else if (MainAct.mPlaying_folderPos == position)
            {
                // stop audio since the folder is deleted
                if(mMediaPlayer != null)
                    mAudio_manager.stopAudioPlayer();

                // update
                if (foldersCount > 0)
                    openFolder(drawer);

            }
        }

        // refresh drawer list view
        folderAdapter.notifyDataSetChanged();

        act.invalidateOptionsMenu();
    }



    // Renew first and last folder Id
    private Cursor mFolderCursor;
    public void renewFirstAndLast_folderId()
    {
        DB_drawer db_drawer = new DB_drawer(act);
        int i = 0;
        int foldersCount = db_drawer.getFoldersCount(true);
        mLastExist_folderTableId = 0;
        while(i < foldersCount)
        {
            boolean isFirst;
            db_drawer.open();
            mFolderCursor = db_drawer.cursor_folder;
            mFolderCursor.moveToPosition(i);
            isFirst = mFolderCursor.isFirst();
            db_drawer.close();

            if(isFirst)
                mFirstExist_folderId = (int) db_drawer.getFolderId(i,true) ;

            if(db_drawer.getFolderTableId(i,true) >= mLastExist_folderTableId)
                mLastExist_folderTableId = db_drawer.getFolderTableId(i,true);

            i++;
        }
    }


    // start tabs host runnable
    public void startTabsHostRun()
    {
        System.out.println("Folder / _startTabsHostRun");

        DB_drawer dB_drawer = new DB_drawer(act);
        dB_drawer.open();
        for (int i = 0; i < dB_drawer.getFoldersCount(false); i++) {
            if (dB_drawer.getFolderTableId(i, false) == Pref.getPref_focusView_folder_tableId(act)) {
                setFocus_folderPos(i);
                System.out.println("TabsHost / _onCreate / mFolder.getFocus_folderPos() = " + getFocus_folderPos());
            }
        }
        dB_drawer.close();

        mHandler = new Handler();
        mHandler.post(mTabsHostRun);
    }

    public Handler mHandler;
    public TabsHost tabsHost;
    // runnable to launch folder host, (press alt+enter to get lambda)
    public Runnable mTabsHostRun = () -> {
//	    System.out.println("Folder / mTabsHostRun");

        tabsHost = new TabsHost(act,this);

        FragmentTransaction fragmentTransaction = act.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, tabsHost).commitAllowingStateLoss();
        act.getSupportFragmentManager().executePendingTransactions();
    };

    public int getFolder_pagesCount(AppCompatActivity act,int folderPos)
    {
        DB_drawer dB_drawer = new DB_drawer(act);
//        System.out.println("Folder / _getFolder_pagesCount / folderPos = " + folderPos);
        int pagesCount;
        try {
            int focusFolder_tableId = dB_drawer.getFolderTableId(folderPos, true);
            DB_folder db_folder = new DB_folder(focusFolder_tableId);
            db_folder.open();
            pagesCount = db_folder.getPagesCount(false);
//            System.out.println("Folder / _getFolder_pagesCount / pagesCount = " + pagesCount);
            db_folder.close();
        }
        catch (Exception e)
        {
            System.out.println("Folder / _getFolder_pagesCount / db_folder.getPagesCount error / 0 page");
            pagesCount = 0;
        }
        return  pagesCount;
    }

    // List all folder tables
    public void listAllFolderTables(AppCompatActivity act)
    {
        DB_drawer dB_drawer = new DB_drawer(act);
        // list all folder tables
        int foldersCount = dB_drawer.getFoldersCount(true);
        for(int folderPos=0; folderPos<foldersCount; folderPos++)
        {
            String folderTitle = dB_drawer.getFolderTitle(folderPos,true);
            setFocus_folderPos(folderPos);

            // list all folder tables
            int folderTableId = dB_drawer.getFolderTableId(folderPos,true);
            System.out.println("--- folder table Id = " + folderTableId +
                    ", folder title = " + folderTitle);

            DB_folder db_folder = new DB_folder(folderTableId);

            int pagesCount = db_folder.getPagesCount(true);

            for(int tabPos=0; tabPos<pagesCount; tabPos++)
            {
                tabsHost.setFocus_tabPos(tabPos);
                int pageId = db_folder.getPageId(tabPos, true);
                int pageTableId = db_folder.getPageTableId(tabPos, true);
                String pageTitle = db_folder.getPageTitle(tabPos, true);
                System.out.println("   --- page Id = " + pageId);
                System.out.println("   --- page table Id = " + pageTableId);
                System.out.println("   --- page title = " + pageTitle);

                try {
                    DB_page db_page = new DB_page(pageTableId);
                    db_page.open();
                    db_page.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Add new folder
     *
     */
    static private int mAddFolderAt;
    static private SharedPreferences mPref_add_new_folder_location;
    public void addNewFolder(final AppCompatActivity act, final int newTableId, final SimpleDragSortCursorAdapter folderAdapter)
    {
        // get folder name
        final String hintFolderName = act.getResources()
                .getString(R.string.default_folder_name)
                .concat(String.valueOf(newTableId));

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.add_new_folder, null);
        final TouchableEditText editFolderName = (TouchableEditText) rootView.findViewById(R.id.new_folder_name);

        // set hint
//	    ((EditText)editFolderName).setHint(hintFolderName);

        // set default text
        editFolderName.setText(hintFolderName);

        // request cursor
        editFolderName.requestFocus();

        // set cursor
//        try {
//            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
//            f.setAccessible(true);
//            f.set(editFolderName, R.drawable.cursor);
//        } catch (Exception ignored) {
//        }

        // set hint
//        editFolderName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (hasFocus) {
//                    ((EditText) v).setHint(hintFolderName);
//                }
//            }
//        });

//        editFolderName.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                ((EditText) v).setText(hintFolderName);
//                ((EditText) v).setSelection(hintFolderName.length());
//                v.performClick();
//                return false;
//            }
//        });

        // radio buttons
        final RadioGroup mRadioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_new_folder_at);

        // get new folder location option
        mPref_add_new_folder_location = act.getSharedPreferences("add_new_folder_option", 0);
        if (mPref_add_new_folder_location.getString("KEY_ADD_NEW_FOLDER_TO", "bottom").equalsIgnoreCase("top")) {
            mRadioGroup.check(mRadioGroup.getChildAt(0).getId());
            mAddFolderAt = 0;
        } else if (mPref_add_new_folder_location.getString("KEY_ADD_NEW_FOLDER_TO", "bottom").equalsIgnoreCase("bottom")) {
            mRadioGroup.check(mRadioGroup.getChildAt(1).getId());
            mAddFolderAt = 1;
        }

        // update new folder location option
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup RG, int id) {
                mAddFolderAt = mRadioGroup.indexOfChild(mRadioGroup.findViewById(id));
                if (mAddFolderAt == 0) {
                    mPref_add_new_folder_location.edit().putString("KEY_ADD_NEW_FOLDER_TO", "top").apply();
                } else if (mAddFolderAt == 1) {
                    mPref_add_new_folder_location.edit().putString("KEY_ADD_NEW_FOLDER_TO", "bottom").apply();
                }
            }
        });

        // set view to dialog
        AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
        builder1.setView(rootView);
        final AlertDialog dialog1 = builder1.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) rootView.findViewById(R.id.new_folder_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // add button
        Button btnAdd = (Button) rootView.findViewById(R.id.new_folder_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DB_drawer db_drawer = new DB_drawer(act);

                String folderTitle;
                if (!Util.isEmptyString(editFolderName.getText().toString()))
                    folderTitle = editFolderName.getText().toString();
                else
                    folderTitle = act.getResources().getString(R.string.default_folder_name).concat(String.valueOf(newTableId));

                mFolderTitles.add(folderTitle);
                // insert new drawer Id and Title
                db_drawer.insertFolder(newTableId, folderTitle,true );

                // insert folder table
                db_drawer.insertFolderTable(newTableId, true);

                // insert initial page table after Add new folder
                if(Define.INITIAL_PAGES_COUNT > 0)
                {
                    for(int i = 1; i<= Define.INITIAL_PAGES_COUNT; i++)
                    {
                        DB_folder dB_folder = new DB_folder(newTableId);
                        int style = Util.getNewPageStyle(act);
                        dB_folder.insertPage(DB_folder.getFocusFolder_tableName(),
                                Define.getTabTitle(act,1),
                                i,
                                style,
                                true );

                        dB_folder.insertPageTable(dB_folder,newTableId, i, true);
                    }
                }

                // add new folder to the top
                if(mAddFolderAt == 0)
                {
                    int startCursor = db_drawer.getFoldersCount(true)-1;
                    int endCursor = 0;

                    //reorder data base storage for ADD_NEW_TO_TOP option
                    int loop = Math.abs(startCursor-endCursor);
                    for(int i=0;i< loop;i++)
                    {
                        swapFolderRows(startCursor,endCursor);
                        if((startCursor-endCursor) >0)
                            endCursor++;
                        else
                            endCursor--;
                    }

                    // update focus folder position
                    if(db_drawer.getFoldersCount(true)==1)
                        setFocus_folderPos(0);
                    else
                        setFocus_folderPos(getFocus_folderPos()+1);

                    // update focus folder table Id for Add to top
                    Pref.setPref_focusView_folder_tableId(act,db_drawer.getFolderTableId(getFocus_folderPos(),true) );

                    // update playing highlight if needed
                    if(mMediaPlayer != null)
                        MainAct.mPlaying_folderPos++;
                }

                // recover focus folder table Id
                DB_folder.setFocusFolder_tableId(Pref.getPref_focusView_folder_tableId(act));

                folderAdapter.notifyDataSetChanged();

                //end
                dialog1.dismiss();
                updateFocus_folderPosition();

                act.invalidateOptionsMenu();
            }
        });
    }

}