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

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.folder.FolderUi;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.note.Note;
import com.cw.audio7.note_edit.Note_edit;
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.operation.audio.AudioPlayer_page;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.page.item_touch_helper.ItemTouchHelperAdapter;
import com.cw.audio7.page.item_touch_helper.ItemTouchHelperViewHolder;
import com.cw.audio7.page.item_touch_helper.OnStartDragListener;
import com.cw.audio7.tabs.AudioUi_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.image.AsyncTaskAudioBitmap;
import com.cw.audio7.util.preferences.Pref;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import static com.cw.audio7.db.DB_page.KEY_NOTE_AUDIO_URI;
import static com.cw.audio7.db.DB_page.KEY_NOTE_MARKING;
import static com.cw.audio7.page.Page_recycler.swapRows;

// Pager adapter
public class PageAdapter_recycler extends RecyclerView.Adapter<PageAdapter_recycler.ViewHolder>
        implements ItemTouchHelperAdapter
{
	private AppCompatActivity mAct;
	Cursor cursor;
	private static int style;
    private DB_folder dbFolder;
	private DB_page mDb_page;
	private int page_pos;
    private final OnStartDragListener mDragStartListener;
	private int page_table_id;

    PageAdapter_recycler(int pagePos,  int pageTableId, OnStartDragListener dragStartListener) {
	    mAct = MainAct.mAct;
	    mDragStartListener = dragStartListener;

        dbFolder = new DB_folder(mAct,Pref.getPref_focusView_folder_tableId(mAct));
	    page_pos = pagePos;
	    page_table_id = pageTableId;
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        ImageView btnMarking;
		TextView rowId;
		View audioBlock;
        TextView audioTitle;
        TextView audioArtist;
        TextView thumbLength;
		TextView textTitle;
        ImageViewCustom btnDrag;
		View thumbBlock;
		ImageView thumbAudio;
		ProgressBar progressBar;
        GifImageView gifAudio;

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
            btnMarking = (ImageView) v.findViewById(R.id.btn_marking);
            thumbBlock = v.findViewById(R.id.row_thumb_nail);
            thumbAudio = (ImageView) v.findViewById(R.id.thumb_audio);
            thumbLength = (TextView) v.findViewById(R.id.thumb_length);
            btnDrag = (ImageViewCustom) v.findViewById(R.id.btn_drag);
            progressBar = (ProgressBar) v.findViewById(R.id.thumb_progress);
            gifAudio = v.findViewById(R.id.row_audio_gif);
        }

        public TextView getTextView() {
            return textTitle;
        }

        @Override
        public void onItemSelected() {
//            itemView.setBackgroundColor(Color.LTGRAY);
            ((CardView)itemView).setCardBackgroundColor(MainAct.mAct.getResources().getColor(R.color.button_color));
        }

        @Override
        public void onItemClear() {
//            ((CardView)itemView).setCardBackgroundColor(ColorSet.mBG_ColorArray[style]);
            ((CardView)itemView).setCardBackgroundColor(ColorSet.color_black);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        int resource_id;
        if(Pref.getPref_card_view_enable_large_view(mAct))
            resource_id = R.layout.page_view_card_high;
        else
            resource_id = R.layout.page_view_card;

        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(resource_id, viewGroup, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

//        System.out.println("PageAdapter_recycler / _onBindViewHolder / position = " + position);

        // style
	    style = dbFolder.getPageStyle(page_pos, true);

//        ((CardView)holder.itemView).setCardBackgroundColor(ColorSet.mBG_ColorArray[style]);
        ((CardView)holder.itemView).setCardBackgroundColor(ColorSet.color_black);


        // get DB data
        String audioUri = null;
        int marking = 0;

	    mDb_page = new DB_page(mAct, page_table_id);
	    mDb_page.open();
	    cursor = mDb_page.mCursor_note;
        if(cursor.moveToPosition(position)) {
            audioUri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_AUDIO_URI));
            marking = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_MARKING));
        }
	    mDb_page.close();

        /**
         *  control block
         */
        // show row Id
        holder.rowId.setText(String.valueOf(position+1));
        holder.rowId.setTextColor(ColorSet.color_white);

        // show marking check box
        if(Pref.getPref_card_view_enable_select(mAct)) {
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
        if(Pref.getPref_card_view_enable_draggable(mAct))
            holder.btnDrag.setVisibility(View.VISIBLE);
        else
            holder.btnDrag.setVisibility(View.GONE);

        // show audio name
        if(Util.isUriExisted(audioUri, mAct)) {

            // set audio name
            String[] audio_name = null;
            if(!Util.isEmptyString(audioUri))
                audio_name = Util.getDisplayNameByUriString(audioUri, mAct);

//            System.out.println("-> title = " + audio_name[0]);
//            System.out.println("-> artist = " + audio_name[1]);

            holder.audioTitle.setText(audio_name[0]);

            if(!Util.isEmptyString(audio_name[1]))
                holder.audioArtist.setText(audio_name[1]);
            else
                holder.audioArtist.setText("N/A");
//                holder.audioArtist.setVisibility(View.INVISIBLE);
        }
        else {
            holder.audioTitle.setText(R.string.file_not_found);
            holder.audioArtist.setText("");
        }

        // show audio highlight if audio is not at Stop
        if( PageUi.isAudioPlayingPage() &&
            (marking !=0) &&
            (position == Audio_manager.mAudioPos)  &&
            (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) &&
            (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)   )
        {
//            System.out.println("PageAdapter / _getView / show highlight / position = " + position);
            TabsHost.getCurrentPage().mHighlightPosition = position;
//            holder.audioBlock.setVisibility(View.VISIBLE);

            // background case 1: border
//            holder.audioBlock.setBackgroundResource(R.drawable.bg_highlight_border);

            // background case 2: normal
//            holder.audioTitle.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
//            holder.audioArtist.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

            // background case 3: fill highlight
            holder.audioTitle.setBackgroundColor(ColorSet.getHighlightColor(mAct));
            holder.audioArtist.setVisibility(View.GONE);

            // gif case
            // cf: https://stackoverflow.com/questions/6533942/adding-gif-image-in-an-imageview-in-android
            if(BackgroundAudioService.mMediaPlayer!= null)
            {
                if(BackgroundAudioService.mMediaPlayer.isPlaying())
                    ((GifDrawable) holder.gifAudio.getDrawable()).start();
                else
                    ((GifDrawable) holder.gifAudio.getDrawable()).pause();
            }
            holder.gifAudio.setVisibility(View.VISIBLE);
        }
        else
        {

//			System.out.println("PageAdapter / _getView / not show highlight ");

            // background case: normal
            holder.audioTitle.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
            holder.audioArtist.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

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
                holder.audioTitle.setTextColor(ColorSet.color_gray);
                holder.audioArtist.setTextColor(ColorSet.color_gray);
            }
        }

        // show audio icon and block
        if(Util.isEmptyString(audioUri))
            holder.audioBlock.setVisibility(View.GONE);

		// case : show audio thumb nail if audio Uri exists
		if(UtilAudio.hasAudioExtension(audioUri) )
		{
			holder.thumbBlock.setVisibility(View.VISIBLE);
			holder.thumbAudio.setVisibility(View.VISIBLE);
			holder.thumbLength.setVisibility(View.VISIBLE);

			int in_sample_size;
			if(Pref.getPref_card_view_enable_large_view(mAct))
                in_sample_size = 1;
			else
                in_sample_size = 8;

            try {
                AsyncTaskAudioBitmap audioAsyncTask;
                audioAsyncTask = new AsyncTaskAudioBitmap(mAct,
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

//        System.out.println("PageAdapter_recycler / setBindViewHolder_listeners / position = " + position);

        /**
         *  control block
         */
        // on mark note
        viewHolder.btnMarking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                System.out.println("PageAdapter / _getView / btnMarking / _onClick");
                // toggle marking
                toggleNoteMarking(mAct,position);

                // Stop if unmarked item is at playing state
                if(Audio_manager.mAudioPos == position) {
                    UtilAudio.stopAudioIfNeeded();
                }

                //Toggle marking will resume page, so do Store v scroll
                RecyclerView listView = TabsHost.mTabsPagerAdapter.fragmentList.get(TabsHost.getFocus_tabPos()).recyclerView;
                TabsHost.store_listView_vScroll(listView);
                TabsHost.isDoingMarking = true;

                TabsHost.reloadCurrentPage();
                TabsHost.showFooter(MainAct.mAct);

                // update audio info
                if(PageUi.isAudioPlayingPage()) {
                    System.out.println("PageAdapter / _getView / btnMarking / is AudioPlayingPage");
                    AudioPlayer_page.prepareAudioInfo();
                }
            }
        });

        // on view note
        viewHolder.thumbBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TabsHost.getCurrentPage().mCurrPlayPosition = position;
                DB_page db_page = new DB_page(mAct,TabsHost.getCurrentPageTableId());
                int count = db_page.getNotesCount(true);
                if(position < count)
                {
                    // apply Note class
//                    Intent intent;
//                    intent = new Intent(mAct, Note.class);
//                    intent.putExtra("POSITION", position);
//                    mAct.startActivity(intent);

                    // hide the tab layout
                    TabsHost.mTabLayout.setVisibility(View.GONE);
                    mAct.getSupportFragmentManager().findFragmentById(R.id.content_frame).getView().setBackgroundColor(ColorSet.color_black);

                    Note noteFragment = new Note();
                    final Bundle args = new Bundle();
                    args.putInt("POSITION", position);
                    noteFragment.setArguments(args);
                    FragmentTransaction transaction = mAct.getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.anim.fragment_slide_up, R.anim.fragment_slide_down, R.anim.fragment_slide_up, R.anim.fragment_slide_down);
                    transaction.replace(R.id.content_frame, noteFragment, "note").addToBackStack("note").commit();
                }
            }
        });

        // on edit note
        viewHolder.audioBlock.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                DB_page db_page = new DB_page(mAct, TabsHost.getCurrentPageTableId());
                Long rowId = db_page.getNoteId(position,true);

                Intent i = new Intent(mAct, Note_edit.class);
                i.putExtra("list_view_position", position);
                i.putExtra(DB_page.KEY_NOTE_ID, rowId);
                i.putExtra(DB_page.KEY_NOTE_TITLE, db_page.getNoteTitle_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_AUDIO_URI , db_page.getNoteAudioUri_byId(rowId));
                i.putExtra(DB_page.KEY_NOTE_BODY, db_page.getNoteBody_byId(rowId));
                mAct.startActivity(i);

                return true;
            }
        });

        // on play audio
        viewHolder.audioBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                TabsHost.reloadCurrentPage();// after Drag and drop: this is needed to update thumb nail and title

                Audio_manager.setAudioPlayMode(Audio_manager.PAGE_PLAY_MODE);
                DB_page db_page = new DB_page(mAct, TabsHost.getCurrentPageTableId());
                int notesCount = db_page.getNotesCount(true);
                if(position >= notesCount) //end of list
                    return ;

                int marking = db_page.getNoteMarking(position,true);
                String uriString = db_page.getNoteAudioUri(position,true);

                boolean isAudioUri = false;
                if( !Util.isEmptyString(uriString) && (marking == 1))
                    isAudioUri = true;

                if(position < notesCount) // avoid footer error
                {
                    if(isAudioUri)
                    {
                        // cancel playing
                        if(BackgroundAudioService.mMediaPlayer != null)
                        {
                            if(BackgroundAudioService.mMediaPlayer.isPlaying())
                                BackgroundAudioService.mMediaPlayer.pause();

                            if((AudioPlayer_page.mAudioHandler != null) &&
                                    (TabsHost.audioPlayer_page != null)        ){
                                AudioPlayer_page.mAudioHandler.removeCallbacks(TabsHost.audioPlayer_page.page_runnable);
                            }

                            BackgroundAudioService.mMediaPlayer.release();
                            BackgroundAudioService.mMediaPlayer = null;
                        }

                        Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);

                        // create new Intent to play audio
                        Audio_manager.mAudioPos = position;
                        Audio_manager.setAudioPlayMode(Audio_manager.PAGE_PLAY_MODE);

                        TabsHost.audioUi_page = new AudioUi_page(mAct);
                        TabsHost.audioUi_page.initAudioBlock(MainAct.mAct);

                        TabsHost.audioPlayer_page = new AudioPlayer_page(mAct,TabsHost.audioUi_page);
                        AudioPlayer_page.prepareAudioInfo();
                        TabsHost.audioPlayer_page.runAudioState();

                        // update audio play position
                        TabsHost.audioPlayTabPos = page_pos;

                        // update audio panel
                        TabsHost.audioUi_page.updateAudioPanel_page(TabsHost.audioUi_page.audioPanel_play_button,
                                TabsHost.audioUi_page.audio_panel_title_textView);

                        // update playing page position
                        MainAct.mPlaying_pagePos = TabsHost.getFocus_tabPos();

                        // update playing page table Id
                        MainAct.mPlaying_pageTableId = TabsHost.getCurrentPageTableId();

                        // update playing folder position
                        MainAct.mPlaying_folderPos = FolderUi.getFocus_folderPos();

                        // update playing folder table Id
                        DB_drawer dB_drawer = new DB_drawer(mAct);
                        MainAct.mPlaying_folderTableId = dB_drawer.getFolderTableId(MainAct.mPlaying_folderPos,true);

                        TabsHost.mTabsPagerAdapter.notifyDataSetChanged();
                    }
                }
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
                        System.out.println("PageAdapter_recycler / onTouch / ACTION_DOWN");
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
	    mDb_page = new DB_page(mAct, page_table_id);
	    return  mDb_page.getNotesCount(true);
    }

    // toggle mark of note
    public static int toggleNoteMarking(AppCompatActivity mAct, int position)
    {
        int marking = 0;
		DB_page db_page = new DB_page(mAct,TabsHost.getCurrentPageTableId());
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

        System.out.println("PageAdapter_recycler / _toggleNoteMarking / position = " + position + ", marking = " + db_page.getNoteMarking(position,true));
        return  marking;
    }

    @Override
    public void onItemDismiss(int position) {
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPos, int toPos) {
//        System.out.println("PageAdapter_recycler / _onItemMove / fromPos = " +
//                        fromPos + ", toPos = " + toPos);

        notifyItemMoved(fromPos, toPos);

        int oriStartPos = fromPos;
        int oriEndPos = toPos;

        mDb_page = new DB_page(mAct, TabsHost.getCurrentPageTableId());
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

        if( PageUi.isAudioPlayingPage() &&
                (BackgroundAudioService.mMediaPlayer != null)				   )
        {
            if( (Page_recycler.mHighlightPosition == oriEndPos)  && (oriStartPos > oriEndPos))
            {
                Page_recycler.mHighlightPosition = oriEndPos+1;
            }
            else if( (Page_recycler.mHighlightPosition == oriEndPos) && (oriStartPos < oriEndPos))
            {
                Page_recycler.mHighlightPosition = oriEndPos-1;
            }
            else if( (Page_recycler.mHighlightPosition == oriStartPos)  && (oriStartPos > oriEndPos))
            {
                Page_recycler.mHighlightPosition = oriEndPos;
            }
            else if( (Page_recycler.mHighlightPosition == oriStartPos) && (oriStartPos < oriEndPos))
            {
                Page_recycler.mHighlightPosition = oriEndPos;
            }
            else if(  (Page_recycler.mHighlightPosition < oriEndPos) &&
                    (Page_recycler.mHighlightPosition > oriStartPos)   )
            {
                Page_recycler.mHighlightPosition--;
            }
            else if( (Page_recycler.mHighlightPosition > oriEndPos) &&
                    (Page_recycler.mHighlightPosition < oriStartPos)  )
            {
                Page_recycler.mHighlightPosition++;
            }

            Audio_manager.mAudioPos = Page_recycler.mHighlightPosition;
            AudioPlayer_page.prepareAudioInfo();
        }

        // update footer
        TabsHost.showFooter(mAct);
        return true;
    }

    @Override
    public void onItemMoved(RecyclerView.ViewHolder sourceViewHolder, int fromPos, RecyclerView.ViewHolder targetViewHolder, int toPos) {
        System.out.println("PageAdapter_recycler / _onItemMoved");
        ((TextView)sourceViewHolder.itemView.findViewById(R.id.row_id)).setText(String.valueOf(toPos+1));
        ((TextView)targetViewHolder.itemView.findViewById(R.id.row_id)).setText(String.valueOf(fromPos+1));

        setBindViewHolder_listeners((ViewHolder)sourceViewHolder,toPos);
        setBindViewHolder_listeners((ViewHolder)targetViewHolder,fromPos);
    }

}