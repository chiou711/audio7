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

import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.operation.delete.DeleteFolders;
import com.cw.audio7.util.Util;
import com.google.android.material.navigation.NavigationView;
import com.mobeta.android.dslv.DragSortListView;
import  com.cw.audio7.main.MenuId;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

/**
 * Created by CW on 2016/8/24.
 */
public class Drawer {


    public DrawerLayout drawerLayout;
    private final AppCompatActivity act;
    public ActionBarDrawerToggle drawerToggle;
    public NavigationView mNavigationView;
    DragSortListView listView;
    public SharedPreferences mPref_show_note_attribute;
    public Folder folder;
    public static int foldersCount;

    public Drawer(AppCompatActivity _act, Toolbar toolbar) {
        this.act = _act;

        // new folder
        folder = new Folder(this.act);

        drawerLayout = (DrawerLayout) act.findViewById(R.id.drawer_layout);

        mNavigationView = (NavigationView) act.findViewById(R.id.nav_view);
        mNavigationView.setItemIconTintList(null);// use original icon color

        // set icon for folder draggable: portrait
        if (Util.isPortraitOrientation(act) && (mPref_show_note_attribute != null)) {
            if (mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
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
                    case MenuId.ADD_NEW_FOLDER:
                        folder.renewFirstAndLast_folderId();
                        folder.addNewFolder(act, folder.mLastExist_folderTableId + 1, folder.getAdapter());
                        return true;

                    case MenuId.ENABLE_FOLDER_DRAG_AND_DROP:
                        if (mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                                .equalsIgnoreCase("yes")) {
                            menuItem.setIcon(R.drawable.btn_check_off_holo_light);
                            mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                                    .apply();
                            DragSortListView listView = (DragSortListView) Drawer.this.act.findViewById(R.id.drawer_listview);
                            listView.setDragEnabled(false);
                            Toast.makeText(Drawer.this.act, Drawer.this.act.getResources().getString(R.string.drag_folder) +
                                            ": " +
                                            Drawer.this.act.getResources().getString(R.string.set_disable),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            menuItem.setIcon(R.drawable.btn_check_on_holo_light);
                            mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE", "yes")
                                    .apply();
                            DragSortListView listView = (DragSortListView) Drawer.this.act.findViewById(R.id.drawer_listview);
                            listView.setDragEnabled(true);
                            Toast.makeText(Drawer.this.act, Drawer.this.act.getResources().getString(R.string.drag_folder) +
                                            ": " +
                                            Drawer.this.act.getResources().getString(R.string.set_enable),
                                    Toast.LENGTH_SHORT).show();
                        }
                        folder.getAdapter().notifyDataSetChanged();
                        Drawer.this.act.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                        return true;

                    case MenuId.DELETE_FOLDERS:

                        DB_drawer dB_drawer = new DB_drawer(act);
                        if (dB_drawer.getFoldersCount(true) > 0) {
                            closeDrawer();

//                            mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                            DeleteFolders delFoldersFragment = new DeleteFolders(act);
                            FragmentTransaction fragmentTransaction = act.getSupportFragmentManager().beginTransaction();
                            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                            fragmentTransaction.replace(R.id.content_frame, delFoldersFragment).addToBackStack("delete_folders").commit();
                        } else {
                            Toast.makeText(Drawer.this.act, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
                        }
                        return true;

                    default:
                        return true;
                }
            }
        });

        listView = (DragSortListView) this.act.findViewById(R.id.drawer_listview);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle = new ActionBarDrawerToggle(act,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                toolbar,  /* tool bar */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerOpened(View drawerView) {
                System.out.println("Drawer / _onDrawerOpened ");

                if (act.getSupportActionBar() != null) {
                    act.getSupportActionBar().setTitle(R.string.app_name);
                    toolbar.setLogo(R.mipmap.ic_launcher); //todo Smaller icon?
                }

                act.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                if (listView.getCount() > 0) {
                    // will call Folder_adapter _getView to update audio playing high light
                    listView.invalidateViews();
                }
            }// onDrawerOpened

            public void onDrawerClosed(View view) {
                System.out.println("Drawer / _onDrawerClosed / Folder.getFocus_folderPos() = " + Folder.getFocus_folderPos());

                FragmentManager fragmentManager = act.getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() == 0) {
                    act.invalidateOptionsMenu(); // creates a call to onPrepareOptionsMenu()

                    DB_drawer dB_drawer = new DB_drawer(act);
                    if (dB_drawer.getFoldersCount(true) > 0) {
                        int pos = listView.getCheckedItemPosition();
                        String mFolderTitle = dB_drawer.getFolderTitle(pos, true);

                        if (act.getSupportActionBar() != null) {
                            act.getSupportActionBar().setTitle(mFolderTitle);
                            toolbar.setLogo(null);
                        }
                    }
                } // onDrawerClosed
            }
        };
    }

    public void initDrawer() {
        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerLayout.addDrawerListener(drawerToggle);

        setFoldersCount(getFoldersCount(act));
    }

    public static int getFoldersCount(AppCompatActivity act) {
        DB_drawer dB_drawer = new DB_drawer(act);
        return dB_drawer.getFoldersCount(true);
    }

    public static void setFoldersCount(int foldersCount) {
        Drawer.foldersCount = foldersCount;
    }

    public void closeDrawer() {
        drawerLayout.closeDrawer(mNavigationView);
    }

    public boolean isDrawerOpen() {
        return drawerLayout.isDrawerOpen(mNavigationView);
    }

}


