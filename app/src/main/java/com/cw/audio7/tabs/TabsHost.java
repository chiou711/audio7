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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.define.Define;
import com.cw.audio7.drawer.Drawer;
import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.operation.audio.AudioPlayer_page;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.page.Page_recycler;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;

//if(Define.ENABLE_ADMOB), enable the following
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class TabsHost extends AppCompatDialogFragment implements TabLayout.OnTabSelectedListener
{
    public static TabLayout mTabLayout;
    public static ViewPager mViewPager;
    public static TabsPagerAdapter mTabsPagerAdapter;
    public static int mFocusPageTableId;
    public static int mFocusTabPos;

    public static int lastPageTableId;
    public static int audioPlayTabPos;

    public static int firstPos_pageId;

    public static AudioUi_page audioUi_page;
    public static AudioPlayer_page audioPlayer_page;
    public static boolean isDoingMarking;
    private AdView adView;

    public TabsHost()
    {
//        System.out.println("TabsHost / construct");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        System.out.println("TabsHost / _onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        System.out.println("TabsHost / _onCreateView");

        View rootView;

        // set layout by orientation
        if (Util.isLandscapeOrientation(MainAct.mAct)) {
            if(Define.ENABLE_ADMOB) {
                if (Define.CODE_MODE == Define.DEBUG_MODE)
                    rootView = inflater.inflate(R.layout.tabs_host_landscape_test, container, false);
                else
                    rootView = inflater.inflate(R.layout.tabs_host_landscape, container, false);
            }
            else
                rootView = inflater.inflate(R.layout.tabs_host_landscape_no_admob, container, false);
        }
        else {
            if(Define.ENABLE_ADMOB) {
                if (Define.CODE_MODE == Define.DEBUG_MODE)
                    rootView = inflater.inflate(R.layout.tabs_host_portrait_test, container, false);
                else
                    rootView = inflater.inflate(R.layout.tabs_host_portrait, container, false);
            }
            else
                rootView = inflater.inflate(R.layout.tabs_host_portrait_no_admob, container, false);
        }

        // view pager
        mViewPager = (ViewPager) rootView.findViewById(R.id.tabs_pager);

        // mTabsPagerAdapter
        mTabsPagerAdapter = new TabsPagerAdapter(MainAct.mAct,MainAct.mAct.getSupportFragmentManager());
//        mTabsPagerAdapter = new TabsPagerAdapter(MainAct.mAct,getChildFragmentManager());

        // add pages to mTabsPagerAdapter
        int pageCount = 0;
        if(Drawer.getFolderCount() > 0) {
            pageCount = addPages(mTabsPagerAdapter);
        }

        // show blank folder if no page exists
        if(pageCount == 0) {
            rootView.findViewById(R.id.blankFolder).setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        }
        else {
            rootView.findViewById(R.id.blankFolder).setVisibility(View.GONE);
            mViewPager.setVisibility(View.VISIBLE);
        }

        // set mTabsPagerAdapter of view pager
        mViewPager.setAdapter(mTabsPagerAdapter);

        // set tab layout
        mTabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.addOnTabSelectedListener(this);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
//        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

//        mTabLayout.setBackgroundColor(ColorSet.getBarColor(MainAct.mAct));
        mTabLayout.setBackgroundColor(ColorSet.getButtonColor(MainAct.mAct));
//        mTabLayout.setBackgroundColor(Color.parseColor("#FF303030"));

        // tab indicator
        mTabLayout.setSelectedTabIndicatorHeight(15);
        mTabLayout.setSelectedTabIndicatorColor(Color.parseColor("#FFFF7F00"));
//        mTabLayout.setSelectedTabIndicatorHeight((int) (5 * getResources().getDisplayMetrics().density));

        mTabLayout.setTabTextColors(
                ContextCompat.getColor(MainAct.mAct,R.color.colorGray), //normal
                ContextCompat.getColor(MainAct.mAct,R.color.colorWhite) //selected
        );

        mFooterMessage = (TextView) rootView.findViewById(R.id.footerText);
        mFooterMessage.setBackgroundColor(Color.BLUE);
        mFooterMessage.setVisibility(View.VISIBLE);


        // AdMob support
        // if ENABLE_ADMOB = true, enable the following
        // test app id
        if(Define.ENABLE_ADMOB) {
            // old code
//                if (Define.CODE_MODE == Define.DEBUG_MODE)
//                    MobileAds.initialize(getActivity(), getActivity().getResources().getString(R.string.ad_mob_app_id_test));
//                else // real app id
//                    MobileAds.initialize(getActivity(), getActivity().getResources().getString(R.string.ad_mob_app_id));
//
//                // Load an ad into the AdMob banner view.
//                AdView adView = (AdView) rootView.findViewById(R.id.adView);
//                AdRequest adRequest = new AdRequest.Builder().build();
//                adView.loadAd(adRequest);

            // new code
            // Initialize the Mobile Ads SDK.
            MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {}
            });

            // get test ads on a physical device
//            String android_id = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
//            String deviceId = md5(android_id).toUpperCase();
//
//            Log.d("TabsHost  <deviceId>" , deviceId);
//
//            MobileAds.setRequestConfiguration(
//                    new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList(deviceId))
//                            .build());

            // Create an ad request.
            AdRequest adRequest = new AdRequest.Builder().build();

            adView = rootView.findViewById(R.id.adView);
            // Start loading the ad in the background.
            adView.loadAd(adRequest);
        }
        return rootView;
    }

    // get MD5
