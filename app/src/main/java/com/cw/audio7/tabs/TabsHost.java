/*
 * Copyright (C) 2021 CW Chiu
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
import android.graphics.drawable.GradientDrawable;
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
import com.cw.audio7.audio.Audio7Player;
import com.cw.audio7.audio.Audio_manager;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;

import com.cw.audio7.drawer.Drawer;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.audio.AudioUi_page;
import com.cw.audio7.page.Page;
import com.cw.audio7.page.PageUi;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

//if(Define.ENABLE_ADMOB), enable the following
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.initialization.InitializationStatus;
//import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import com.cw.audio7.util.system.SystemState;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import static com.cw.audio7.audio.BackgroundAudioService.mAudio_manager;
import static com.cw.audio7.audio.BackgroundAudioService.mMediaPlayer;

public class TabsHost extends AppCompatDialogFragment implements TabLayout.OnTabSelectedListener
{
    public TabLayout mTabLayout;
    public ViewPager mViewPager;
    public TabsPagerAdapter mTabsPagerAdapter;
    public static int mFocusPageTableId;
    public static int mFocusTabPos;

    public static int lastPageTableId;
    public int firstPos_pageId;

    public AudioUi_page audioUi_page;
    public static boolean isDoingMarking;
    AppCompatActivity act;
    Folder folder;

    //if(Define.ENABLE_ADMOB), enable the following
    //private AdView adView;

    public TabsHost()
    {
//        System.out.println("TabsHost / construct");
    }

    public TabsHost(AppCompatActivity _act, Folder _folder) {
        act =_act;
        folder = _folder;
    }

    int pagesCount;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int folderPos = 0;
        
        if(folder != null) {
            folderPos = Folder.getFocus_folderPos();
            pagesCount = folder.getFolder_pagesCount(act, folderPos);
            System.out.println("TabsHost / _onCreate / pagesCount = " + pagesCount);
        }
    }

    public View rootView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        System.out.println("TabsHost / _onCreateView");

        // set layout by orientation
        if ((act!= null) && Util.isLandscapeOrientation(act)) {
//            if(Define.ENABLE_ADMOB) {
//                if (Define.CODE_MODE == Define.DEBUG_MODE)
//                    rootView = inflater.inflate(R.layout.tabs_host_landscape_test, container, false);
//                else
//                    rootView = inflater.inflate(R.layout.tabs_host_landscape, container, false);
//            }
//            else
                rootView = inflater.inflate(R.layout.tabs_host_landscape_no_admob, container, false);
        }
        else {
//            if(Define.ENABLE_ADMOB) {
//                if (Define.CODE_MODE == Define.DEBUG_MODE)
//                    rootView = inflater.inflate(R.layout.tabs_host_portrait_test, container, false);
//                else
//                    rootView = inflater.inflate(R.layout.tabs_host_portrait, container, false);
//            }
//            else
                rootView = inflater.inflate(R.layout.tabs_host_portrait_no_admob, container, false);
        }

        // view pager
        mViewPager = (ViewPager) rootView.findViewById(R.id.tabs_pager);

        int pageCount = 0;

        if(act!= null)
            mTabsPagerAdapter = new TabsPagerAdapter(act, act.getSupportFragmentManager());

        // add pages to mTabsPagerAdapter
        if ( (act != null) && Drawer.getFoldersCount(act) > 0) {
            pageCount = addPages(mTabsPagerAdapter);
        }

        // show blank folder if no page exists
        if(pageCount == 0) {
            rootView.findViewById(R.id.blankFolder).setVisibility(View.VISIBLE);
            ((TextView)rootView.findViewById(R.id.blankFolder)).setTextColor(Color.WHITE);
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

        if(act != null) {
            mTabLayout.setBackgroundColor(ColorSet.getBarColor(act));

            // set text color
            mTabLayout.setTabTextColors(
                    ContextCompat.getColor(act, R.color.colorGray), //normal
                    ContextCompat.getColor(act, R.color.colorWhite) //selected
            );

            // tab indicator height
//        mTabLayout.setSelectedTabIndicatorHeight(25); //todo ??? Replaced with?
            mTabLayout.setSelectedTabIndicatorHeight((int) (6 * getResources().getDisplayMetrics().density));

            // indicator color
            mTabLayout.setSelectedTabIndicatorColor(ColorSet.getTabIndicatorColor(act));
        }


        // indicator corner
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(10);
        mTabLayout.setSelectedTabIndicator(gradientDrawable);

        // indicator ripple
        mTabLayout.setUnboundedRipple(true);

        // indicator width
        mTabLayout.setTabIndicatorFullWidth(false);

        mFooterMessage = (TextView) rootView.findViewById(R.id.footerText);
        mFooterMessage.setBackgroundColor(Color.BLUE);
        mFooterMessage.setVisibility(View.VISIBLE);


        // AdMob support
        // if ENABLE_ADMOB = true, enable the following
        // test app id
//        if(Define.ENABLE_ADMOB) {
//            // Initialize the Mobile Ads SDK.
//            MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
//                @Override
//                public void onInitializationComplete(InitializationStatus initializationStatus) {}
//            });
//
//            // Create an ad request.
//            AdRequest adRequest = new AdRequest.Builder().build();
//
//            adView = rootView.findViewById(R.id.adView);
//            // Start loading the ad in the background.
//            adView.loadAd(adRequest);
//        }
        return rootView;
    }

    /**
     * Add pages
     */
    PageUi pageUi;
    public PageUi mPageUi;
    Bundle args;
    public View audio_panel;
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

                audio_panel = rootView.findViewById(R.id.audio_panel);
                pageUi = new PageUi(act, folder,audio_panel);

                int currPageTableId = Pref.getPref_focusView_page_tableId(act);
                setCurrentPageTableId(currPageTableId);
