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

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.widget.AdapterView;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.util.preferences.Pref;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import static com.cw.audio7.main.MainAct.mFolderUi;

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

    Folder(){};

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
                mFolderUi.swapFolderRows(startPosition,endPosition);
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
            mFolderUi.updateFocus_folderPosition();
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
    public static class FolderListener_click implements AdapterView.OnItemClickListener
    {
        AppCompatActivity act;
        FolderListener_click(AppCompatActivity act_){act = act_;}

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            System.out.println("Folder / _onItemClickListener / position = " + position);
            mFolderUi.setFocus_folderPos(position);

            DB_drawer db_drawer = new DB_drawer(act);
            Pref.setPref_focusView_folder_tableId(act,db_drawer.getFolderTableId(position,true) );

            mFolderUi.openFolder();
        }
    }

    // long click
    public static class FolderListener_longClick implements DragSortListView.OnItemLongClickListener
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
            mFolderUi.editFolder(act,position, adapter);
            return true;
        }
    }
}