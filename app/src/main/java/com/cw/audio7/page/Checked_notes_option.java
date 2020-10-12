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

package com.cw.audio7.page;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.audio.Audio7Player;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.audio.Audio_manager;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2017/11/4.
 */
public class Checked_notes_option {
    private int option_id;
    int option_drawable_id;
    int option_string_id;
    public static int MOVE_TO = 0;
    private static int COPY_TO = 1;
    private DB_page mDb_page;
    private AppCompatActivity mAct;

    public Checked_notes_option(AppCompatActivity act){
        mDb_page = new DB_page(act, TabsHost.getCurrentPageTableId());
        mAct = act;
    }

    private Checked_notes_option(int id, int draw_id, int string_id)
    {
        this.option_id = id;
        this.option_drawable_id = draw_id;
        this.option_string_id = string_id;
    }

    /**
     *
     * 	Add new note
     *
     */
    static List<Checked_notes_option> checkedOperationList;

    private final static int BACK = 0;
    private final static int CHECK_ALL = 1;
    private final static int UN_CHECK_ALL = 2;
    private final static int INVERT_SELECTED = 3;
    private final static int MOVE_CHECKED_NOTE = 4;
    private final static int COPY_CHECKED_NOTE = 5;
    private final static int DELETE_CHECKED_NOTE = 7;


    public void open_option_grid(final AppCompatActivity act)
    {
        AbsListView gridView;

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.option_grid, null);

        checkedOperationList = new ArrayList<>();

        // Back
        checkedOperationList.add(new Checked_notes_option(BACK,
                R.drawable.ic_menu_back,
                R.string.view_note_button_back));

        // CHECK_ALL
        checkedOperationList.add(new Checked_notes_option(CHECK_ALL,
                R.drawable.btn_check_on_holo_dark,
                R.string.checked_notes_check_all));

        // UN_CHECK_ALL
        checkedOperationList.add(new Checked_notes_option(UN_CHECK_ALL,
                R.drawable.btn_check_off_holo_dark,
                R.string.checked_notes_uncheck_all));

        // INVERT_SELECTED
        checkedOperationList.add(new Checked_notes_option(INVERT_SELECTED,
//                R.drawable.btn_check_on_focused_holo_dark,
                android.R.drawable.ic_menu_set_as,
                R.string.checked_notes_invert_selected));

        // MOVE_CHECKED_NOTE
        checkedOperationList.add(new Checked_notes_option(MOVE_CHECKED_NOTE,
                R.drawable.ic_menu_goto,
                R.string.checked_notes_move_to));

        // COPY_CHECKED_NOTE
        checkedOperationList.add(new Checked_notes_option(COPY_CHECKED_NOTE,
                R.drawable.ic_menu_copy_holo_dark,
                R.string.checked_notes_copy_to));

        // DELETE_CHECKED_NOTE
        checkedOperationList.add(new Checked_notes_option(DELETE_CHECKED_NOTE,
                R.drawable.ic_menu_clear_playlist,
                R.string.checked_notes_delete));


        gridView = (GridView) rootView.findViewById(R.id.option_grid_view);

