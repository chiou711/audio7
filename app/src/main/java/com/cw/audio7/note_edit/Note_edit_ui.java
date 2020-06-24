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

package com.cw.audio7.note_edit;

import java.util.Date;

import com.cw.audio7.db.DB_folder;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.image.TouchImageView;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.preferences.Pref;
import com.cw.audio7.util.Util;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Note_edit_ui {

	private TextView audioTextView;

	private ImageView picImageView;
	private String audioUriInDB;
	String currAudioUri;
	String oriAudioUri;

	private EditText titleEditText;
	private EditText bodyEditText;
	private String oriTitle;
	private String oriBody;

	private Long noteId;
	private Long oriCreatedTime;
	private Long oriMarking;

	boolean bRollBackData;
	boolean bRemovePictureUri = false;
	boolean bRemoveAudioUri = false;

    private DB_page dB_page;
	private Activity act;
	private int style;
	private ProgressBar progressBar;
	private ProgressBar progressBarExpand;
	private TouchImageView enlargedImage;

	Note_edit_ui(Activity act, DB_page _db, Long noteId, String strTitle, String pictureUri, String audioUri, String drawingUri, String linkUri, String strBody, Long createdTime)
    {
    	this.act = act;
    	this.noteId = noteId;
    			
    	oriTitle = strTitle;
	    oriBody = strBody;
	    oriAudioUri = audioUri;

	    oriCreatedTime = createdTime;
	    currAudioUri = audioUri;
	    
	    dB_page = _db;//Page.mDb_page;
	    
	    oriMarking = dB_page.getNoteMarking_byId(noteId);
		
	    bRollBackData = false;
    }

	void UI_init()
    {

		UI_init_text();

    	audioTextView = (TextView) act.findViewById(R.id.edit_audio);
        picImageView = (ImageView) act.findViewById(R.id.edit_picture);

        progressBar = (ProgressBar) act.findViewById(R.id.edit_progress_bar);
        progressBarExpand = (ProgressBar) act.findViewById(R.id.edit_progress_bar_expand);

		DB_folder dbFolder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(act));
		style = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		enlargedImage = (TouchImageView)act.findViewById(R.id.expanded_image);

		//set audio color
//		audioTextView.setTextColor(Util.mText_ColorArray[style]);
//		audioTextView.setBackgroundColor(Util.mBG_ColorArray[style]);

		picImageView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

	    final InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

	private void UI_init_text()
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db = new DB_folder(MainAct.mAct, focusFolder_tableId);
		style = db.getPageStyle(TabsHost.getFocus_tabPos(), true);

		LinearLayout block = (LinearLayout) act.findViewById(R.id.edit_title_block);
		if(block != null)
			block.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		titleEditText = (EditText) act.findViewById(R.id.edit_title);
		bodyEditText = (EditText) act.findViewById(R.id.edit_body);

		//set title color
		titleEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		titleEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		//set body color
		bodyEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		bodyEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
	}

	void deleteNote(Long rowId)
    {
    	System.out.println("Note_edit_ui / _deleteNote");
        // for Add new note (noteId is null first), but decide to cancel
        if(rowId != null)
        	dB_page.deleteNote(rowId,true);
    }
    
    // populate text fields
	void populateFields_text(Long rowId)
	{
		if (rowId != null) {
			// title
			String strTitleEdit = dB_page.getNoteTitle_byId(rowId);
			titleEditText.setText(strTitleEdit);
			titleEditText.setSelection(strTitleEdit.length());

			// body
			String strBodyEdit = dB_page.getNoteBody_byId(rowId);
			bodyEditText.setText(strBodyEdit);
			bodyEditText.setSelection(strBodyEdit.length());
		}
        else
        {
            // renew title
            String strBlank = "";
            titleEditText.setText(strBlank);
            titleEditText.setSelection(strBlank.length());
            titleEditText.requestFocus();

            // renew body
            bodyEditText.setText(strBlank);
            bodyEditText.setSelection(strBlank.length());
        }
	}

    // populate all fields
	void populateFields_all(Long rowId)
    {
    	if (rowId != null) 
    	{
			populateFields_text(rowId);

			// load bitmap to image view
			{
	    		picImageView.setImageResource(style %2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);
			}
			
    		// audio
			audioUriInDB = dB_page.getNoteAudioUri_byId(rowId);
        	if(!Util.isEmptyString(audioUriInDB))
    		{
    			String audio_name = audioUriInDB;
				System.out.println("populateFields_all / set audio name / audio_name = " + audio_name);
				audioTextView.setText(act.getResources().getText(R.string.note_audio) + ": " + audio_name);
    		}
        	else
				audioTextView.setText("");
        }
    }

	private boolean isTitleModified()
    {
    	return !oriTitle.equals(titleEditText.getText().toString());
    }

	private boolean isAudioModified()
    {
    	if(oriAudioUri == null)
    		return false;
    	else
    		return !oriAudioUri.equals(audioUriInDB);
    }

	private boolean isBodyModified()
    {
    	return !oriBody.equals(bodyEditText.getText().toString());
    }

	boolean isNoteModified()
    {
    	boolean bModified = false;
//		System.out.println("Note_edit_ui / _isNoteModified / isTitleModified() = " + isTitleModified());
//		System.out.println("Note_edit_ui / _isNoteModified / isPictureModified() = " + isPictureModified());
//		System.out.println("Note_edit_ui / _isNoteModified / isAudioModified() = " + isAudioModified());
//		System.out.println("Note_edit_ui / _isNoteModified / isBodyModified() = " + isBodyModified());
//		System.out.println("Note_edit_ui / _isNoteModified / bRemoveAudioUri = " + bRemoveAudioUri);
    	if( isTitleModified() ||
    		isAudioModified() ||
    		isBodyModified() ||
    		bRemoveAudioUri)
    	{
    		bModified = true;
    	}
    	
    	return bModified;
    }

	Long saveStateInDB(Long rowId,boolean enSaveDb, String pictureUri, String audioUri, String drawingUri)
	{
    	String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();

        if(enSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( (!Util.isEmptyString(title)) ||
	        		(!Util.isEmptyString(body)) ||
	        		(!Util.isEmptyString(pictureUri)) ||
	        		(!Util.isEmptyString(audioUri))       )
	        	{
	        		// insert
	        		System.out.println("Note_edit_ui / _saveStateInDB / insert");
	        		rowId = dB_page.insertNote(title, pictureUri, audioUri, drawingUri, "linkUri", body, 0, (long) 0);// add new note, get return row Id
	        	}
        		currAudioUri = audioUri; // update file name
	        } 
	        else // for Edit
	        {
    	        Date now = new Date();
	        	if( !Util.isEmptyString(title) ||
	        		!Util.isEmptyString(body) ||
	        		!Util.isEmptyString(pictureUri) ||
	        		!Util.isEmptyString(audioUri)   )
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_edit_ui / _saveStateInDB / update: roll back");
	        			title = oriTitle;
	        			body = oriBody;
	        			Long time = oriCreatedTime;
	        			dB_page.updateNote(rowId, title, pictureUri, audioUri, drawingUri, "linkUri", body, oriMarking, time,true);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_edit_ui / _saveStateInDB / update new");
						System.out.println("--- rowId = " + rowId);
						System.out.println("--- oriMarking = " + oriMarking);
						System.out.println("--- audioUri = " + audioUri);

                        long marking;
                        if(null == oriMarking)
                            marking = 0;
                        else
                            marking = oriMarking;

                        boolean isOK;
	        			isOK = dB_page.updateNote(rowId, title, pictureUri, audioUri, drawingUri, "linkUri", body,
												marking, now.getTime(),true); // update note
	        			System.out.println("--- isOK = " + isOK);
	        		}
	        		currAudioUri = audioUri;
	        	}
	        	else if( Util.isEmptyString(title) &&
	        			 Util.isEmptyString(body) &&
 						 Util.isEmptyString(pictureUri) &&
						 Util.isEmptyString(drawingUri) &&
			        	 Util.isEmptyString(audioUri)      )
	        	{
	        		// delete
	        		System.out.println("Note_edit_ui / _saveStateInDB / delete");
	        		deleteNote(rowId);
			        rowId = null;
	        	}
	        }
        }

		return rowId;
	}

	// for confirmation condition
	void removePictureStringFromOriginalNote(Long rowId) {
    	dB_page.updateNote(rowId,
				oriTitle,
    				   "",
				oriAudioUri,
				"",
				"",
				oriBody,
				oriMarking,
				oriCreatedTime, true );
	}

	private void removePictureStringFromCurrentEditNote(Long rowId) {
        String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();
        
    	dB_page.updateNote(rowId,
    				   title,
    				   "",
				oriAudioUri,
				"",
    				   "linkUri",
    				   body,
				oriMarking,
				oriCreatedTime, true );
	}

	void removeAudioStringFromOriginalNote(Long rowId) {
    	dB_page.updateNote(rowId,
				oriTitle,
				"",
    				   "",
				"",
				"",
				oriBody,
				oriMarking,
				oriCreatedTime, true );
	}

	void removeAudioStringFromCurrentEditNote(Long rowId) {
        String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();
        dB_page.updateNote(rowId,
    				   title,
				"",
    				   "",
				"",
    				   "",
    				   body,
				oriMarking,
				oriCreatedTime, true );
	}

	public int getCount()
	{
		int noteCount = dB_page.getNotesCount(true);
		return noteCount;
	}
	
}