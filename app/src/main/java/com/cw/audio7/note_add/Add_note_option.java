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

package com.cw.audio7.note_add;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.cw.audio7.R;
import com.cw.audio7.drawer.Drawer;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.note_add.add_audio.Add_audio_all;
import com.cw.audio7.note_add.add_recording.Add_recording_act;
import com.cw.audio7.note_add.add_audio.Add_audio_1by1;
import com.cw.audio7.note_add.add_audio.Add_audio_byFolder;
import com.cw.audio7.util.preferences.Pref;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;


/**
 * Created by cw on 2017/10/7.
 */
public class Add_note_option {
    int option_id;
    int option_drawable_id;
    int option_string_id;
    AppCompatActivity act;
    Menu menu;
    Drawer drawer;
    Folder folder;

    public Add_note_option(AppCompatActivity _act, Menu _menu, Drawer _drawer) {
        act = _act;
        menu = _menu;
        drawer = _drawer;
        folder = drawer.folder;
    }

    Add_note_option(int id, int draw_id, int string_id)
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
    static List<Add_note_option> addNoteList;

    private final static int ID_NEW_RECORDING = 3;
    private final static int ID_NEW_AUDIO = 4;
    private final static int ID_NEW_AUDIO_1by1 = 5;
    private final static int ID_NEW_AUDIO_byFolder = 6;
    private final static int ID_NEW_AUDIO_all = 7;
    private final static int ID_NEW_BACK = 11;
    private final static int ID_NEW_SETTING = 12;

    public void createSelection(AppCompatActivity act)
    {

        System.out.println("Add_note_option / _createSelection");
        AbsListView gridView;

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.option_grid, null);

        addNoteList = new ArrayList<>();

        int pagesCount = folder.getFolder_pagesCount(act, Folder.getFocus_folderPos());

        int foldersCount = Drawer.getFoldersCount(act);

        // audio by all
        addNoteList.add(new Add_note_option(ID_NEW_AUDIO_all,
                R.drawable.ic_audio_unselected,
                R.string.note_ready_audio_by_all));

        // audio by folder
        if (foldersCount > 0)
            addNoteList.add(new Add_note_option(ID_NEW_AUDIO_byFolder,
                    R.drawable.ic_audio_unselected,
                    R.string.note_ready_audio_byFolder));

        // audio 1by1
        if (pagesCount > 0)
            addNoteList.add(new Add_note_option(ID_NEW_AUDIO_1by1,
                    R.drawable.ic_audio_unselected,
                    R.string.note_ready_audio_1by1));

        // recording
        if (pagesCount > 0)
            addNoteList.add(new Add_note_option(ID_NEW_RECORDING,
                    R.drawable.ic_mic,
                    R.string.note_recording));

        // audio app
        if (pagesCount > 0)
            addNoteList.add(new Add_note_option(ID_NEW_AUDIO,
                    R.drawable.ic_audio_unselected,
                    R.string.note_ready_audio));

        // Setting
        addNoteList.add(new Add_note_option(ID_NEW_SETTING,
                android.R.drawable.ic_menu_preferences,
                R.string.settings));

        // Back
        addNoteList.add(new Add_note_option(ID_NEW_BACK,
                R.drawable.ic_menu_back,
                R.string.btn_Cancel));

        gridView = (GridView) rootView.findViewById(R.id.option_grid_view);