        // check if directory is created AND not empty
        if( (checkedOperationList != null  ) && (checkedOperationList.size() > 0))
        {
            GridIconAdapter mGridIconAdapter = new GridIconAdapter(act,noItemChecked());
            gridView.setAdapter(mGridIconAdapter);
        }
        else
        {
            act.finish();
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("Checked_notes_option / _open_option_grid / _onItemClick / position = " + position +" id = " + id);
                if(noItemChecked() && (position > CHECK_ALL))
                    Toast.makeText(act,R.string.delete_checked_no_checked_items,Toast.LENGTH_SHORT).show();
                else
                    startCheckedOperation(act, checkedOperationList.get(position).option_id);
            }
        });

        // set view to dialog
        AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
        builder1.setView(rootView);
        dlgAddNew = builder1.create();
        dlgAddNew.show();
    }

    private static AlertDialog dlgAddNew;

    private void startCheckedOperation(AppCompatActivity act, int option)
    {
        System.out.println("Checked_notes_option / _startCheckedOperation / option = " + option);

        switch (option) {
            case BACK:
                dlgAddNew.dismiss();
                break;

            case CHECK_ALL:
                checkAll(1);
                dlgAddNew.dismiss();
                break;

            case UN_CHECK_ALL:
                checkAll(0);
                dlgAddNew.dismiss();
                break;

            case INVERT_SELECTED:
                invertSelected();
                dlgAddNew.dismiss();
                break;

            case MOVE_CHECKED_NOTE:
            case COPY_CHECKED_NOTE:
                if(!noItemChecked())
                {
                    int count = mDb_page.getCheckedNotesCount();
                    String copyItemsTitle[] = new String[count];
                    String copyItemsAudio[] = new String[count];
                    String copyItemsBody[] = new String[count];
                    int cCopy = 0;

                    mDb_page.open();
                    int noteCount = mDb_page.getNotesCount(false);
                    for(int i=0; i<noteCount; i++)
                    {
                        if(mDb_page.getNoteMarking(i,false) == 1)
                        {
                            copyItemsTitle[cCopy] = mDb_page.getNoteTitle(i,false);
                            copyItemsAudio[cCopy] = mDb_page.getNoteAudioUri(i,false);
                            copyItemsBody[cCopy] = mDb_page.getNoteBody(i,false);
                            cCopy++;
                        }
                    }
                    mDb_page.close();

                    if(option == MOVE_CHECKED_NOTE)
                        operateCheckedTo(mAct,copyItemsTitle,  copyItemsAudio, copyItemsBody,  MOVE_TO); // move to
                    else if(option == COPY_CHECKED_NOTE)
                        operateCheckedTo(mAct,copyItemsTitle,  copyItemsAudio, copyItemsBody,  COPY_TO);// copy to

                }
                else
                    Toast.makeText(act,
                            R.string.delete_checked_no_checked_items,
                            Toast.LENGTH_SHORT)
                            .show();
                dlgAddNew.dismiss();
                break;

            case DELETE_CHECKED_NOTE:
                if(!noItemChecked())
                    deleteCheckedNotes(act);
                else
                    Toast.makeText(act,
                            R.string.delete_checked_no_checked_items,
                            Toast.LENGTH_SHORT)
                            .show();
                dlgAddNew.dismiss();
                break;

            // default
            default:
                break;
        }
    }

    /**
     *  check all or un_check all
     */
    private void checkAll(int action)
    {
        System.out.println("Checked_notes_option / _checkAll / action = " + action);
        boolean bStopAudio = false;

        mDb_page.open();
        int count = mDb_page.getNotesCount(false);
        for(int i=0; i<count; i++)
        {
            Long rowId = mDb_page.getNoteId(i,false);
            String noteTitle = mDb_page.getNoteTitle(i,false);
            String audioUri = mDb_page.getNoteAudioUri(i,false);
            String noteBody = mDb_page.getNoteBody(i,false);
            mDb_page.updateNote(rowId, noteTitle, audioUri, noteBody , action ,false);// action 1:check all, 0:uncheck all
            // Stop if unmarked item is at playing state
            if((Audio_manager.mAudioPos == i) && (action == 0) )
                bStopAudio = true;
        }
        mDb_page.close();

        if(bStopAudio)
            UtilAudio.stopAudioIfNeeded();

        // update audio play list
        if(PageUi.isAudioPlayingPage())
            Audio7Player.prepareAudioInfo();

        TabsHost.reloadCurrentPage();

        TabsHost.showFooter(MainAct.mAct);
    }

    /**
     *  Invert Selected
     */
    private void invertSelected()
    {
        boolean bStopAudio = false;
        mDb_page.open();
        int count = mDb_page.getNotesCount(false);
        for(int i=0; i<count; i++)
        {
            Long rowId = mDb_page.getNoteId(i,false);
            String noteTitle = mDb_page.getNoteTitle(i,false);
            String audioUri = mDb_page.getNoteAudioUri(i,false);
            String noteBody = mDb_page.getNoteBody(i,false);
            long marking = (mDb_page.getNoteMarking(i,false)==1)?0:1;
            mDb_page.updateNote(rowId, noteTitle, audioUri, noteBody , marking, false);// action 1:check all, 0:uncheck all
            // Stop if unmarked item is at playing state
            if((Audio_manager.mAudioPos == i) && (marking == 0) )
                bStopAudio = true;
        }
        mDb_page.close();

        if(bStopAudio)
            UtilAudio.stopAudioIfNeeded();

        // update audio play list
        if(PageUi.isAudioPlayingPage())
            Audio7Player.prepareAudioInfo();

        TabsHost.reloadCurrentPage();
        TabsHost.showFooter(MainAct.mAct);
    }


    /**
     *   operate checked to: move to, copy to
     *
     */
    private void operateCheckedTo(final AppCompatActivity act,final String[] copyItemsTitle,
                                  final String[] copyItemsAudio, final String[] copyItemsBody,  final int action)
    {
        //list all pages
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db_folder = new DB_folder(act, focusFolder_tableId);
        db_folder.open();
        int tabCount = db_folder.getPagesCount(false);
        final String[] pageNames = new String[tabCount];
        final int[] pageTableIds = new int[tabCount];
        for(int i=0;i<tabCount;i++)
        {
            pageNames[i] = db_folder.getPageTitle(i,false);
            pageTableIds[i] = db_folder.getPageTableId(i,false);
        }
        db_folder.close();

        // add * mark to current page
        pageNames[TabsHost.getFocus_tabPos()] = pageNames[TabsHost.getFocus_tabPos()] + " *";

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //keep focus page table id
                int srcPageTableId = DB_page.getFocusPage_tableId();

                //copy checked item to destination page
                int destPageTableId = pageTableIds[which];
                DB_page.setFocusPage_tableId(destPageTableId);
                for(int i=0;i< copyItemsTitle.length;i++)
                {
                    // move to same page is not allowed
                    if(!((action == MOVE_TO) && (srcPageTableId == destPageTableId)))
                        mDb_page.insertNote(copyItemsTitle[i], copyItemsAudio[i], copyItemsBody[i],1);
                }

                //recover table Id of original page
                if((action == MOVE_TO) && (srcPageTableId != destPageTableId))
                {
                    DB_page.setFocusPage_tableId(srcPageTableId);
                    mDb_page.open();
                    int count = mDb_page.getNotesCount(false);

                    //delete checked items that were moved
                    for(int i=0; i<count; i++)
                    {
                        if(mDb_page.getNoteMarking(i,false) == 1)
                        {
                            mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
                            // update playing highlight
                            UtilAudio.stopAudioIfNeeded();
                        }
                    }
                    mDb_page.close();

                    TabsHost.reloadCurrentPage();
                    TabsHost.showFooter(MainAct.mAct);
                }
                else if(action == COPY_TO)
                {
                    DB_page.setFocusPage_tableId(srcPageTableId);
                    TabsHost.reloadCurrentPage();
                    TabsHost.showFooter(MainAct.mAct);
                }
                dialog.dismiss();
            }
        };

        if(action == MOVE_TO)
            builder.setTitle(R.string.checked_notes_move_to_dlg);
        else if(action == COPY_TO)
            builder.setTitle(R.string.checked_notes_copy_to_dlg);

        builder.setSingleChoiceItems(pageNames, -1, listener)
                .setNegativeButton(R.string.btn_Cancel, null);

        // override onShow to mark current page status
        AlertDialog alertDlg = builder.create();
        alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dlgInterface) {
                // add mark for current page
                Util util = new Util(act);
                util.addMarkToCurrentPage(dlgInterface,action);
            }
        });
        alertDlg.show();
    }


    /**
     * delete checked notes
     */
    private void deleteCheckedNotes(AppCompatActivity act)
    {
        final Context context = act;

        Util util = new Util(act);
        util.vibrate();

        // show warning dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_checked_note_title)
                .setMessage(R.string.delete_checked_message)
                .setNegativeButton(R.string.btn_Cancel,
                        new DialogInterface.OnClickListener()
                        {	@Override
                        public void onClick(DialogInterface dialog, int which)
                        {/*cancel*/} })
                .setPositiveButton(R.string.btn_OK,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                mDb_page.open();
                                int count = mDb_page.getNotesCount(false);
                                for(int i=0; i<count; i++)
                                {
                                    if(mDb_page.getNoteMarking(i,false) == 1)
                                        mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
                                }
                                mDb_page.close();

                                // Stop Play/Pause if current tab's item is played and is not at Stop state
                                if(Audio_manager.mAudioPos == Page_recycler.mHighlightPosition)
                                    UtilAudio.stopAudioIfNeeded();

                                TabsHost.reloadCurrentPage();
                                TabsHost.showFooter(MainAct.mAct);
                            }
                        });

        AlertDialog d = builder.create();
        d.show();
    }

    private boolean noItemChecked()
    {
        DB_page mDb_page = new DB_page(mAct, TabsHost.getCurrentPageTableId());
        int checkedItemCount = mDb_page.getCheckedNotesCount();
        return (checkedItemCount == 0);
    }

    /**
     * Created by cw on 2017/10/7.
     */
    static class GridIconAdapter extends BaseAdapter {
        private AppCompatActivity act;
        boolean hasNoCheckedItems;
        GridIconAdapter(AppCompatActivity fragAct,boolean hasNoCheckedItems)
        {
            this.hasNoCheckedItems = hasNoCheckedItems;
            act = fragAct;
        }

        @Override
        public int getCount() {
            return checkedOperationList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = act.getLayoutInflater().inflate(R.layout.add_note_grid_item, parent, false);
                holder = new ViewHolder();
                assert view != null;
                holder.imageView = (ImageView) view.findViewById(R.id.grid_item_image);
                holder.text = (TextView) view.findViewById(R.id.grid_item_text);

                if( hasNoCheckedItems && (position > CHECK_ALL))
                    view.setBackgroundColor(Color.DKGRAY);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Drawable drawable = act.getResources().getDrawable(checkedOperationList.get(position).option_drawable_id);
            holder.imageView.setImageDrawable(drawable);
            holder.text.setText(checkedOperationList.get(position).option_string_id);
            return view;
        }

        private class ViewHolder {
            ImageView imageView;
            TextView text;
        }
    }
}
