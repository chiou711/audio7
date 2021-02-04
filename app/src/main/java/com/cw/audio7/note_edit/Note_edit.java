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

import com.cw.audio7.audio.Audio7Player;
import com.cw.audio7.audio.Audio_manager;
import com.cw.audio7.page.Page;
import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.page.PageUi;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Note_edit extends Activity 
{

    private Long noteId;
    private String title,   audioUri, body;
    Note_edit_ui note_edit_ui;
    private boolean enSaveDb = true;
    DB_page dB;
    int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        // check note count first
	    dB = new DB_page(this, TabsHost.getCurrentPageTableId());

        if(dB.getNotesCount(true) ==  0)
        {
        	finish(); // add for last note being deleted
        	return;
        }
        
        setContentView(R.layout.note_edit);
        setTitle(R.string.edit_note_title);// set title
    	
        System.out.println("Note_edit / onCreate");
        
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(this)));

    	Bundle extras = getIntent().getExtras();
    	position = extras.getInt("list_view_position");
    	noteId = extras.getLong(DB_page.KEY_NOTE_ID);
    	audioUri = extras.getString(DB_page.KEY_NOTE_AUDIO_URI);
    	title = extras.getString(DB_page.KEY_NOTE_TITLE);
    	body = extras.getString(DB_page.KEY_NOTE_BODY);


        //initialization
        note_edit_ui = new Note_edit_ui(this, dB, noteId, title,  audioUri,  body);
        note_edit_ui.UI_init();

        if(savedInstanceState != null)
        {
	        System.out.println("Note_edit / onCreate / noteId =  " + noteId);
	        if(noteId != null)
	        {
	        	audioUri = dB.getNoteAudioUri_byId(noteId);
				note_edit_ui.currAudioUri = audioUri;
	        }
        }
        
    	// show view
		note_edit_ui.populateFields_all(noteId);
		
		// OK button: edit OK, save
        Button okButton = (Button) findViewById(R.id.note_edit_ok);
//        okButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
				if(note_edit_ui.bRemoveAudioUri)
				{
					audioUri = "";
				}	
				System.out.println("Note_edit / onClick (okButton) / noteId = " + noteId);
                enSaveDb = true;
                finish();
            }

        });
        
        // delete button: delete note
        Button delButton = (Button) findViewById(R.id.note_edit_delete);
//        delButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        // delete
        delButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view)
			{
				Util util = new Util(Note_edit.this);
				util.vibrate();

				Builder builder1 = new Builder(Note_edit.this );
				builder1.setTitle(R.string.confirm_dialog_title)
					.setMessage(R.string.confirm_dialog_message)
					.setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener()
						{   @Override
							public void onClick(DialogInterface dialog1, int which1)
							{/*nothing to do*/}
						})
					.setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener()
						{   @Override
							public void onClick(DialogInterface dialog1, int which1)
							{
								note_edit_ui.deleteNote(noteId);


								if(Audio7Player.isOnAudioPlayingPage())
									Audio7Player.prepareAudioInfo();

								// Stop Play/Pause if current edit item is played and is not at Stop state
								if(Page.mHighlightPosition == position)
									UtilAudio.stopAudioIfNeeded();

								// update highlight position
								if(position < Page.mHighlightPosition )
									Audio_manager.mAudioPos--;

								finish();
							}
						})
					.show();//warning:end
            }
        });
        
        // cancel button: leave, do not save current modification
        Button cancelButton = (Button) findViewById(R.id.note_edit_cancel);
