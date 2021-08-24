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

package com.cw.audio7.tabs;

import android.view.ViewGroup;

import com.cw.audio7.db.DB_folder;
import com.cw.audio7.page.Page;
import com.cw.audio7.util.preferences.Pref;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * Created by cw on 2018/3/20.
 *
 *  View Pager Adapter Class
 *
 */
public class TabsPagerAdapter extends FragmentPagerAdapter {
    public ArrayList<Page> fragmentList = new ArrayList<>();
    DB_folder dbFolder;

    TabsPagerAdapter(AppCompatActivity act, FragmentManager fm)
    {
        super(fm,FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        int folderTableId = Pref.getPref_focusView_folder_tableId(act);
        dbFolder = new DB_folder(folderTableId);
    }

    @Override
    public Page getItem(int position)
    {
        return fragmentList.get(position);
    }

    // add fragment
    public void addFragment(Page fragment) {
        fragmentList.add(fragment);
    }

    @Override
    public int getCount(){
        return fragmentList.size();
    }

    @Override
    public CharSequence getPageTitle(int position){
//        System.out.println("TabsPagerAdapter / _getPageTitle / position = " + position);
        return dbFolder.getPageTitle(position,true);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
//        System.out.println("TabsPagerAdapter / _setPrimaryItem / position = " + position);
    }

}

