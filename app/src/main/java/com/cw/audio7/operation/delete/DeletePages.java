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
import android.content.DialogInterface;
import android.content.Intent;
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
import android.database.Cursor;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.operation.List_selectPage;
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.util.BaseBackPressedListener;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class DeletePages extends Fragment {
    TextView title;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	List_selectPage list_selPage;
	public static View rootView;
    AppCompatActivity act;

	public DeletePages(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.select_page_list, container, false);
        act = MainAct.mAct;

        // title
        title = (TextView) rootView.findViewById(R.id.select_list_title);
        title.setText(R.string.config_select_pages_delete_title);

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    list_selPage.selectAllPages(true);
                else
                    list_selPage.selectAllPages(false);
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
                if(list_selPage.mChkNum > 0)
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
                                    doDeletePages();
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
            if(FolderUi.getFolder_pagesCount(act,FolderUi.getFocus_folderPos()) == 0)
            {
                getActivity().finish();
                Intent intent  = new Intent(act,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().startActivity(intent);
            }
            else
                act.getSupportFragmentManager().popBackStack();
            }
        });

        //show list for selection
        list_selPage = new List_selectPage(act,rootView , mListView);

		((MainAct)act).setOnBackPressedListener(new BaseBackPressedListener(act));

		return rootView;
	}


	@Override
	public void onPause() {
		super.onPause();
	}


	void doDeletePages()
    {
        DB_folder mDbFolder = new DB_folder(MainAct.mAct,DB_folder.getFocusFolder_tableId());
        mDbFolder.open();
        for(int i = 0; i< list_selPage.count; i++)
        {
            if (list_selPage.mCheckedTabs.get(i))
            {
                int pageTableId = mDbFolder.getPageTableId(i, false);
                mDbFolder.dropPageTable(pageTableId,false);

                int pageId = mDbFolder.getPageId(i,false);

                // delete page row
                mDbFolder.deletePage(DB_folder.getFocusFolder_tableName(),pageId,false);
            }
        }
        mDbFolder.close();

        mDbFolder.open();
        // check if only one page left
        int pgsCnt = mDbFolder.getPagesCount(false);
        if(pgsCnt > 0)
        {
            int newFirstPageTblId=0;
            int i=0;
            Cursor mPageCursor = mDbFolder.getPageCursor();
            while(i < pgsCnt)
            {
                mPageCursor.moveToPosition(i);
                if(mPageCursor.isFirst())
                    newFirstPageTblId = mDbFolder.getPageTableId(i,false);
                i++;
            }
            System.out.println("TabsHost / _postDeletePage / newFirstPageTblId = " + newFirstPageTblId);
            Pref.setPref_focusView_page_tableId(act, newFirstPageTblId);
        }
        else if(pgsCnt ==0)
            Pref.setPref_focusView_page_tableId(act, 0);//todo 0 OK?

        mDbFolder.close();

        // set scroll X
//        int scrollX = 0; //over the last scroll X
//        Pref.setPref_focusView_scrollX_byFolderTableId(act, scrollX );

        if(BackgroundAudioService.mMediaPlayer != null)
        {
            Audio_manager.stopAudioPlayer();
            Audio_manager.mAudioPos = 0;
            Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
        }

        list_selPage = new List_selectPage(act,rootView , mListView);
    }

}