//                System.out.println("TabsHost / _addPages / currPageTableId = " + currPageTableId);

                if(pageTableId == currPageTableId)
                    mPageUi = new PageUi(act, folder,audio_panel);

                args = new Bundle();
                args.putInt("page_pos",i);
                args.putInt("page_table_id",pageTableId);
                pageUi.setArguments(args);
//                System.out.println("TabsHost / _addPages / page_tableId = " + pageTableId);
                adapter.addFragment(pageUi);
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
        doOnTabReselected(tab);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        doOnTabReselected(tab);
    }

    public void doOnTabReselected(TabLayout.Tab tab) {
        System.out.println("TabsHost / _doOnTabReselected / tab position: " + tab.getPosition());
        // TODO
        //  note: tab position is kept after importing new XML, how to change it?
        setFocus_tabPos(tab.getPosition());

        // keep focus view page table Id
        int pageTableId = mTabsPagerAdapter.dbFolder.getPageTableId(getFocus_tabPos(), true);
        Pref.setPref_focusView_page_tableId(act, pageTableId);

        // current page table Id
        setCurrentPageTableId(pageTableId);

        // refresh list view of selected page
        Page page = mTabsPagerAdapter.fragmentList.get(getFocus_tabPos());

        // add for update page item view
        if((page != null) && (page.itemAdapter != null)) {
            page.itemAdapter.updateDbCache();
            page.itemAdapter.notifyDataSetChanged();
        }

        // set tab audio icon when audio playing
        showPlayingTab();

        // call onCreateOptionsMenu
        act.invalidateOptionsMenu();

        // set long click listener
        setLongClickListener();

        showFooter(act);

        isDoingMarking = false;
    }

