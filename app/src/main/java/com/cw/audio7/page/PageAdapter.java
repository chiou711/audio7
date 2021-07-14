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

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.audio.Audio7Player;
import com.cw.audio7.audio.AudioUi_page;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.note.Note;
import com.cw.audio7.note_edit.Note_edit;
import com.cw.audio7.audio.Audio_manager;
import com.cw.audio7.audio.BackgroundAudioService;
import com.cw.audio7.page.item_touch_helper.ItemTouchHelperAdapter;
import com.cw.audio7.page.item_touch_helper.ItemTouchHelperViewHolder;
import com.cw.audio7.page.item_touch_helper.OnStartDragListener;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.image.AsyncTaskAudioBitmap;
import com.cw.audio7.util.preferences.Pref;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
//import es.claucookie.miniequalizerlibrary.EqualizerView;
//import pl.droidsonroids.gif.GifDrawable;
//import pl.droidsonroids.gif.GifImageView;

import static com.cw.audio7.db.DB_page.KEY_NOTE_AUDIO_URI;
import static com.cw.audio7.db.DB_page.KEY_NOTE_MARKING;
import static com.cw.audio7.main.MainAct.mFolderUi;
import static com.cw.audio7.page.Page.swapRows;

// Pager adapter
public class PageAdapter extends RecyclerView.Adapter<PageAdapter.ViewHolder>
        implements ItemTouchHelperAdapter
{
	private AppCompatActivity act;
    final DB_folder dbFolder;
	private DB_page mDb_page;
    private final OnStartDragListener mDragStartListener;
	private final int page_table_id;
    private int style;
    List<Db_cache> listCache;
    AudioUi_page audioUi_page;
    View panelView;


    PageAdapter(AppCompatActivity _act,View _panelView,int pageTableId, OnStartDragListener dragStartListener) {
//        System.out.println("PageAdapter / constructor / pageTableId = " + pageTableId );
	    act = _act;
	    panelView = _panelView;
	    mDragStartListener = dragStartListener;

        dbFolder = new DB_folder(act,Pref.getPref_focusView_folder_tableId(act));
	    page_table_id = pageTableId;

        // style
        style = 0;
        int pagesCount = dbFolder.getPagesCount(true);
        for(int i=0;i<pagesCount;i++) {
            if (dbFolder.getPageTableId(i,true) == page_table_id) {
                style = dbFolder.getPageStyle(i ,true);
                break;
            }
        }

        updateDbCache();
    }

    // update list cache from DB
    public void updateDbCache() {
//        System.out.println("PageAdapter / _updateDbCache " );
        listCache = new ArrayList<>();

        int notesCount = getItemCount();
        mDb_page = new DB_page(act, page_table_id);
        mDb_page.open();
        Cursor cursor = mDb_page.cursor_note;
        for(int i=0;i<notesCount;i++) {
            if (cursor.moveToPosition(i)) {
                Db_cache cache = new Db_cache();
                cache.audioUri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_AUDIO_URI));
                cache.marking = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_MARKING));
                listCache.add(cache);
            }
        }
        cursor.close();
        mDb_page.close();
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
		TextView rowId;
		View audioBlock;
        TextView audioTitle;
        TextView audioArtist;
        TextView thumbLength;
		TextView textTitle;
		View playlistOperation;
        ImageView btnMarking;
        ImageViewCustom btnDrag;
		View thumbBlock;
		ImageView thumbAudio;
		ProgressBar progressBar;
