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

package com.cw.audio7.operation.delete;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.operation.List_selectFolder;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class DeleteFolders extends Fragment {
	Context mContext;
	TextView title;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	List_selectFolder list_selFolder;
	View rootView;
    AppCompatActivity act;

	public DeleteFolders(){}

    public DeleteFolders(AppCompatActivity _act){
	    act = _act;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = act;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.select_page_list, container, false);

		// title
		title = (TextView) rootView.findViewById(R.id.select_list_title);
        title.setText(R.string.config_select_folders_title);

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    list_selFolder.selectAllPages(true);
                else
                    list_selFolder.selectAllPages(false);
            }
        });

        // list view: selecting which pages to send
        mListView = (ListView)rootView.findViewById(R.id.listView1);

        // OK button: click to do next
        btnSelPageOK = (Button) rootView.findViewById(R.id.btnSelPageOK);
        btnSelPageOK.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        btnSelPageOK.setText(R.string.config_delete_DB_btn);
        btnSelPageOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list_selFolder.mChkNum > 0)
                {
                    Util util = new Util(act);
                    util.vibrate();

                    AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
                    builder1.setTitle(R.string.confirm_dialog_title)
                            .setMessage(R.string.confirm_dialog_message_selection)
                            .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener()
                            {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                            {
                                    /*nothing to do*/
                            }
                            })
                            .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener()
                            {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                            {
                                doDeleteFolders();
                            }
                            })
                            .show();//warning:end
                }
                else
                    Toast.makeText(act,
                            R.string.delete_checked_no_checked_items,
                            Toast.LENGTH_SHORT).show();
            }
        });

        // cancel button
        Button btnSelPageCancel = (Button) rootView.findViewById(R.id.btnSelPageCancel);
		btnSelPageCancel.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        btnSelPageCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("DeleteFolders / _btnSelPageCancel");
                DB_drawer db_drawer = new DB_drawer(act);

                int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
                DB_folder db_folder = new DB_folder(focusFolder_tableId);
                if((db_drawer.getFoldersCount(true) == 0) || (db_folder.getPagesCount(true) == 0))
                {
                    System.out.println("DeleteFolders / _btnSelPageCancel / will call MainAct");
                    getActivity().finish();
                    Intent intent  = new Intent(act,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getActivity().startActivity(intent);
                }
                else {
                    System.out.println("DeleteFolders / _btnSelPageCancel / will do _popBackStack");
                    act.getSupportFragmentManager().popBackStack();
                }
                // for pages count = 0 case
                // java.lang.IllegalArgumentException: No view found for id 0x1020011 (android:id/tabcontent) for fragment Page{8ac28af #0 id=0x1020011 tab1}
            }
        });

        //show list for selection
        list_selFolder = new List_selectFolder(act,rootView , mListView);

//        ((MainAct)act).setOnBackPressedListener(new BaseBackPressedListener(act));
//        act.setOnBackPressedListener(new BaseBackPressedListener(act));

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

    /**
     * Do delete folders
     */
	void doDeleteFolders()
    {
        DB_drawer dbDrawer = new DB_drawer(act);

        // drawer DB check
        boolean doDB_check = false;
        if(doDB_check) {
            dbDrawer.open();
            for (int i = 0; i < list_selFolder.count; i++) {
                int folderTableId = dbDrawer.getFolderTableId(i, false);
                System.out.println("DeleteFolders / folderTableId = " + folderTableId);

                int folderId = (int) dbDrawer.getFolderId(i, false);
                System.out.println("DeleteFolders / folderId = " + folderId);
            }
            dbDrawer.close();
        }

        dbDrawer.open();
        for(int i = 0; i< list_selFolder.count; i++)
        {
            if(list_selFolder.mCheckedArr.get(i))
            {
                // get folder table id
                int folderTableId = dbDrawer.getFolderTableId(i,false);

                // remove focus view Key for page table Id
                Pref.removePref_focusView_page_tableId_key(act, folderTableId);

                // 1) delete related page tables
                DB_folder dbFolder = new DB_folder(folderTableId);
                int pgsCnt = dbFolder.getPagesCount(true);
                for (int j = 0; j < pgsCnt; j++) {
                    int pageTableId = dbFolder.getPageTableId(j, true);
                    dbFolder.dropPageTable(folderTableId, pageTableId);
                }

                // 2) delete folder table
                dbDrawer.dropFolderTable(folderTableId,false);
                System.out.println("DeleteFolders / drop folderTableId = " + folderTableId);

                // 3) delete folder Id
                int folderId = (int)dbDrawer.getFolderId(i,false);
                dbDrawer.deleteFolderId(folderId,false);
                System.out.println("DeleteFolders / delete folderId = " + folderId);

                // change focus
                Folder.setFocus_folderPos(0);
            }
        }
        dbDrawer.close();

        // check if only one folder left
        int foldersCnt = dbDrawer.getFoldersCount(true);

        // set focus folder table Id
        dbDrawer.open();
        if(foldersCnt > 0)
        {
            int newFirstFolderTblId=0;
            int i=0;
            Cursor folderCursor = dbDrawer.getFolderCursor();
            while(i < foldersCnt)
            {
                folderCursor.moveToPosition(i);
                if(folderCursor.isFirst())
                    newFirstFolderTblId = dbDrawer.getFolderTableId(i,false);
                i++;
            }
            Pref.setPref_focusView_folder_tableId(act, newFirstFolderTblId);
        }
        else if(foldersCnt ==0)
            Pref.setPref_focusView_folder_tableId(act, 1);
        dbDrawer.close();

        // set scroll X
//        int scrollX = 0; //over the last scroll X
//        Pref.setPref_focusView_scrollX_byFolderTableId(act, scrollX );

        //todo TBD
//        if(BackgroundAudioService.mMediaPlayer != null)
//        {
//            Audio_manager.stopAudioPlayer();
//            Audio_manager.mAudioPos = 0;
//            Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
//        }

        list_selFolder = new List_selectFolder(act,rootView , mListView);
    }

}