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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.audio.Audio_manager;
import com.cw.audio7.audio.BackgroundAudioService;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

/**
 * Created by cw on 2017/10/6.
 */

public class Folder_adapter extends SimpleDragSortCursorAdapter
{
    Folder_adapter(Context context, int layout, Cursor c,
            String[] from, int[] to, int flags)
    {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public int getCount() {
        DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
        int count = db_drawer.getFoldersCount(true);
        return count;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder viewHolder; // holds references to current item's GUI

        // if convertView is null, inflate GUI and create ViewHolder;
        // otherwise, get existing ViewHolder
        if (convertView == null)
        {
            convertView = MainAct.mAct.getLayoutInflater().inflate(R.layout.folder_row, parent, false);

            // set up ViewHolder for this ListView item
            viewHolder = new ViewHolder();
            viewHolder.audioPlayingIcon = (ImageView) convertView.findViewById(R.id.folder_audio);
            viewHolder.folderTitle = (TextView) convertView.findViewById(R.id.folderText);
            viewHolder.dragIcon = (ImageView) convertView.findViewById(R.id.folder_drag);
            convertView.setTag(viewHolder); // store as View's tag
        }
        else // get the ViewHolder from the convertView's tag
            viewHolder = (ViewHolder) convertView.getTag();

        // set highlight of selected drawer
        if( (BackgroundAudioService.mMediaPlayer != null) &&
            (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) &&
            (MainAct.mPlaying_folderPos == position)        )
            viewHolder.audioPlayingIcon.setVisibility(View.VISIBLE);
        else
            viewHolder.audioPlayingIcon.setVisibility(View.GONE);

        DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
        viewHolder.folderTitle.setText(db_drawer.getFolderTitle(position,true));

        // dragger
        SharedPreferences pref = MainAct.mAct.getSharedPreferences("show_note_attribute", 0);;
        if(pref.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no").equalsIgnoreCase("yes"))
            viewHolder.dragIcon.setVisibility(View.VISIBLE);
        else
            viewHolder.dragIcon.setVisibility(View.GONE);

        return convertView;
    }


    private class ViewHolder
    {
        ImageView audioPlayingIcon;
        TextView folderTitle; // refers to ListView item's ImageView
        ImageView dragIcon;
    }
}
