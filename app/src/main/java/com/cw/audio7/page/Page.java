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


package com.cw.audio7.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.page.item_touch_helper.OnStartDragListener;
import com.cw.audio7.page.item_touch_helper.SimpleItemTouchHelperCallback;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.image.ImageCache;
import com.cw.audio7.util.image.ImageFetcher;
import com.cw.audio7.util.preferences.Pref;
import com.cw.audio7.util.uil.UilCommon;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static com.cw.audio7.audio.BackgroundAudioService.audio_manager;

public class Page extends Fragment implements OnStartDragListener {

    public int page_tableId;

    public RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutMgr;
    public static int mCurrPlayPosition;
    public static int mHighlightPosition;
    public SeekBar seekBarProgress;

    public PageAdapter itemAdapter;
    private ItemTouchHelper itemTouchHelper;
    AppCompatActivity act;
    View panelView;

    // for Image Cache
    public ImageFetcher mImageFetcher;
    private int mImageThumbSize;
    private static final String IMAGE_CACHE_DIR = "thumbs";

    TabsHost tabsHost;

    public Page(){
    }

    public Page(AppCompatActivity _act, TabsHost _tabsHost, View _panelView){
        act = _act;
        tabsHost = _tabsHost;
        panelView = _panelView;

        // for Image Cache
        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(act, IMAGE_CACHE_DIR);

        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        mImageThumbSize = act.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);

        mImageFetcher = new ImageFetcher(act, mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.empty_photo);
        mImageFetcher.addImageCache(act.getSupportFragmentManager(), cacheParams);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        page_tableId = args.getInt("page_table_id");
//        System.out.println("Page / _onCreateView / page_tableId = " + page_tableId);

        View rootView = inflater.inflate(R.layout.page_view, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        TextView blankView = rootView.findViewById(R.id.blankPage);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        layoutMgr = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutMgr);

        int scrollPosition = 0;
        // If a layout manager has already been set, get current scroll position.
        if (recyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }
        recyclerView.scrollToPosition(scrollPosition);

        // add divider
//        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
//                DividerItemDecoration.VERTICAL));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(getContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(itemDecorator);

        UilCommon.init();

        fillData();

        if(tabsHost != null)
            tabsHost.showFooter(act);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(itemAdapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        if( (itemAdapter != null) &&
            (itemAdapter.getItemCount() ==0) ){ //todo bug: Attempt to invoke interface method 'int android.database.Cursor.getCount()' on a null object reference
            blankView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        else {
            blankView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // for Image Cache
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
               if (newState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    // Before Honeycomb pause image loading on scroll to help with performance
                    mImageFetcher.setPauseWork(true);
                } else {
                    mImageFetcher.setPauseWork(false);
                }
            }

        });

        return rootView;
    }


    @Override
    public void onResume() {
        System.out.println("Page / _onResume / page_tableId = " + page_tableId);
        super.onResume();
        if(Pref.getPref_focusView_page_tableId(act) == page_tableId) {

            if( (tabsHost!= null) && (recyclerView != null) ) {
                tabsHost.resume_listView_vScroll(recyclerView);
                System.out.println("Page / _onResume / resume_listView_vScroll");
            }
        }

        // for Image Cache
        mImageFetcher.setExitTasksEarly(false);

        // set doScroll flag for Scroll to playing item
        if( (audio_manager !=null) &&  audio_manager.willDoScroll())
            audio_manager.doScroll = true;
    }

    @Override
    public void onPause() {
        System.out.println("Page / _onPause ");
        super.onPause();
        // for Image Cache
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("Page / _onDestroy");
        // for Image Cache
        mImageFetcher.closeCache();
    }

    private void fillData()
    {
        //System.out.println("Page / _fillData / page_tableId = " + page_tableId);
        if(itemAdapter == null)
            itemAdapter = new PageAdapter(act, tabsHost,panelView,page_tableId, this);
        // Set PageAdapter_recycler as the adapter for RecyclerView.
        recyclerView.setAdapter(itemAdapter);
    }

    // swap rows
    protected static void swapRows(DB_page dB_page, int startPosition, int endPosition)
    {
        Long noteNumber1;
        String noteTitle1;
        String noteAudioUri1;
        String noteBodyString1;
        int markingIndex1;
        Long noteNumber2 ;
        String noteAudioUri2;
        String noteTitle2;
        String noteBodyString2;
        int markingIndex2;

        dB_page.open();
        noteNumber1 = dB_page.getNoteId(startPosition,false);
        noteTitle1 = dB_page.getNoteTitle(startPosition,false);
        noteAudioUri1 = dB_page.getNoteAudioUri(startPosition,false);
        noteBodyString1 = dB_page.getNoteBody(startPosition,false);
        markingIndex1 = dB_page.getNoteMarking(startPosition,false);

        noteNumber2 = dB_page.getNoteId(endPosition,false);
        noteTitle2 = dB_page.getNoteTitle(endPosition,false);
        noteAudioUri2 = dB_page.getNoteAudioUri(endPosition,false);
        noteBodyString2 = dB_page.getNoteBody(endPosition,false);
        markingIndex2 = dB_page.getNoteMarking(endPosition,false);

        dB_page.updateNote(noteNumber2,
                noteTitle1,
                noteAudioUri1,
                noteBodyString1,
                markingIndex1,
               false);

        dB_page.updateNote(noteNumber1,
                noteTitle2,
                noteAudioUri2,
                noteBodyString2,
                markingIndex2,
                false);

        dB_page.close();
    }

    static public void swapTopBottom(AppCompatActivity _act)
    {
        DB_page dB_page = new DB_page(  _act ,DB_page.getFocusPage_tableId());
        int startCursor = dB_page.getNotesCount(true)-1;
        int endCursor = 0;

        //reorder data base storage for ADD_NEW_TO_TOP option
        int loop = Math.abs(startCursor-endCursor);
        for(int i=0;i< loop;i++)
        {
            swapRows(dB_page, startCursor,endCursor);
            if((startCursor-endCursor) >0)
                endCursor++;
            else
                endCursor--;
        }
    }


    public int getNotesCountInPage(AppCompatActivity act)
    {
        int page_table_id = tabsHost.getCurrentPageTableId();
        DB_page db_page = new DB_page(act,page_table_id );
        int count = db_page.getNotesCount(true);
        return count;
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }
}
