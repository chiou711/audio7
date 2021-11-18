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

package com.cw.audio7.operation;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.util.ColorSet;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by cw on 2017/10/4.
 */

public class List_selectFolder
{
    View mView;
    CheckedTextView mCheckTvSelAll;
    ListView mListView;
    List<String> mListStrArr; // list view string array
    public List<Boolean> mCheckedArr; // checked list view items array
    DB_drawer dB_drawer;
    public int count;
    Activity mAct;

    public List_selectFolder(Activity act, View rootView, View view)
    {
        mAct = act;

        dB_drawer = new DB_drawer(mAct);

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new View.OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    selectAllPages(true);
                else
                    selectAllPages(false);
            }
        });

        // list view: selecting which pages to send
        mListView = (ListView)view;
        showFolderList(rootView);
    }

    // select all pages
    public void selectAllPages(boolean enAll)
    {
        mChkNum = 0;

        dB_drawer.open();
        count = dB_drawer.getFoldersCount(false);
        for(int i = 0; i< count; i++)
        {
            CheckedTextView chkTV = (CheckedTextView) mListView.findViewById(R.id.checkTV);
            mCheckedArr.set(i, enAll);
            mListStrArr.set(i, dB_drawer.getFolderTitle(i,false));

            int style = 1;

            if( enAll)
                chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                R.drawable.btn_check_on_holo_light:
                R.drawable.btn_check_on_holo_dark,0,0,0);
            else
                chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                R.drawable.btn_check_off_holo_light:
                R.drawable.btn_check_off_holo_dark,0,0,0);
        }
        dB_drawer.close();

        mChkNum = (enAll == true)? count : 0;

        // set list adapter
        ListAdapter listAdapter = new ListAdapter(mAct, mListStrArr);

        // list view: set adapter
        mListView.setAdapter(listAdapter);
    }

    // show list for Select
    public int mChkNum;
    private void showFolderList(View root)
    {
        mChkNum = 0;
        // set list view
        mListView = (ListView) root.findViewById(R.id.listView1);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View vw, int position, long id)
            {
                System.out.println("List_selectFolder / _showFolderList / _onItemClick / position = " + position);
                CheckedTextView chkTV = (CheckedTextView) vw.findViewById(R.id.checkTV);
                chkTV.setChecked(!chkTV.isChecked());
                mCheckedArr.set(position, chkTV.isChecked());
                if(mCheckedArr.get(position))
                    mChkNum++;
                else
                    mChkNum--;

                if(!chkTV.isChecked())
                {
                    mCheckTvSelAll.setChecked(false);
                }

                // set for contrast
                int mStyle = 1;
                if( chkTV.isChecked())
                    chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle%2 == 1 ?
                    R.drawable.btn_check_on_holo_light:
                    R.drawable.btn_check_on_holo_dark,0,0,0);
                else
                    chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle%2 == 1 ?
                    R.drawable.btn_check_off_holo_light:
                    R.drawable.btn_check_off_holo_dark,0,0,0);

            }
        });

        // set list string array
        mListStrArr = new ArrayList<>();
        mCheckedArr = new ArrayList<>();

        dB_drawer.open();
        count = dB_drawer.getFoldersCount(false);
        for(int i = 0; i< count; i++)
        {
            // list string array: init
            mListStrArr.add(dB_drawer.getFolderTitle(i,false));
            // checked mark array: init
            mCheckedArr.add(false);
        }
        dB_drawer.close();

        // set list adapter
        ListAdapter listAdapter = new ListAdapter(mAct, mListStrArr);

        // list view: set adapter
        mListView.setAdapter(listAdapter);
    }

    // list adapter
    public class ListAdapter extends BaseAdapter
    {
        private Activity activity;
        private List<String> mList;
        private LayoutInflater inflater = null;

        ListAdapter(Activity a, List<String> list)
        {
            activity = a;
            mList = list;
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount()
        {
            return mList.size();
        }

        public Object getItem(int position)
        {
            return mCheckedArr.get(position);
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            mView = inflater.inflate(R.layout.select_page_list_row, null);

            // set checked text view
            CheckedTextView chkTV = (CheckedTextView) mView.findViewById(R.id.checkTV);
            // show style
            int style = 1;
            chkTV.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
            chkTV.setTextColor(ColorSet.mText_ColorArray[style]);

            // Show current page
            //??? how to set left padding of text view of a CheckedTextview
            // workaround: set single line to true and add one space in front of the text
            if(position == Folder.getFocus_folderPos())
            {
                chkTV.setTypeface(chkTV.getTypeface(), Typeface.BOLD_ITALIC);
                chkTV.setText( " " + mList.get(position) + "*" );
            }
            else
                chkTV.setText( " " + mList.get(position).toString());

            chkTV.setChecked(mCheckedArr.get(position));

            // set for contrast
            if( chkTV.isChecked())
            // note: have to remove the following in XML file
            // android:drawableLeft="?android:attr/listChoiceIndicatorMultiple"
            // otherwise, setCompoundDrawablesWithIntrinsicBounds will not work on ICS
                chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                R.drawable.btn_check_on_holo_light:
                R.drawable.btn_check_on_holo_dark,0,0,0);
            else
                chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                R.drawable.btn_check_off_holo_light:
                R.drawable.btn_check_off_holo_dark,0,0,0);

            return mView;
        }
    }
}
