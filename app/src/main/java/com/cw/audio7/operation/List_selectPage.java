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
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.preferences.Pref;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cw on 2017/9/21.
 */

public class List_selectPage
{
    View mView;
    CheckedTextView mCheckTvSelAll;
    ListView mListView;
    public List<String> mListStrArr; // list view string array
    public List<Boolean> mCheckedTabs; // checked list view items array
    DB_drawer dB_drawer;
    DB_folder mDb_folder;
    public int count;
    Activity mAct;
    public String mFolderTitle;
    public boolean isCheckAll;

    public List_selectPage(Activity act, View rootView, View view)
    {
        mAct = act;

        dB_drawer = new DB_drawer(act);
        DragSortListView listView = (DragSortListView) act.findViewById(R.id.drawer_listview);
        int pos = listView.getCheckedItemPosition();
        mFolderTitle = dB_drawer.getFolderTitle(pos,true);

        mDb_folder = new DB_folder(mAct, Pref.getPref_focusView_folder_tableId(mAct));

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new View.OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if( ((CheckedTextView)checkSelAll).isChecked() )
                {
                    isCheckAll = true;
                    selectAllPages(true);
                }
                else
                {
                    isCheckAll = false;
                    selectAllPages(false);
                }
            }
        });

        // list view: selecting which pages to send
        mListView = (ListView)view;
        showPageList(rootView);

        isCheckAll = false;
    }

    // select all pages
    public void selectAllPages(boolean enAll)
    {
        mChkNum = 0;

        mDb_folder.open();
        count = mDb_folder.getPagesCount(false);
        for(int i = 0; i< count; i++)
        {
            CheckedTextView chkTV = (CheckedTextView) mListView.findViewById(R.id.checkTV);
            mCheckedTabs.set(i, enAll);
            mListStrArr.set(i, mDb_folder.getPageTitle(i,false));

            int style = mDb_folder.getPageStyle(i, false);

            if( enAll)
                chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                R.drawable.btn_check_on_holo_light:
                R.drawable.btn_check_on_holo_dark,0,0,0);
            else
                chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                R.drawable.btn_check_off_holo_light:
                R.drawable.btn_check_off_holo_dark,0,0,0);
        }
        mDb_folder.close();

        mChkNum = (enAll == true)? count : 0;

        // set list adapter
        ListAdapter listAdapter = new ListAdapter(mAct, mListStrArr);

        // list view: set adapter
        mListView.setAdapter(listAdapter);
    }

    // show list for Select
    public int mChkNum;
    void showPageList(View root)
    {
        mChkNum = 0;
        // set list view
        mListView = (ListView) root.findViewById(R.id.listView1);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View vw, int position, long id)
            {
                System.out.println("List_selectPage / _showPageList / _onItemClick / position = " + position);
                CheckedTextView chkTV = (CheckedTextView) vw.findViewById(R.id.checkTV);
                chkTV.setChecked(!chkTV.isChecked());
                mCheckedTabs.set(position, chkTV.isChecked());
                if(mCheckedTabs.get(position) == true)
                    mChkNum++;
                else
                    mChkNum--;

                if(!chkTV.isChecked())
                {
                    mCheckTvSelAll.setChecked(false);
                }

                // set for contrast
                int mStyle = mDb_folder.getPageStyle(position, true);
                if( chkTV.isChecked()) {
                    chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle % 2 == 1 ?
                            R.drawable.btn_check_on_holo_light :
                            R.drawable.btn_check_on_holo_dark, 0, 0, 0);
                }
                else {
                    chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle % 2 == 1 ?
                            R.drawable.btn_check_off_holo_light :
                            R.drawable.btn_check_off_holo_dark, 0, 0, 0);
                    isCheckAll = false;
                }
            }
        });

        // set list string array
        mCheckedTabs = new ArrayList<Boolean>();
        mListStrArr = new ArrayList<String>();

        // DB
        int pageTableId = Pref.getPref_focusView_page_tableId(mAct);
        DB_page.setFocusPage_tableId(pageTableId);

        mDb_folder.open();
        count = mDb_folder.getPagesCount(false);
        for(int i = 0; i< count; i++)
        {
            // list string array: init
            mListStrArr.add(mDb_folder.getPageTitle(i,false));
            // checked mark array: init
            mCheckedTabs.add(false);
        }
        mDb_folder.close();

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

        public ListAdapter(Activity a, List<String> list)
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
            return mCheckedTabs.get(position);
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
            int style = mDb_folder.getPageStyle(position, true);
            chkTV.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
            chkTV.setTextColor(ColorSet.mText_ColorArray[style]);

            // Show current page
            // workaround: set single line to true and add one space in front of the text
            if(mDb_folder.getPageTableId(position,true) == Integer.valueOf(DB_page.getFocusPage_tableId()))
            {
                chkTV.setTypeface(chkTV.getTypeface(), Typeface.BOLD_ITALIC);
                chkTV.setText( " " + mList.get(position) + "*" );
            }
            else
                chkTV.setText( " " + mList.get(position).toString());

            chkTV.setChecked(mCheckedTabs.get(position));

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