//    public Audio7Player audio7Player;
    @Override
    public void onResume() {
        super.onResume();

        System.out.println("TabsHost / _onResume");

        // default
        setFocus_tabPos(0);

        if(Drawer.getFoldersCount(act) == 0)
            return;

        int pageCount = 0;
        // restore focus view page
        if(mTabsPagerAdapter != null)
            pageCount = mTabsPagerAdapter.dbFolder.getPagesCount(true);

//        System.out.println("TabsHost / _onResume / pageCount = " + pageCount);
        for(int pos=0; pos<pageCount; pos++)
        {
            int pageTableId = mTabsPagerAdapter.dbFolder.getPageTableId(pos, true);

            if(pageTableId == Pref.getPref_focusView_page_tableId(act)) {
                System.out.println("TabsHost / _onResume / set focus tab pos = " + pos);
                setFocus_tabPos(pos);
            }
        }

        System.out.println("TabsHost / _onResume / _getFocus_tabPos = " + getFocus_tabPos());

        // auto scroll to show focus tab
        new Handler().postDelayed(
                new Runnable() {
                    @Override public void run() {
                        if(mTabLayout.getTabAt(getFocus_tabPos()) != null) {
                                mTabLayout.getTabAt(getFocus_tabPos()).select();
                        }
                    }
                }, 100);

        /** The following is used for
         * - incoming phone call case
         * - after Key Protect (screen off/on)
         * */
        if( (mAudio_manager !=null) &&
            (mAudio_manager.getAudioPlayMode() == mAudio_manager.PAGE_PLAY_MODE) &&
            (mAudio_manager.getPlayerState() != mAudio_manager.PLAYER_AT_STOP)               ) {
            if(mAudio_manager.audio7Player!=null)
                mAudio_manager.audio7Player.updateAudioPanel(act);
        }

//        if(Define.ENABLE_ADMOB) {
//            if (adView != null) {
//                adView.resume();
//            }
//        }

        // init audio parameters
        if(mAudio_manager == null)
            mAudio_manager = new Audio_manager(act, folder);

        // change audio panel when Note audio is changed to Page audio
        /** Entry: Note play -> Page play */
        /** Entry: background play -> Page play */
        if ( mMediaPlayer != null &&
             MainAct.mPlaying_folderPos == Folder.getFocus_folderPos())
        {
            mAudio_manager.kill_runnable = true;

            if(mAudio_manager.audio7Player == null)
                mAudio_manager.audio7Player = new Audio7Player(act, this,audio_panel, mAudio_manager.mAudioUri);
            else {
                mAudio_manager.audio7Player.setAudioPanel(audio_panel);
                mAudio_manager.audio7Player.initAudioBlock(mAudio_manager.mAudioUri);
            }

            audioUi_page = new AudioUi_page(act, this, mAudio_manager.audio7Player,  audio_panel, mAudio_manager.mAudioUri);

            if(audio_panel != null)
                audio_panel.setVisibility(View.VISIBLE);

            mAudio_manager.audio7Player.updateAudioPanel(act);
            mAudio_manager.audio7Player.updateAudioProgress();

            mAudio_manager.audioHandler.postDelayed(mAudio_manager.audio_runnable,Util.oneSecond*2);

            showPlayingTab();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("TabsHost / _onPause");

        //  Remove fragments
        if( (act!=null) && !act.isDestroyed())
            removePages();//Put here will solve onBackStackChanged issue (no Page_recycler / _onCreate)

//        if(Define.ENABLE_ADMOB) {
//            if (adView != null) {
//                adView.pause();
//            }
//        }

//        mTabsPagerAdapter = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        System.out.println("TabsHost / _onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("TabsHost / _onDestroy");
//        if(Define.ENABLE_ADMOB) {
//            if (adView != null) {
//                adView.destroy();
//            }
//        }
    }

    // show audio playing tab
   public void showPlayingTab() {
//       System.out.println("TabsHost / _showPlayingTab" );
        // set audio icon after Key Protect

       // return if screen off
       if(SystemState.isScreenOff(act)) {
           System.out.println("TabsHost / _showPlayingTab / return");
           return;
       }

       TabLayout.Tab tab =  mTabLayout.getTabAt(MainAct.mPlaying_pagePos);

        if(tab != null) {
            if( (MainAct.mPlaying_folderPos == Folder.getFocus_folderPos()) &&
                (mAudio_manager !=null) &&
                (mAudio_manager.getPlayerState() != mAudio_manager.PLAYER_AT_STOP)  &&
                (tab.getPosition() == MainAct.mPlaying_pagePos)                               )
            {
                if(tab.getCustomView() == null)
                {
                    LinearLayout tabLinearLayout = (LinearLayout) act.getLayoutInflater().inflate(R.layout.tab_custom, null);
                    TextView title = (TextView) tabLinearLayout.findViewById(R.id.tabTitle);
                    title.setText(mTabsPagerAdapter.dbFolder.getPageTitle(tab.getPosition(), true));
                    title.setTextColor(act.getResources().getColor(R.color.colorWhite));
                    title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_audio, 0, 0, 0);
                    tab.setCustomView(title);
                }
            }
            else
                tab.setCustomView(null);
        }
    }

    // store scroll of recycler view
    public void store_listView_vScroll(RecyclerView recyclerView)
    {
        int firstVisibleIndex = ((LinearLayoutManager) recyclerView.getLayoutManager())
                .findFirstVisibleItemPosition();

        View v = recyclerView.getChildAt(0);
        int firstVisibleIndexTop = (v == null) ? 0 : v.getTop();

//        System.out.println("TabsHost / _store_listView_vScroll / firstVisibleIndex = " + firstVisibleIndex +
//                " , firstVisibleIndexTop = " + firstVisibleIndexTop);

        // keep index and top position
        Pref.setPref_focusView_list_view_first_visible_index(act, firstVisibleIndex);
        Pref.setPref_focusView_list_view_first_visible_index_top(act, firstVisibleIndexTop);
    }

    // resume scroll of recycler view
    public void resume_listView_vScroll(RecyclerView recyclerView)
    {
        // recover scroll Y
        int firstVisibleIndex = Pref.getPref_focusView_list_view_first_visible_index(act);
        int firstVisibleIndexTop = Pref.getPref_focusView_list_view_first_visible_index_top(act);

//        System.out.println("TabsHost / _resume_listView_vScroll / firstVisibleIndex = " + firstVisibleIndex +
//                " , firstVisibleIndexTop = " + firstVisibleIndexTop);

        // restore index and top position
        ((LinearLayoutManager)recyclerView.getLayoutManager()).scrollToPositionWithOffset(firstVisibleIndex, firstVisibleIndexTop);
    }


    /**
     * Get first position page Id
     * @return page Id of 1st position
     */
    public int getFirstPos_pageId() {
        return firstPos_pageId;
    }

    /**
     * Set first position table Id
     * @param id: page Id
     */
    public void setFirstPos_pageId(int id) {
        firstPos_pageId = id;
    }

    public  void reloadCurrentPage()
    {
//        System.out.println("TabsHost / _reloadCurrentPage");
        int pagePos = getFocus_tabPos();
        mViewPager.setAdapter(mTabsPagerAdapter);
        mViewPager.setCurrentItem(pagePos);
    }

    public Page getCurrentPage()
    {
        if(mTabsPagerAdapter.fragmentList == null)
            System.out.println("------------- mTabsPagerAdapter.fragmentList == null)");
        return mTabsPagerAdapter.fragmentList.get(getFocus_tabPos());
    }


    public void setCurrentPageTableId(int id)
    {
//        System.out.println("TabsHost / _setCurrentPageTableId / id = " + id);
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
                    editPageTitle(tabPos,act);
                    return false;
                }
            });
        }
    }

    /**
     * edit page title
     *
     */
    void editPageTitle(final int tabPos, final AppCompatActivity act)
    {
        final DB_folder mDbFolder = new DB_folder(Folder.getFocus_folderTableId());

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
                                            folder.selectFolder(act, Folder.getFocus_folderPos());
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

                            folder.startTabsHostRun();
                        }
                    })
                .setIcon(android.R.drawable.ic_menu_edit);

        AlertDialog d1 = builder.create();
        d1.show();
        // android.R.id.button1 for positive: save
        ((Button)d1.findViewById(android.R.id.button1))
                .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);

        // android.R.id.button2 for negative: color
        // note: marked for space issue
