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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.page.Page_recycler;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2017/10/7.
 */

public class MainUi {

    MainUi(){}

    /**
     * Add note with Intent link
     */
    String title;
    String addNote_IntentLink(Intent intent,final AppCompatActivity act)
    {
        Bundle extras = intent.getExtras();
        String pathOri = null;
        String path;
        if(extras != null)
            pathOri = extras.getString(Intent.EXTRA_TEXT);
        else
            System.out.println("MainUi / _addNote_IntentLink / extras == null");

        path = pathOri;

        if(!Util.isEmptyString(pathOri))
        {
            System.out.println("MainUi / _addNote_IntentLink / pathOri = " + pathOri);
            // for SoundCloud case, path could contain other strings before URI path
            if(pathOri.contains("http"))
            {
                String[] str = pathOri.split("http");

                for(int i=0;i< str.length;i++)
                {
                    if(str[i].contains("://"))
                        path = "http".concat(str[i]);
                }
            }

            DB_drawer db_drawer = new DB_drawer(act);
            DB_folder db_folder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(MainAct.mAct));
            int folders_count = db_drawer.getFoldersCount(true);
            int pages_count = db_folder.getPagesCount(true);
            if((folders_count == 0) || (pages_count == 0))            {
                Toast.makeText(act,"No folder or no page yet, please add a new one in advance.",Toast.LENGTH_LONG).show();
                return null;
            }

            System.out.println("MainUi / _addNote_IntentLink / path = " + path);
            DB_page dB_page = new DB_page(act,Pref.getPref_focusView_page_tableId(MainAct.mAct));
            dB_page.insertNote("", "", "", "", path, "", 0, (long) 0);// add new note, get return row Id

            // save to top or to bottom
            final String link =path;
            int count = dB_page.getNotesCount(true);
            SharedPreferences pref_show_note_attribute = act.getSharedPreferences("add_new_note_option", 0);

            // swap if new position is top
            boolean isAddedToTop = pref_show_note_attribute.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top");
            if( isAddedToTop && (count > 1) )
            {
                Page_recycler.swapTopBottom();
            }

            return title;
        }
        else
            return null;
    }

}
