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

package com.cw.audio7.drawer;

import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.operation.delete.DeleteFolders;
import com.cw.audio7.util.Util;
import com.google.android.material.navigation.NavigationView;
import com.mobeta.android.dslv.DragSortListView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

/**
 * Created by CW on 2016/8/24.
 */
public class Drawer {


    public static DrawerLayout drawerLayout;
    private AppCompatActivity act;
    public ActionBarDrawerToggle drawerToggle;
    public static NavigationView mNavigationView;
    DragSortListView listView;

    public Drawer(AppCompatActivity activity)
    {
        drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);

        mNavigationView = (NavigationView) activity.findViewById(R.id.nav_view);
        mNavigationView.setItemIconTintList(null);// use original icon color

        // set icon for folder draggable: portrait
        if(Util.isPortraitOrientation(MainAct.mAct) && (MainAct.mPref_show_note_attribute != null)) {
            if (MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                    .equalsIgnoreCase("yes"))
                mNavigationView.getMenu().findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_on_holo_light);
            else
                mNavigationView.getMenu().findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setIcon(R.drawable.btn_check_off_holo_light);
        }

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    case R.id.ADD_NEW_FOLDER:
                        FolderUi.renewFirstAndLast_folderId();
                        FolderUi.addNewFolder(MainAct.mAct, FolderUi.mLastExist_folderTableId +1, MainAct.mFolder.getAdapter());
                        return true;

                    case R.id.ENABLE_FOLDER_DRAG_AND_DROP:
                        if(MainAct.mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                                .equalsIgnoreCase("yes"))
                        {
                            menuItem.setIcon(R.drawable.btn_check_off_holo_light);
                            MainAct.mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","no")
                                    .apply();
                            DragSortListView listView = (DragSortListView) act.findViewById(R.id.drawer_listview);
                            listView.setDragEnabled(false);
                            Toast.makeText(act,act.getResources().getString(R.string.drag_folder)+
                                            ": " +
                                            act.getResources().getString(R.string.set_disable),
                                    Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            menuItem.setIcon(R.drawable.btn_check_on_holo_light);
                            MainAct.mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","yes")
                                    .apply();
                            DragSortListView listView = (DragSortListView) act.findViewById(R.id.drawer_listview);
                            listView.setDragEnabled(true);
                            Toast.makeText(act,act.getResources().getString(R.string.drag_folder) +
                                            ": " +
                                           act.getResources().getString(R.string.set_enable),
                                    Toast.LENGTH_SHORT).show();
                        }
                        MainAct.mFolder.getAdapter().notifyDataSetChanged();
                        act.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                        return true;

                    case R.id.DELETE_FOLDERS:

                        DB_drawer dB_drawer = new DB_drawer(act);
                        if(dB_drawer.getFoldersCount(true)>0)
                        {
                            closeDrawer();
                            MainAct.mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                            DeleteFolders delFoldersFragment = new DeleteFolders();
                            MainAct.mFragmentTransaction = MainAct.mAct.getSupportFragmentManager().beginTransaction();
                            MainAct.mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                            MainAct.mFragmentTransaction.replace(R.id.content_frame, delFoldersFragment).addToBackStack("delete_folders").commit();
                        }
                        else
                        {
                            Toast.makeText(act, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
                        }
                        return true;

                    default:
                        return true;
                }
            }
        });



        act = activity;
        listView = (DragSortListView) act.findViewById(R.id.drawer_listview);
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle =new ActionBarDrawerToggle(act,                  /* host Activity */
                                                drawerLayout,         /* DrawerLayout object */
                                                MainAct.mToolbar,  /* tool bar */
                                                R.string.drawer_open,  /* "open drawer" description for accessibility */
                                                R.string.drawer_close  /* "close drawer" description for accessibility */
                                                )
            {
                public void onDrawerOpened(View drawerView)
                {
                    System.out.println("Drawer / _onDrawerOpened ");

                    if(act.getSupportActionBar() != null) {
                        act.getSupportActionBar().setTitle(R.string.app_name);
                        MainAct.mToolbar.setLogo(R.mipmap.ic_launcher);
                    }

                    act.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                    if(listView.getCount() >0) {
                        // will call Folder_adapter _getView to update audio playing high light
                        listView.invalidateViews();
                    }
                }// onDrawerOpened

                public void onDrawerClosed(View view)
                {
                    System.out.println("Drawer / _onDrawerClosed / FolderUi.getFocus_folderPos() = " + FolderUi.getFocus_folderPos());

                    FragmentManager fragmentManager = act.getSupportFragmentManager();
                    if(fragmentManager.getBackStackEntryCount() ==0 )
                    {
                        act.invalidateOptionsMenu(); // creates a call to onPrepareOptionsMenu()

                        DB_drawer dB_drawer = new DB_drawer(act);
                        if (dB_drawer.getFoldersCount(true) > 0)
                        {
                            int pos = listView.getCheckedItemPosition();
                            MainAct.mFolderTitle = dB_drawer.getFolderTitle(pos,true);

                            if(act.getSupportActionBar() != null) {
                                act.getSupportActionBar().setTitle(MainAct.mFolderTitle);
                                MainAct.mToolbar.setLogo(null);
                            }
                        }
                    } // onDrawerClosed
                }
           };
    }

    public void initDrawer()
    {
        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerLayout.addDrawerListener(drawerToggle);
    }

    public void closeDrawer()
    {
        drawerLayout.closeDrawer(mNavigationView);
    }


    public boolean isDrawerOpen()
    {
        return drawerLayout.isDrawerOpen(mNavigationView);
    }

    public static int getFolderCount() {
        DB_drawer dB_drawer = new DB_drawer(MainAct.mAct);
        return dB_drawer.getFoldersCount(true);
    }
}