//        GifImageView gifAudio;
        EqualizerView gifAudio;

        public ViewHolder(View v) {
            super(v);

            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });

            rowId= (TextView) v.findViewById(R.id.row_id);
            audioBlock = v.findViewById(R.id.audio_block);
            audioTitle = (TextView) v.findViewById(R.id.row_audio_title);
            audioArtist = (TextView) v.findViewById(R.id.row_audio_artist);
            playlistOperation = v.findViewById(R.id.playlist_operation);
            btnMarking = (ImageView) v.findViewById(R.id.btn_marking);
            btnDrag = (ImageViewCustom) v.findViewById(R.id.btn_drag);
            thumbBlock = v.findViewById(R.id.row_thumb_nail);
            thumbAudio = (ImageView) v.findViewById(R.id.thumb_audio);
            thumbLength = (TextView) v.findViewById(R.id.thumb_length);
            progressBar = (ProgressBar) v.findViewById(R.id.thumb_progress);
            gifAudio = v.findViewById(R.id.row_audio_gif);
        }

        public TextView getTextView() {
            return textTitle;
        }

        @Override
        public void onItemSelected() {
            ((CardView)itemView).setCardBackgroundColor(ColorSet.getButtonColor(act));
        }

        @Override
        public void onItemClear() {
            ((CardView)itemView).setCardBackgroundColor(act.getResources().getColor(R.color.colorBlack));
        }
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        int resource_id;
        if(Pref.getPref_card_view_enable_large_view(act))
            resource_id = R.layout.page_card_view_high;
        else
            resource_id = R.layout.page_card_view;

        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(resource_id, viewGroup, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

//        System.out.println("PageAdapter / _onBindViewHolder / position = " + position);

        ((CardView)holder.itemView).setCardBackgroundColor(act.getResources().getColor(R.color.colorBlack));

        // get DB data
        String audioUri = null;
        int marking = 0;

        // add check to avoid exception during Copy/Move checked
//        System.out.println("PageAdapter / _onBindViewHolder / listCache.size() = " + listCache.size());
        if( (listCache != null)
            && (listCache.size() > 0)
            && (position!=listCache.size()) )
        {
            audioUri = listCache.get(position).audioUri;
            marking = listCache.get(position).marking;
        } else  {
            audioUri = "";
            marking = 0;
        }

        AsyncTaskAudioBitmap audioAsyncTask;
        /**
         *  control block
         */
        // show row Id
        holder.rowId.setText(String.valueOf(position+1));
        holder.rowId.setTextColor(act.getResources().getColor(R.color.colorWhite));

        if( Pref.getPref_card_view_enable_select(act) ||
            Pref.getPref_card_view_enable_draggable(act) )
            holder.playlistOperation.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

        // show marking check box
        if(Pref.getPref_card_view_enable_select(act)) {
            // show checked icon
            holder.btnMarking.setVisibility(View.VISIBLE);

            if(marking == 1)
            {
                holder.btnMarking.setBackgroundResource(style % 2 == 1 ?
                        R.drawable.btn_check_on_holo_light :
                        R.drawable.btn_check_on_holo_dark);
            }
            else
            {
                holder.btnMarking.setBackgroundResource(style % 2 == 1 ?
                        R.drawable.btn_check_off_holo_light :
                        R.drawable.btn_check_off_holo_dark);
            }
        }
        else {
            holder.btnMarking.setVisibility(View.GONE);
        }

        // show drag button
        if(Pref.getPref_card_view_enable_draggable(act))
            holder.btnDrag.setVisibility(View.VISIBLE);
        else
            holder.btnDrag.setVisibility(View.GONE);

        // show audio name
        if(Util.isUriExisted(audioUri, act)) {

            // set audio name
            String[] audio_name = null;
            if(!Util.isEmptyString(audioUri))
                audio_name = Util.getDisplayNameByUriString(audioUri, act);

//            System.out.println("-> title = " + audio_name[0]);
//            System.out.println("-> artist = " + audio_name[1]);

            holder.audioTitle.setText(audio_name[0]);

            if(!Util.isEmptyString(audio_name[1]))
                holder.audioArtist.setText(audio_name[1]);
            else
                holder.audioArtist.setText("N/A");
//                holder.audioArtist.setVisibility(View.INVISIBLE);

            holder.audioTitle.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
            holder.audioArtist.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
        }
        else {
            holder.audioTitle.setText(R.string.file_not_found);
            holder.audioArtist.setText("");
        }

        /** show audio highlight if audio is not at Stop */
        if( (marking !=0) &&
            (position == Audio_manager.mAudioPos)  &&
            Audio7Player.isOnAudioPlayingPage() &&
            (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) )
        {
//            System.out.println("PageAdapter / _getView / show highlight / position = " + position);
            mFolderUi.tabsHost.getCurrentPage().mHighlightPosition = position;

            // background case 1: border
//            holder.audioBlock.setBackgroundResource(R.drawable.bg_highlight_border);

            // background case 2: normal
//            holder.audioTitle.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
//            holder.audioArtist.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

            holder.rowId.setBackgroundColor(ColorSet.getHighlightColor(act));

            // background case 3: fill highlight
//            holder.audioTitle.setBackgroundColor(ColorSet.getHighlightColor(mAct));
//            holder.audioArtist.setVisibility(View.GONE);

            // gif case 1
            // cf: https://stackoverflow.com/questions/6533942/adding-gif-image-in-an-imageview-in-android
//            if(BackgroundAudioService.mMediaPlayer!= null)
//            {
//                if(BackgroundAudioService.mMediaPlayer.isPlaying())
//                    ((GifDrawable) holder.gifAudio.getDrawable()).start();
//                else
//                    ((GifDrawable) holder.gifAudio.getDrawable()).pause();
//            }

            // gif case 2
            // cf: https://github.com/claucookie/mini-equalizer-library-android
            if(Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PLAY)
                holder.gifAudio.animateBars();
            else
                holder.gifAudio.stopBars();

            holder.gifAudio.setVisibility(View.VISIBLE);
        }
        else
        {

//			System.out.println("PageAdapter / _getView / not show highlight ");

            // background case: normal
//            holder.audioTitle.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
//            holder.audioArtist.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

            holder.rowId.setBackground(act.getDrawable(R.drawable.bg_text_rounded));

            // gif case
            holder.audioBlock.setVisibility(View.VISIBLE);
            holder.audioBlock.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
            holder.gifAudio.setVisibility(View.GONE);
        }

        if(!Util.isEmptyString(audioUri)) {
            if( marking == 1) {
                holder.audioTitle.setTextColor(ColorSet.mText_ColorArray[style]);
                holder.audioArtist.setTextColor(ColorSet.mText_ColorArray[style]);
            } else {
                holder.audioTitle.setTextColor(act.getResources().getColor(R.color.colorGray));
                holder.audioArtist.setTextColor(act.getResources().getColor(R.color.colorGray));
            }
        }

        // show audio icon and block
        if(Util.isEmptyString(audioUri))
            holder.audioBlock.setVisibility(View.GONE);

            // case : show audio thumb nail if audio Uri exists
            if (UtilAudio.hasAudioExtension(audioUri)) {
                holder.thumbBlock.setVisibility(View.VISIBLE);
                holder.thumbAudio.setVisibility(View.VISIBLE);
                holder.thumbLength.setVisibility(View.VISIBLE);

                int in_sample_size;
                if (Pref.getPref_card_view_enable_large_view(act))
                    in_sample_size = 1;
                else
                    in_sample_size = 1;//8; // 1/8 the width/height of the original

                //todo disable the following will decrease native memory usage
            try {
                audioAsyncTask = new AsyncTaskAudioBitmap(act,
                        audioUri,
                        holder.thumbAudio,
                        holder.progressBar,
                        holder.thumbLength,
                        false,
                        in_sample_size);
                audioAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Searching media ...");
            } catch (Exception e) {
                Log.e("PageAdapter", "AsyncTaskAudioBitmap error");
                holder.thumbBlock.setVisibility(View.GONE);
                holder.thumbAudio.setVisibility(View.GONE);
            }
		}
		else
		{
			holder.thumbBlock.setVisibility(View.GONE);
			holder.thumbAudio.setVisibility(View.GONE);
		}

        setBindViewHolder_listeners(holder,position);
    }

    /**
     * Set bind view holder listeners
     * @param viewHolder
     * @param position
     */
    void setBindViewHolder_listeners(ViewHolder viewHolder, final int position)
    {

//        System.out.println("PageAdapter / setBindViewHolder_listeners / position = " + position);

        /**
         *  control block
         */
        // on mark note
        viewHolder.btnMarking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                System.out.println("PageAdapter / _getView / btnMarking / _onClick");

                // toggle marking and get new setting
                int marking = toggleNoteMarking(act,position);

                updateDbCache();

                // Stop if unmarked item is at playing state
                if(Audio_manager.mAudioPos == position) {
                    UtilAudio.stopAudioIfNeeded();
                }

                //Toggle marking will resume page, so do Store v scroll
                RecyclerView listView = mFolderUi.tabsHost.mTabsPagerAdapter.fragmentList.get(mFolderUi.tabsHost.getFocus_tabPos()).recyclerView;
                mFolderUi.tabsHost.store_listView_vScroll(listView);
                mFolderUi.tabsHost.isDoingMarking = true;

                // set marking icon
                if(marking == 1)
                {
                    v.setBackgroundResource(style % 2 == 1 ?
                            R.drawable.btn_check_on_holo_light :
                            R.drawable.btn_check_on_holo_dark);
                }
                else
                {
                    v.setBackgroundResource(style % 2 == 1 ?
                            R.drawable.btn_check_off_holo_light :
                            R.drawable.btn_check_off_holo_dark);
                }

                // set audio title / artist color
                DB_page db_page = new DB_page(act,mFolderUi.tabsHost.getCurrentPageTableId());
                String audioUri = db_page.getNoteAudioUri(position,true);

                if(!Util.isEmptyString(audioUri)) {
                    if( marking == 1) {
                        viewHolder.audioTitle.setTextColor(ColorSet.mText_ColorArray[style]);
                        viewHolder.audioArtist.setTextColor(ColorSet.mText_ColorArray[style]);
                    } else {
                        viewHolder.audioTitle.setTextColor(act.getResources().getColor(R.color.colorGray));
                        viewHolder.audioArtist.setTextColor(act.getResources().getColor(R.color.colorGray));
                    }
                }

                mFolderUi.tabsHost.showFooter(MainAct.mAct);

                // update audio info
                if(Audio7Player.isOnAudioPlayingPage()) {
                    System.out.println("PageAdapter / _getView / btnMarking / is AudioPlayingPage");
                    Audio_manager.setupAudioList();
                }
            }
        });

        // on view note
        viewHolder.thumbBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAudioPanel_note(position);
            }
        });

        // on play audio
        viewHolder.audioBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // check if selected
                DB_page db_page = new DB_page(act,mFolderUi.tabsHost.getCurrentPageTableId());
                int marking = db_page.getNoteMarking(position,true);
                if(marking == 0) {
                    Toast.makeText(act,R.string.is_an_unchecked_item,Toast.LENGTH_SHORT).show();
                    return;
                }

                // case 1: open Note audio