        // check if directory is created AND not empty
        if( (addNoteList != null  ) && (addNoteList.size() > 0))
        {
            GridIconAdapter mGridIconAdapter = new GridIconAdapter(act);
            gridView.setAdapter(mGridIconAdapter);
        }
        else
        {
            act.finish();
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("MainUi / _addNewNote / _OnItemClickListener / position = " + position +" id = " + id);
                startAddNoteOption(act, addNoteList.get(position).option_id);
            }
        });

        // set view to dialog
        AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
        builder1.setView(rootView);
        dlgAddNew = builder1.create();
        dlgAddNew.show();
    }

    private static AlertDialog dlgAddNew;

    private void startAddNoteOption(AppCompatActivity act, int option)
    {
        System.out.println("MainUi / _startAddNoteOption / option = " + option);

        SharedPreferences mPref_add_new_note_location = act.getSharedPreferences("add_new_note_option", 0);
        boolean bTop = mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top");
        boolean bDirectory = mPref_add_new_note_location.getString("KEY_ADD_DIRECTORY","no").equalsIgnoreCase("yes");

        switch (option) {

            case ID_NEW_RECORDING:
            {
                Intent intent = new Intent(act, Add_recording_act.class);
                if( bTop && !bDirectory )
                    intent.putExtra("EXTRA_ADD_EXIST", "single_to_top");
                else if(!bTop && !bDirectory)
                    intent.putExtra("EXTRA_ADD_EXIST", "single_to_bottom");
                else if(bTop && bDirectory)
                    intent.putExtra("EXTRA_ADD_EXIST", "directory_to_top");
                else if(!bTop && bDirectory)
                    intent.putExtra("EXTRA_ADD_EXIST", "directory_to_bottom");

                act.startActivity(intent);
            }
            break;

            case ID_NEW_AUDIO:
            {
                Intent intent = new Intent(act, Note_addAudio.class);
                if( bTop && !bDirectory )
                    intent.putExtra("EXTRA_ADD_EXIST", "single_to_top");
                else if(!bTop && !bDirectory)
                    intent.putExtra("EXTRA_ADD_EXIST", "single_to_bottom");
                else if(bTop && bDirectory)
                    intent.putExtra("EXTRA_ADD_EXIST", "directory_to_top");
                else if(!bTop && bDirectory)
                    intent.putExtra("EXTRA_ADD_EXIST", "directory_to_bottom");

                act.startActivity(intent);
            }
            break;

            case ID_NEW_AUDIO_1by1:
            {
                // replace fragment
                dlgAddNew.dismiss();

                //hide the menu
                menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                menu.setGroupVisible(R.id.group_notes, false);
                menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                menu.setGroupVisible(R.id.group_pages_and_more, false);

                Add_audio_1by1 add_audio1by1 = new Add_audio_1by1(act, folder);
                FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();

                transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                transaction.replace(R.id.content_frame, add_audio1by1, "add_audio").addToBackStack(null).commit();
            }
            break;

            case ID_NEW_AUDIO_byFolder:
            {
                // replace fragment
                dlgAddNew.dismiss();
                Add_audio_byFolder add_audio_byFolder = new Add_audio_byFolder(act, folder);
                FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();

                //hide the menu
                menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                menu.setGroupVisible(R.id.group_notes, false);
                menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                menu.setGroupVisible(R.id.group_pages_and_more, false);

                transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                transaction.replace(R.id.content_frame, add_audio_byFolder, "add_audio").addToBackStack(null).commit();
            }
            break;

            case ID_NEW_AUDIO_all:
            {
                // replace fragment
                dlgAddNew.dismiss();

                //hide the menu
                menu.findItem(R.id.ADD_NEW_NOTE).setVisible(false);
                menu.setGroupVisible(R.id.group_notes, false);
                menu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible(false);
                menu.setGroupVisible(R.id.group_pages_and_more, false);

                // do Add all
                Pref.setPref_will_create_default_content(act,true);
                Add_audio_all add_audio_all = new Add_audio_all(drawer); //todo More check
                FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                transaction.replace(R.id.content_frame, add_audio_all, "add_audio").addToBackStack(null).commit();
            }
            break;

            case ID_NEW_BACK:
            {
                dlgAddNew.dismiss();
            }
            break;

            case ID_NEW_SETTING:
                new Note_addNew_setting(act);
            break;

            // default
            default:
                break;
        }

    }


    /**
     * Created by cw on 2017/10/7.
     */
    static class GridIconAdapter extends BaseAdapter {
        private AppCompatActivity act;
        GridIconAdapter(AppCompatActivity fragAct){act = fragAct;}

        @Override
        public int getCount() {
            return addNoteList.size();
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
                view.setTag(holder);

                // set grid item background color
                // local audio
                if( (position == 0) || (position == 1) || (position == 2))
                    view.setBackgroundColor( act.getResources().getColor(R.color.localGrid));
                // recording, app chooser, setting
                else if( (position == 3)|| (position == 4) || (position == 5))
                    view.setBackgroundColor( act.getResources().getColor(R.color.appGrid));
                // cancel
                else if((position == 6))
                    view.setBackgroundColor( act.getResources().getColor(R.color.otherGrid));

            } else {
                holder = (ViewHolder) view.getTag();
            }

            Drawable drawable = act.getResources().getDrawable(addNoteList.get(position).option_drawable_id);
            holder.imageView.setImageDrawable(drawable);
            holder.text.setText(addNoteList.get(position).option_string_id);
            return view;
        }

        private class ViewHolder {
            ImageView imageView;
            TextView text;
        }
    }
}
