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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import com.cw.audio7.note_add.add_recording.Add_recording_act;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.drawing.Note_drawingAct;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2017/10/7.
 */
public class Add_note_option {
    int option_id;
    int option_drawable_id;
    int option_string_id;

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
    private final static int ID_NEW_BACK = 11;
    private final static int ID_NEW_SETTING = 12;

    public static void createSelection(AppCompatActivity act, boolean permitted)
    {

        System.out.println("Add_note_option / _createSelection");
        AbsListView gridView;

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.option_grid, null);

        // check camera feature
        PackageManager packageManager = act.getPackageManager();

        addNoteList = new ArrayList<>();

            // recording
            addNoteList.add(new Add_note_option(ID_NEW_RECORDING,
//                    android.R.drawable.ic_btn_speak_now,
                    R.drawable.ic_mic,
                    R.string.note_recording));

            // audio
            addNoteList.add(new Add_note_option(ID_NEW_AUDIO,
                    R.drawable.ic_audio_unselected,
                    R.string.note_ready_audio));

        // Back
        addNoteList.add(new Add_note_option(ID_NEW_BACK,
                R.drawable.ic_menu_back,
                R.string.btn_Cancel));

        // Setting
        addNoteList.add(new Add_note_option(ID_NEW_SETTING,
                android.R.drawable.ic_menu_preferences,
                R.string.settings));

        gridView = (GridView) rootView.findViewById(R.id.option_grid_view);

        // check if directory is created AND not empty
        if( (addNoteList != null  ) && (addNoteList.size() > 0))
        {
            GridIconAdapter mGridIconAdapter = new GridIconAdapter(act);
            gridView.setAdapter(mGridIconAdapter);
        }
        else
        {
            Toast.makeText(act,R.string.gallery_toast_no_file, Toast.LENGTH_SHORT).show();
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

    private static void startAddNoteOption(AppCompatActivity act, int option)
    {
        System.out.println("MainUi / _startAddNoteOption / option = " + option);

        SharedPreferences mPref_add_new_note_location = act.getSharedPreferences("add_new_note_option", 0);
        boolean bTop = mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top");
        boolean bDirectory = mPref_add_new_note_location.getString("KEY_ADD_DIRECTORY","no").equalsIgnoreCase("yes");

        switch (option) {

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

            case ID_NEW_BACK:
            {
                dlgAddNew.dismiss();
            }
            break;

            case ID_NEW_SETTING:
            {
                new Note_addNew_option(act);
            }
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
                // text , drawing
                if( (position == 0) || (position == 1) )
                    view.setBackgroundColor( act.getResources().getColor(R.color.textGrid));
                // recording, audio
                else if((position == 2) || (position == 3))
                    view.setBackgroundColor( act.getResources().getColor(R.color.audioGrid));
                // picture, ready picture
                else if ( (position == 4) || (position == 5) )
                    view.setBackgroundColor( act.getResources().getColor(R.color.pictureGrid));
                // video , ready video
                else if((position == 6) || (position == 7))
                    view.setBackgroundColor( act.getResources().getColor(R.color.videoGrid));
                // link
                else if((position == 8) || (position == 9))
                    view.setBackgroundColor( act.getResources().getColor(R.color.linkGrid));
                // others
                else if((position == 10) || (position == 11))
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
