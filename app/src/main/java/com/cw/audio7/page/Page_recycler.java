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


package com.cw.audio7.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.page.item_touch_helper.OnStartDragListener;
import com.cw.audio7.page.item_touch_helper.SimpleItemTouchHelperCallback;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.preferences.Pref;
import com.cw.audio7.util.uil.UilCommon;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Page_recycler extends Fragment implements OnStartDragListener {

    public int page_tableId;

    public RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutMgr;
    public static int mCurrPlayPosition;
    public static int mHighlightPosition;
    public SeekBar seekBarProgress;
    public AppCompatActivity act;

    public PageAdapter_recycler itemAdapter;
    private ItemTouchHelper itemTouchHelper;

    public Page_recycler(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Bundle args = getArguments();
        page_tableId = args.getInt("page_table_id");
        //System.out.println("Page_recycler / _onCreateView / page_tableId = " + page_tableId);

        View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);
        act = MainAct.mAct;

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

        TabsHost.showFooter(MainAct.mAct);

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

        return rootView;
    }

    @Override
    public void onResume() {
//        System.out.println("Page_recycler / _onResume / page_tableId = " + page_tableId);
        super.onResume();
        if(Pref.getPref_focusView_page_tableId(MainAct.mAct) == page_tableId) {
//            System.out.println("Page_recycler / _onResume / resume_listView_vScroll");
            TabsHost.resume_listView_vScroll(recyclerView);
        }
    }

    private void fillData()
    {
        //System.out.println("Page_recycler / _fillData / page_tableId = " + page_tableId);
        if(itemAdapter == null)
            itemAdapter = new PageAdapter_recycler(page_tableId, this);
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

    static public void swapTopBottom()
    {
        DB_page dB_page = new DB_page(  MainAct.mAct ,DB_page.getFocusPage_tableId());
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
        int page_table_id = TabsHost.getCurrentPageTableId();
        DB_page db_page = new DB_page(act,page_table_id );
        int count = db_page.getNotesCount(true);
        return count;
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }
}