//                openAudioPanel_note(position);

                // case 2: open Page audio
                Audio_manager.stopAudioPlayer();
                openAudioPanel_page(position);
                Audio_manager.setupAudioList();

                String audioUriStr = db_page.getNoteAudioUri(position,true);
                Audio_manager.mAudioUri = audioUriStr;

                mFolderUi.tabsHost.audio7Player = new Audio7Player(act,panelView,audioUriStr);
                audioUi_page = new AudioUi_page(act, mFolderUi.tabsHost.audio7Player,panelView,audioUriStr);

                mFolderUi.tabsHost.audio7Player.runAudioState();

                mFolderUi.tabsHost.showPlayingTab();
            }
        });

        // on edit note
        viewHolder.audioBlock.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                DB_page db_page = new DB_page(act, mFolderUi.tabsHost.getCurrentPageTableId());
                Long rowId = db_page.getNoteId(position,true);

                Intent i = new Intent(act, Note_edit.class);
                i.putExtra("list_view_position", position);
                i.putExtra(DB_page.KEY_NOTE_ID, rowId);
                i.putExtra(DB_page.KEY_NOTE_TITLE, db_page.getNoteTitle_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_AUDIO_URI , db_page.getNoteAudioUri_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_BODY, db_page.getNoteBody_byId(rowId));
                act.startActivity(i);

                return true;
            }
        });

        // Start a drag whenever the handle view it touched
        viewHolder.btnDrag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked())
                {
                    case MotionEvent.ACTION_DOWN:
                        mDragStartListener.onStartDrag(viewHolder);
                        System.out.println("PageAdapter / onTouch / ACTION_DOWN");
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        return true;
                }
                return false;
            }


        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
	    mDb_page = new DB_page(act, page_table_id);
	    return  mDb_page.getNotesCount(true);
    }

    // open Note audio panel
    void openAudioPanel_note(int position) {
        mFolderUi.tabsHost.getCurrentPage().mCurrPlayPosition = position;
        DB_page db_page = new DB_page(act,mFolderUi.tabsHost.getCurrentPageTableId());
        int count = db_page.getNotesCount(true);
        if(position < count)
        {
            /** Open Note Intent */
//                    Intent intent;
//                    intent = new Intent(mAct, Note.class);
//                    intent.putExtra("POSITION", position);
//                    mAct.startActivity(intent);

            // hide the tab layout
            mFolderUi.tabsHost.mTabLayout.setVisibility(View.GONE);
            act.getSupportFragmentManager()
                    .findFragmentById(R.id.content_frame)
                    .getView()
                    .setBackgroundColor(act.getResources().getColor(R.color.colorBlack));

            /** Open Note fragment */
            Note noteFragment = new Note();
            final Bundle args = new Bundle();
            args.putInt("POSITION", position);
            noteFragment.setArguments(args);

            FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fragment_slide_up, R.anim.fragment_slide_down, R.anim.fragment_slide_up, R.anim.fragment_slide_down);
            transaction.replace(R.id.content_frame, noteFragment, "note").addToBackStack("note").commit();
        }

    }

    // open Page audio panel
    public void openAudioPanel_page(int position) {
        System.out.println("PageAdapter / _openAudioPanel_page");

        DB_page db_page = new DB_page(act, mFolderUi.tabsHost.getCurrentPageTableId());
        int notesCount = db_page.getNotesCount(true);
        if(position >= notesCount) //end of list
            return ;

        int marking = db_page.getNoteMarking(position,true);
        String uriString = db_page.getNoteAudioUri(position,true);

        boolean isAudioUri = false;
        if( !Util.isEmptyString(uriString) && (marking == 1))
            isAudioUri = true;

        if(isAudioUri) {

            // create new Intent to play audio
            Audio_manager.mAudioPos = position;

            // update playing page position
            MainAct.mPlaying_pagePos = mFolderUi.tabsHost.getFocus_tabPos();

            // update playing page table Id
            MainAct.mPlaying_pageTableId = mFolderUi.tabsHost.getCurrentPageTableId();

            // update playing folder position
            MainAct.mPlaying_folderPos = mFolderUi.getFocus_folderPos();

            // update playing folder table Id
            DB_drawer dB_drawer = new DB_drawer(act);
            MainAct.mPlaying_folderTableId = dB_drawer.getFolderTableId(MainAct.mPlaying_folderPos,true);

            mFolderUi.tabsHost.mTabsPagerAdapter.notifyDataSetChanged();
        }
    }

    // toggle mark of note
    public static int toggleNoteMarking(AppCompatActivity mAct, int position)
    {
        int marking = 0;
		DB_page db_page = new DB_page(mAct,mFolderUi.tabsHost.getCurrentPageTableId());
        db_page.open();
        int count = db_page.getNotesCount(false);
        if(position >= count) //end of list
        {
            db_page.close();
            return marking;
        }

        String strNote = db_page.getNoteTitle(position,false);
        String strAudioUri = db_page.getNoteAudioUri(position,false);
        String strNoteBody = db_page.getNoteBody(position,false);
        Long idNote =  db_page.getNoteId(position,false);

        // toggle the marking
        if(db_page.getNoteMarking(position,false) == 0)
        {
            db_page.updateNote(idNote, strNote,  strAudioUri, strNoteBody, 1,  false);
            marking = 1;
        }
        else
        {
            db_page.updateNote(idNote, strNote,  strAudioUri,   strNoteBody, 0, false);
            marking = 0;
        }
        db_page.close();

        System.out.println("PageAdapter / _toggleNoteMarking / position = " + position + ", marking = " + db_page.getNoteMarking(position,true));
        return  marking;
    }

    @Override
    public void onItemDismiss(int position) {
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPos, int toPos) {
//        System.out.println("PageAdapter / _onItemMove / fromPos = " +
//                        fromPos + ", toPos = " + toPos);

        notifyItemMoved(fromPos, toPos);

        int oriStartPos = fromPos;
        int oriEndPos = toPos;

        mDb_page = new DB_page(act, mFolderUi.tabsHost.getCurrentPageTableId());
        if(fromPos >= mDb_page.getNotesCount(true)) // avoid footer error
            return false;

        //reorder data base storage
        int loop = Math.abs(fromPos-toPos);
        for(int i=0;i< loop;i++)
        {
            swapRows(mDb_page, fromPos,toPos);
            if((fromPos-toPos) >0)
                toPos++;
            else
                toPos--;
        }

        if( Audio7Player.isOnAudioPlayingPage() &&
                (BackgroundAudioService.mMediaPlayer != null)				   )
        {
            if( (Page.mHighlightPosition == oriEndPos)  && (oriStartPos > oriEndPos))
            {
                Page.mHighlightPosition = oriEndPos+1;
            }
            else if( (Page.mHighlightPosition == oriEndPos) && (oriStartPos < oriEndPos))
            {
                Page.mHighlightPosition = oriEndPos-1;
            }
            else if( (Page.mHighlightPosition == oriStartPos)  && (oriStartPos > oriEndPos))
            {
                Page.mHighlightPosition = oriEndPos;
            }
            else if( (Page.mHighlightPosition == oriStartPos) && (oriStartPos < oriEndPos))
            {
                Page.mHighlightPosition = oriEndPos;
            }
            else if(  (Page.mHighlightPosition < oriEndPos) &&
                    (Page.mHighlightPosition > oriStartPos)   )
            {
                Page.mHighlightPosition--;
            }
            else if( (Page.mHighlightPosition > oriEndPos) &&
                    (Page.mHighlightPosition < oriStartPos)  )
            {
                Page.mHighlightPosition++;
            }

            Audio_manager.mAudioPos = Page.mHighlightPosition;
            Audio_manager.setupAudioList();
        }

        // update footer
        mFolderUi.tabsHost.showFooter(act);
        return true;
    }

    @Override
    public void onItemMoved(RecyclerView.ViewHolder sourceViewHolder, int fromPos, RecyclerView.ViewHolder targetViewHolder, int toPos) {
        System.out.println("PageAdapter / _onItemMoved");
        ((TextView)sourceViewHolder.itemView.findViewById(R.id.row_id)).setText(String.valueOf(toPos+1));
        ((TextView)targetViewHolder.itemView.findViewById(R.id.row_id)).setText(String.valueOf(fromPos+1));

        setBindViewHolder_listeners((ViewHolder)sourceViewHolder,toPos);
        setBindViewHolder_listeners((ViewHolder)targetViewHolder,fromPos);

        updateDbCache();
    }

}