//        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                
                // check if note content is modified
               	if(note_edit_ui.isNoteModified())
            	{
               		// show confirmation dialog
            		confirmToUpdateDlg();
            	}
            	else
            	{
            		enSaveDb = false;
                    finish();
            	}
            }
        });
    }
    
    // confirm to update change or not
    void confirmToUpdateDlg()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(Note_edit.this);
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.edit_note_confirm_update)
	           // Yes, to update
			   .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						if(note_edit_ui.bRemoveAudioUri)
						{
							audioUri = "";
						}						
					    enSaveDb = true;
					    finish();
					}})
			   // cancel
			   .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{   // do nothing
					}})
			   // no, roll back to original status		
			   .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Bundle extras = getIntent().getExtras();

						String originalAudioFileName = extras.getString(DB_page.KEY_NOTE_AUDIO_URI);

						if(Util.isEmptyString(originalAudioFileName))
						{   // no picture at first
							note_edit_ui.removeAudioStringFromOriginalNote(noteId);
		                    enSaveDb = false;
						}
						else
						{	// roll back existing picture
                            note_edit_ui.bRollBackData = true;
							audioUri = originalAudioFileName;
							enSaveDb = true;
						}	
	                    finish();
					}})
			   .show();
    }
    

    // for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
        
        System.out.println("Note_edit / onPause / enSaveDb = " + enSaveDb);
        System.out.println("Note_edit / onPause / audioUri = " + audioUri);
        noteId = note_edit_ui.saveStateInDB(noteId, enSaveDb,  audioUri);
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        System.out.println("Note_edit / onSaveInstanceState / enSaveDb = " + enSaveDb);
        noteId = note_edit_ui.saveStateInDB(noteId, enSaveDb,  audioUri);
        outState.putSerializable(DB_page.KEY_NOTE_ID, noteId);
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public void onBackPressed() {
        if(note_edit_ui.isNoteModified())
        {
            confirmToUpdateDlg();
        }
        else
        {
            enSaveDb = false;
            finish();
        }
    }
    
    static final int CHANGE_AUDIO = R.id.ADD_AUDIO;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate menu
		getMenuInflater().inflate(R.menu.edit_note_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
    
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
        {
		    case android.R.id.home:
		    	if(note_edit_ui.isNoteModified())
		    	{
		    		confirmToUpdateDlg();
		    	}
		    	else
		    	{
		            enSaveDb = false;
		            finish();
		    	}
		        return true;

            case CHANGE_AUDIO:
				note_edit_ui.bRemoveAudioUri = false; // reset
            	setAudioSource();
			    return true;
			    
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    void setAudioSource() 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_note_set_audio_dlg_title);
		// Cancel
		builder.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
		   	   {
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{// cancel
				}});
		// Set
		builder.setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) 
		{
		    enSaveDb = true;
	        startActivityForResult(Util.chooseMediaIntentByType(Note_edit.this,"audio/*"),
	        					   Util.CHOOSER_SET_AUDIO);
		}});

		// None
		if(!Util.isEmptyString(audioUri))
		{
			builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						note_edit_ui.bRemoveAudioUri = true;
						note_edit_ui.oriAudioUri = "";
						audioUri = "";
						note_edit_ui.removeAudioStringFromCurrentEditNote(noteId);
						note_edit_ui.populateFields_all(noteId);
					}});		
		}
		
		Dialog dialog = builder.create();
		dialog.show();
    }
    
//    static String selectedAudioUri;
	protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) 
	{
        // choose audio
		if(requestCode == Util.CHOOSER_SET_AUDIO)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				// for audio
				Uri audioUri = returnedIntent.getData();

				// SAF support, take persistent Uri permission
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				{
					int takeFlags = returnedIntent.getFlags()
							& (Intent.FLAG_GRANT_READ_URI_PERMISSION
							| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

					// add for solving inspection error
					takeFlags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;

					//fix: no permission grant found for UID 10070 and Uri content://media/external/file/28
					String authority = audioUri.getAuthority();
					if(authority.equalsIgnoreCase("com.google.android.apps.docs.storage"))
					{
						getContentResolver().takePersistableUriPermission(audioUri, takeFlags);
					}
				}

				String scheme = audioUri.getScheme();
				String audioUriStr = audioUri.toString();

				// get real path
				if(	(scheme.equalsIgnoreCase("file") ||
					 scheme.equalsIgnoreCase("content") ) ) {

					// check if content scheme points to local file
					if (scheme.equalsIgnoreCase("content")) {
						String realPath = Util.getLocalRealPathByUri(this, audioUri);

						if (realPath != null)
							audioUriStr = "file://".concat(realPath);
					}
				}

//				System.out.println(" Note_edit / onActivityResult / Util.CHOOSER_SET_AUDIO / picUriStr = " + picUriStr);
				note_edit_ui.saveStateInDB(noteId,true,  audioUriStr);

				note_edit_ui.populateFields_all(noteId);
	        	this.audioUri = audioUriStr;
	    			
	        	showSavedFileToast(audioUriStr);
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				Toast.makeText(Note_edit.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
	            setResult(RESULT_CANCELED, getIntent());
	            return; // must add this
			}
		}
		
	}
	
	// show audio file name
	void showSavedFileToast(String audioUri)
	{
        String audioName = Util.getDisplayNameByUriString(audioUri, Note_edit.this)[0];
		Toast.makeText(Note_edit.this,
						audioName,
						Toast.LENGTH_SHORT)
						.show();
	}
	
}