//    private String md5(String md5) {
//        try {
//            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
//            byte[] array = md.digest(md5.getBytes());
//            StringBuffer sb = new StringBuffer();
//            for (int i = 0; i < array.length; ++i) {
//                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
//            }
//            return sb.toString();
//        } catch (java.security.NoSuchAlgorithmException e) {
//        }
//        return null;
//    }

    /**
     * Add pages
     */
    private int addPages(TabsPagerAdapter adapter)
    {
        lastPageTableId = 0;
        int pageCount = adapter.dbFolder.getPagesCount(true);
//        System.out.println("TabsHost / _addPages / pagesCount = " + pageCount);

        if(pageCount > 0) {
            for (int i = 0; i < pageCount; i++) {
                int pageTableId = adapter.dbFolder.getPageTableId(i, true);

                if (i == 0)
                    setFirstPos_pageId(adapter.dbFolder.getPageId(i, true));

                if (pageTableId > lastPageTableId)
                    lastPageTableId = pageTableId;

                Page_recycler page = new Page_recycler();
                Bundle args = new Bundle();
                args.putInt("page_pos",i);
                args.putInt("page_table_id",pageTableId);
                page.setArguments(args);
//                System.out.println("TabsHost / _addPages / page_tableId = " + pageTableId);
                adapter.addFragment(page);
            }
        }

        return pageCount;
    }

    /**
     * Get last page table Id
     */
    public static int getLastPageTableId()
    {
        return lastPageTableId;
    }

    /**
     * Set last page table Id
     */
    public static void setLastPageTableId(int id)
    {
        lastPageTableId = id;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        System.out.println("TabsHost / _onTabSelected / tab position: " + tab.getPosition());

        // TODO
        //  note: tab position is kept after importing new XML, how to change it?
        setFocus_tabPos(tab.getPosition());

        // keep focus view page table Id
        int pageTableId = mTabsPagerAdapter.dbFolder.getPageTableId(getFocus_tabPos(), true);
        Pref.setPref_focusView_page_tableId(MainAct.mAct, pageTableId);

        // current page table Id
        setCurrentPageTableId(pageTableId);

        // refresh list view of selected page
        Page_recycler page = mTabsPagerAdapter.fragmentList.get(getFocus_tabPos());
        if( (tab.getPosition() == audioPlayTabPos) &&
            (page != null) &&
            (page.itemAdapter != null) )
        {
            RecyclerView listView = page.recyclerView;
            if( (audioPlayer_page != null) &&
                !isDoingMarking &&
                (listView != null) &&
                (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP)  )
            {
                audioPlayer_page.scrollHighlightAudioItemToVisible(listView);
            }
        }

        // add for update page item view
        if((page != null) && (page.itemAdapter != null))
        {
            page.itemAdapter.notifyDataSetChanged();
//            System.out.println("TabsHost / _onTabSelected / notifyDataSetChanged ");
        }
//        else
//            System.out.println("TabsHost / _onTabSelected / not notifyDataSetChanged ");

        // set tab audio icon when audio playing
        if ( (MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()) &&
             (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) &&
             (tab.getPosition() == audioPlayTabPos)                              )
        {
            if(tab.getCustomView() == null) {
                LinearLayout tabLinearLayout = (LinearLayout) MainAct.mAct.getLayoutInflater().inflate(R.layout.tab_custom, null);
                TextView title = (TextView) tabLinearLayout.findViewById(R.id.tabTitle);
                title.setText(mTabsPagerAdapter.dbFolder.getPageTitle(tab.getPosition(), true));
                title.setTextColor(MainAct.mAct.getResources().getColor(R.color.colorWhite));
                title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_audio, 0, 0, 0);
                tab.setCustomView(title);
            }
        }
        else
            tab.setCustomView(null);

        // call onCreateOptionsMenu
        MainAct.mAct.invalidateOptionsMenu();

        // set text color
        mTabLayout.setTabTextColors(
                ContextCompat.getColor(MainAct.mAct,R.color.colorGray), //normal
                ContextCompat.getColor(MainAct.mAct,R.color.colorWhite) //selected
        );

        // set long click listener
        setLongClickListener();

        TabsHost.showFooter(MainAct.mAct);

        isDoingMarking = false;
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    @Override
    public void onResume() {
        super.onResume();
        // default
        setFocus_tabPos(0);

        if(Drawer.getFolderCount() == 0)
            return;//todo Check again

        // restore focus view page
        int pageCount = mTabsPagerAdapter.dbFolder.getPagesCount(true);
//        System.out.println("TabsHost / _onResume / pageCount = " + pageCount);
        for(int pos=0; pos<pageCount; pos++)
        {
            int pageTableId = mTabsPagerAdapter.dbFolder.getPageTableId(pos, true);

            if(pageTableId == Pref.getPref_focusView_page_tableId(MainAct.mAct)) {
//                System.out.println("TabsHost / _onResume / set focus tab pos = " + pos);
                setFocus_tabPos(pos);
                setCurrentPageTableId(pageTableId);
            }
        }

//        System.out.println("TabsHost / _onResume / _getFocus_tabPos = " + getFocus_tabPos());

        // auto scroll to show focus tab
        new Handler().postDelayed(
                new Runnable() {
                    @Override public void run() {
                        if(mTabLayout.getTabAt(getFocus_tabPos()) != null)
                            mTabLayout.getTabAt(getFocus_tabPos()).select();
                    }
                }, 100);

        // set audio icon after Key Protect
        TabLayout.Tab tab =  mTabLayout.getTabAt(audioPlayTabPos);
        if(tab != null) {
            if( (MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()) &&
                (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP)  &&
                (tab.getPosition() == audioPlayTabPos)                               )
            {
                if(tab.getCustomView() == null)
                {
                    LinearLayout tabLinearLayout = (LinearLayout) MainAct.mAct.getLayoutInflater().inflate(R.layout.tab_custom, null);
                    TextView title = (TextView) tabLinearLayout.findViewById(R.id.tabTitle);
                    title.setText(mTabsPagerAdapter.dbFolder.getPageTitle(tab.getPosition(), true));
                    title.setTextColor(MainAct.mAct.getResources().getColor(R.color.colorWhite));
                    title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_audio, 0, 0, 0);
                    tab.setCustomView(title);
                }
            }
            else
                tab.setCustomView(null);
        }

        // for incoming phone call case or after Key Protect
        if( (audioUi_page != null) &&
            (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) &&
            (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)   )
        {
            audioUi_page.initAudioBlock(MainAct.mAct);

            audioPlayer_page.page_runnable.run();//todo Why exception when adding new text?

            //todo Why dose this panel disappear?
            UtilAudio.updateAudioPanel(audioUi_page.audioPanel_play_button,
                                       audioUi_page.audio_panel_title_textView);
        }

        // set long click listener
        setLongClickListener();

        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        System.out.println("TabsHost / _onPause");

        //  Remove fragments
        if(!MainAct.mAct.isDestroyed())
            removeTabs();//Put here will solve onBackStackChanged issue (no Page_recycler / _onCreate)

        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
    }

    // store scroll of recycler view
    public static void store_listView_vScroll(RecyclerView recyclerView)
    {
        int firstVisibleIndex = ((LinearLayoutManager) recyclerView.getLayoutManager())
                .findFirstVisibleItemPosition();

        View v = recyclerView.getChildAt(0);
        int firstVisibleIndexTop = (v == null) ? 0 : v.getTop();

        System.out.println("TabsHost / _store_listView_vScroll / firstVisibleIndex = " + firstVisibleIndex +
                " , firstVisibleIndexTop = " + firstVisibleIndexTop);

        // keep index and top position
        Pref.setPref_focusView_list_view_first_visible_index(MainAct.mAct, firstVisibleIndex);
        Pref.setPref_focusView_list_view_first_visible_index_top(MainAct.mAct, firstVisibleIndexTop);
    }

    // resume scroll of recycler view
    public static void resume_listView_vScroll(RecyclerView recyclerView)
    {
        // recover scroll Y
        int firstVisibleIndex = Pref.getPref_focusView_list_view_first_visible_index(MainAct.mAct);
        int firstVisibleIndexTop = Pref.getPref_focusView_list_view_first_visible_index_top(MainAct.mAct);

        System.out.println("TabsHost / _resume_listView_vScroll / firstVisibleIndex = " + firstVisibleIndex +
                " , firstVisibleIndexTop = " + firstVisibleIndexTop);

        // restore index and top position
        ((LinearLayoutManager)recyclerView.getLayoutManager()).scrollToPositionWithOffset(firstVisibleIndex, firstVisibleIndexTop);
    }


    /**
     * Get first position page Id
     * @return page Id of 1st position
     */
    public static int getFirstPos_pageId() {
        return firstPos_pageId;
    }

    /**
     * Set first position table Id
     * @param id: page Id
     */
    public static void setFirstPos_pageId(int id) {
        firstPos_pageId = id;
    }

    public static void reloadCurrentPage()
    {
//        System.out.println("TabsHost / _reloadCurrentPage");
        int pagePos = getFocus_tabPos();
        mViewPager.setAdapter(mTabsPagerAdapter);
        mViewPager.setCurrentItem(pagePos);
    }

    public static Page_recycler getCurrentPage()
    {
        return mTabsPagerAdapter.fragmentList.get(getFocus_tabPos());
    }


    public static void setCurrentPageTableId(int id)
    {
        //System.out.println("TabsHost / _setCurrentPageTableId / id = " + id);
        mFocusPageTableId = id;
    }


    public static int getCurrentPageTableId()
    {
        //System.out.println("TabsHost / _getCurrentPageTableId / mFocusPageTableId = " + mFocusPageTableId);
        return mFocusPageTableId;
    }


    /**
     * Set long click listeners for tabs editing
     */
    void setLongClickListener()
    {
//        System.out.println("TabsHost / _setLongClickListener");

        //https://stackoverflow.com/questions/33367245/add-onlongclicklistener-on-android-support-tablayout-tablayout-tab
        // on long click listener
        LinearLayout tabStrip = (LinearLayout) mTabLayout.getChildAt(0);
        final int tabsCount =  tabStrip.getChildCount();
        for (int i = 0; i < tabsCount; i++)
        {
            final int tabPos = i;
            tabStrip.getChildAt(tabPos).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    editPageTitle(tabPos,MainAct.mAct);
                    return false;
                }
            });
        }
    }

    /**
     * edit page title
     *
     */
    static void editPageTitle(final int tabPos, final AppCompatActivity act)
    {
        final DB_folder mDbFolder = mTabsPagerAdapter.dbFolder;

        // get tab name
        String title = mDbFolder.getPageTitle(tabPos, true);

        final EditText editText1 = new EditText(act.getBaseContext());
        editText1.setText(title);
        editText1.setSelection(title.length()); // set edit text start position
        editText1.setTextColor(Color.BLACK);

        //update tab info
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(R.string.edit_page_tab_title)
                .setMessage(R.string.edit_page_tab_message)
                .setView(editText1)
                .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
                                    {   @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {/*cancel*/}
                                    })
                .setNeutralButton(R.string.edit_page_button_delete, new DialogInterface.OnClickListener()
                    {   @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // delete
                            Util util = new Util(act);
                            util.vibrate();

                            AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
                            builder1.setTitle(R.string.confirm_dialog_title)
                                    .setMessage(R.string.confirm_dialog_message_page)
                                    .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog1, int which1){
                                            /*nothing to do*/}})
                                    .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog1, int which1){
                                            deletePage(tabPos, act);
                                            FolderUi.selectFolder(act,FolderUi.getFocus_folderPos());
                                        }})
                                    .show();
                        }
                    })
                .setPositiveButton(R.string.edit_page_button_update, new DialogInterface.OnClickListener()
                    {   @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // save
                            final int pageId =  mDbFolder.getPageId(tabPos, true);
                            final int pageTableId =  mDbFolder.getPageTableId(tabPos, true);

                            int tabStyle = mDbFolder.getPageStyle(tabPos, true);
                            mDbFolder.updatePage(pageId,
                                                 editText1.getText().toString(),
                                                 pageTableId,
                                                 tabStyle,
                                                 true);

                            FolderUi.startTabsHostRun();
                        }
                    })
                .setIcon(android.R.drawable.ic_menu_edit);

        AlertDialog d1 = builder.create();
        d1.show();
        // android.R.id.button1 for positive: save
        ((Button)d1.findViewById(android.R.id.button1))
                .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);

        // android.R.id.button2 for negative: color
        ((Button)d1.findViewById(android.R.id.button2))
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        // android.R.id.button3 for neutral: delete
        ((Button)d1.findViewById(android.R.id.button3))
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);

    }

    /**
     * delete page
     *
     */
    public static  void deletePage(int tabPos, final AppCompatActivity activity)
    {

        final DB_folder mDbFolder = mTabsPagerAdapter.dbFolder;
        int pageId =  mDbFolder.getPageId(tabPos, true);
        mDbFolder.open();
        // check if only one page left
        int pagesCount = mDbFolder.getPagesCount(false);
        int mFirstPos_PageId = 1;
        Cursor mPageCursor = mDbFolder.getPageCursor();
        if(mPageCursor.isFirst())
            mFirstPos_PageId = pageId;

        if(pagesCount > 0)
        {
            //if current page is the first page and will be delete,
            //try to get next existence of note page
//            System.out.println("TabsHost / deletePage / tabPos = " + tabPos);
//            System.out.println("TabsHost / deletePage / mFirstPos_PageId = " + mFirstPos_PageId);
            if(pageId == mFirstPos_PageId)
            {
                int cGetNextExistIndex = getFocus_tabPos() +1;
                boolean bGotNext = false;
                while(!bGotNext){
                    try{
                        mFirstPos_PageId =  mDbFolder.getPageId(cGetNextExistIndex, false);
                        bGotNext = true;
                    }catch(Exception e){
                        bGotNext = false;
                        cGetNextExistIndex++;}}
            }

            //change to first existing page
            int newFirstPageTblId = 0;
            for(int i=0 ; i<pagesCount; i++)
            {
                if(	mDbFolder.getPageId(i, false)== mFirstPos_PageId)
                {
                    newFirstPageTblId =  mDbFolder.getPageTableId(i, false);
//                    System.out.println("TabsHost / deletePage / newFirstPageTblId = " + newFirstPageTblId);
                }
            }
//            System.out.println("TabsHost / deletePage / --- after delete / newFirstPageTblId = " + newFirstPageTblId);
            Pref.setPref_focusView_page_tableId(activity, newFirstPageTblId);//todo Could be 0?
        }
//		else
//		{
//             Toast.makeText(activity, R.string.toast_keep_one_page , Toast.LENGTH_SHORT).show();
//             return;
//		}
        mDbFolder.close();

        // get page table Id for dropping
        int pageTableId = mDbFolder.getPageTableId(tabPos, true);
//        System.out.println("TabsHost / _deletePage / pageTableId =  " + pageTableId);

        // delete tab name
        mDbFolder.dropPageTable(pageTableId,true);
        mDbFolder.deletePage(DB_folder.getFocusFolder_tableName(),pageId,true);
//        mPagesCount--;

        // After Delete page, update highlight tab
        if(getFocus_tabPos() < MainAct.mPlaying_pagePos)
        {
            MainAct.mPlaying_pagePos--;
        }
        else if((getFocus_tabPos() == MainAct.mPlaying_pagePos) &&
                (MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()))
        {
            if(BackgroundAudioService.mMediaPlayer != null)
            {
                Audio_manager.stopAudioPlayer();
                Audio_manager.mAudioPos = 0;
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_STOP);
            }
        }

        // update change after deleting tab
        FolderUi.startTabsHostRun();
    }

    public static TextView mFooterMessage;

    // set footer
    public static void showFooter(AppCompatActivity mAct)
    {
//		System.out.println("TabsHost / _showFooter ");
        if(mFooterMessage != null)
        {
            mFooterMessage.setTextColor(ColorSet.color_white);
            mFooterMessage.setText(getFooterMessage(mAct));
            mFooterMessage.setBackgroundColor(ColorSet.getBarColor(mAct));
        }
    }

    // get footer message of list view
    static String getFooterMessage(AppCompatActivity mAct)
    {
        int pageTableId = Pref.getPref_focusView_page_tableId(mAct);
        DB_page mDb_page = new DB_page(mAct, pageTableId);
        return mAct.getResources().getText(R.string.footer_checked).toString() +
                "/" +
                mAct.getResources().getText(R.string.footer_total).toString() +
                ": " +
                mDb_page.getCheckedNotesCount() +
                "/" +
                mDb_page.getNotesCount(true);
    }

    /**
     * Get focus tab position
    */
    public static int getFocus_tabPos()
    {
        return mFocusTabPos;
    }

    /**
     * Set focus tab position
     * @param pos
     */
    public static void setFocus_tabPos(int pos)
    {
        mFocusTabPos = pos;
    }


    public static void removeTabs()
    {
//        System.out.println("TabsHost / _removeTabs");
    	if(TabsHost.mTabsPagerAdapter == null)
    		return;

        ArrayList<Page_recycler> fragmentList = TabsHost.mTabsPagerAdapter.fragmentList;
        if( (fragmentList != null) &&
            (fragmentList.size() >0)  )
        {
            RecyclerView listView = fragmentList.get(TabsHost.getFocus_tabPos()).recyclerView;//drag_listView;

            if(listView != null)
                TabsHost.store_listView_vScroll(listView);

            for (int i = 0; i < fragmentList.size(); i++) {
//                System.out.println("TabsHost / _removeTabs / i = " + i);
                MainAct.mAct.getSupportFragmentManager().beginTransaction().remove(fragmentList.get(i)).commit();
            }
        }
    }

}