//        ((Button)d1.findViewById(android.R.id.button2))
//                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        // android.R.id.button3 for neutral: delete
        ((Button)d1.findViewById(android.R.id.button3))
                .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);

    }

    /**
     * delete page
     *
     */
    public void deletePage(int tabPos, final AppCompatActivity activity)
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
                (MainAct.mPlaying_folderPos == Folder.getFocus_folderPos()))
        {
            if(mMediaPlayer != null)
            {
                mAudio_manager.stopAudioPlayer();
                mAudio_manager.mAudioPos = 0;
                mAudio_manager.setPlayerState(mAudio_manager.PLAYER_AT_STOP);
            }
        }

        // update change after deleting tab
        folder.startTabsHostRun();
    }

    public TextView mFooterMessage;

    // set footer
    public void showFooter(AppCompatActivity mAct)
    {
//		System.out.println("TabsHost / _showFooter ");
        if(mFooterMessage != null)
        {
            mFooterMessage.setTextColor(mAct.getResources().getColor(R.color.colorWhite));
            mFooterMessage.setText(getFooterMessage(mAct));
            mFooterMessage.setBackgroundColor(ColorSet.getBarColor(mAct));
        }
    }

    // get footer message of list view
    static String getFooterMessage(AppCompatActivity mAct)
    {
        int pageTableId = Pref.getPref_focusView_page_tableId(mAct);
        DB_page mDb_page = new DB_page(pageTableId);
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

    public void removePages()
    {
        System.out.println("TabsHost / _removePages");
    	if( mTabsPagerAdapter == null)
    		return;

        ArrayList<Page> fragmentList = mTabsPagerAdapter.fragmentList;
        if( (fragmentList != null) &&
            (fragmentList.size() >0)  )
        {
            RecyclerView listView = fragmentList.get(getFocus_tabPos()).recyclerView;//drag_listView;

            if(listView != null)
                store_listView_vScroll(listView);

            for (int i = 0; i < fragmentList.size(); i++) {
//                System.out.println("TabsHost / _removeTabs / i = " + i);
//                fragmentList.remove(i);
                act.getSupportFragmentManager().beginTransaction().remove(fragmentList.get(i)).commit();
            }

        }
    }

}