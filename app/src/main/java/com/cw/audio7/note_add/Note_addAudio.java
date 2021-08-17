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

package com.cw.audio7.note_add;

import java.io.File;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.page.Page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import static com.cw.audio7.main.MainAct.audio_manager;

public class Note_addAudio extends AppCompatActivity {

    Long noteId;
    String selectedAudioUri;
    boolean enSaveDb = true;
	String audioUriInDB;
	private DB_page dB;
    boolean bUseSelectedFile;
    TextView progress;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.note_add_prepare);
        System.out.println("Note_addAudio / onCreate");

        audioUriInDB = "";
        selectedAudioUri = "";
        bUseSelectedFile = false;

        // get row Id from saved instance
        noteId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB_page.KEY_NOTE_ID);

        // get audio Uri in DB if instance is not null
		dB = new DB_page(this, TabsHost.getCurrentPageTableId());
        if(savedInstanceState != null)
        {
	        System.out.println("Note_addAudio / noteId =  " + noteId);
	        if(noteId != null)
	        	audioUriInDB = dB.getNoteAudioUri_byId(noteId);
        }

        // at the first beginning
        if(savedInstanceState == null)
        	chooseAudioMedia();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    }

    // for Rotate screen
    @Override
    protected void onPause() {
    	System.out.println("Note_addAudio / onPause");
        super.onPause();
    }

    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addNew / onSaveInstanceState");
        outState.putSerializable(DB_page.KEY_NOTE_ID, noteId);
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        enSaveDb = false;
        finish();
    }

	public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
	{
		super.onActivityResult(requestCode,resultCode,imageReturnedIntent);
//		System.out.println("Note_addAudio / onActivityResult");
		if (resultCode == Activity.RESULT_OK)
		{
            setContentView(R.layout.note_add_prepare);
            progress = findViewById(R.id.add_audio_progress);//must add this, otherwise text view is not updated
            dB = new DB_page(this, TabsHost.getCurrentPageTableId());

			// for audio
			if(requestCode == Util.CHOOSER_SET_AUDIO)
			{
				Uri selectedUri = imageReturnedIntent.getData();
				System.out.println("Note_addAudio / onActivityResult / selectedUri = " + selectedUri);

                int takeFlags= -1;
				// SAF support, take persistent Uri permission
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				{
			    	takeFlags = imageReturnedIntent.getFlags()
			                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
			                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

					// add for solving inspection error
					takeFlags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;

			    	String authority = selectedUri.getAuthority();
			    	if(authority.equalsIgnoreCase("com.google.android.apps.docs.storage"))
			    	{
			    		getContentResolver().takePersistableUriPermission(selectedUri, takeFlags);
			    	}
				}

				String scheme = selectedUri.getScheme();
				// check option of Add new audio
				String option = getIntent().getExtras().getString("EXTRA_ADD_EXIST", "single_to_bottom");

				// add single file
				if((option.equalsIgnoreCase("single_to_top") ||
           		    option.equalsIgnoreCase("single_to_bottom") ) &&
           		   (scheme.equalsIgnoreCase("file") ||
 					scheme.equalsIgnoreCase("content"))              )
				{
					String uriStr = selectedUri.toString();

					// check if content scheme points to local file
					if(scheme.equalsIgnoreCase("content"))
					{
						String realPath = Util.getLocalRealPathByUri(this, selectedUri);

						if(realPath != null)
							uriStr = "file://".concat(realPath);
					}

		  		    noteId = null; // set null for Insert
//		        	noteId = note_common.insertAudioToDB(uriStr);

					if( !Util.isEmptyString(uriStr))
					{
						// insert
						// set marking to 1 for default
						dB.insertNote("",  uriStr, "",  1);// add new note, get return row Id
					}

		        	selectedAudioUri = uriStr;

					if( (dB.getNotesCount(true) > 0) &&
		        		option.equalsIgnoreCase("single_to_top"))
		        	{
		        		Page.swapTopBottom(this);
		        		//update playing focus
						audio_manager.mAudioPos++;
		        	}

		        	if(!Util.isEmptyString(uriStr))
		        	{
		                String[] audioName = Util.getDisplayNameByUriString(uriStr, Note_addAudio.this);
		        		Util.showSavedFileToast(this,audioName[0].concat(" / ").concat(audioName[1]),500);
		        	}
				}
				// add multiple audio files in the selected file's directory
				else if((option.equalsIgnoreCase("directory_to_top") ||
						 option.equalsIgnoreCase("directory_to_bottom")) &&
						 (scheme.equalsIgnoreCase("file") ||
						  scheme.equalsIgnoreCase("content") )              )
				{
					// get file path and add prefix (file://)
					String realPath = Util.getLocalRealPathByUri(this, selectedUri);

					// when scheme is content, it could be local or remote
					if(realPath != null)
					{
						// get file name
						File file = new File("file://".concat(realPath));
						String fileName = file.getName();

						// get directory
						String dirStr = realPath.replace(fileName, "");
						File dir = new File(dirStr);

						// get Urls array
						String[] urlsArray = Util.getUrlsByFiles(dir.listFiles(), Util.AUDIO);
						if(urlsArray == null)
						{
							Toast.makeText(this,"No file is found",Toast.LENGTH_SHORT).show();
							finish();
						}
						else
						{
							// show Start
							Toast.makeText(this, R.string.add_new_start, Toast.LENGTH_SHORT).show();
						}

                        int i= 1;
                        int total=0;

                        for(int cnt = 0; cnt < urlsArray.length; cnt++)
                        {
                            if(!Util.isEmptyString(urlsArray[cnt]))
                                total++;
                        }

                        // note: the order add insert items depends on file manager
                        for(String urlStr:urlsArray)
                        {
//                            System.out.println("urlStr = " + urlStr);
                            noteId = null; // set null for Insert
                            if(!Util.isEmptyString(urlStr))
                            {
                                // insert
                                // set marking to 1 for default
                                dB.insertNote("",  urlStr, "",  1);// add new note, get return row Id
                            }
                            selectedAudioUri = urlStr;

                            if( (dB.getNotesCount(true) > 0) &&
                                    option.equalsIgnoreCase("directory_to_top") )
                            {
                                Page.swapTopBottom(this);
                                //update playing focus
                                audio_manager.mAudioPos++;
                            }

                            // avoid showing empty toast
                            if(!Util.isEmptyString(urlStr))
                            {
                                String audioName = Util.getDisplayNameByUriString(urlStr, Note_addAudio.this)[0];
                                audioName = i + "/" + total + ": " + audioName;
                                progress.append("\r\n"+audioName);
                            }
                            i++;
                        }

                        // show Stop
                        Toast.makeText(Note_addAudio.this,R.string.add_new_stop,Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(this,
								R.string.add_new_file_error,
								Toast.LENGTH_LONG)
								.show();
					}
				}

				// open chooser again
//	        	chooseAudioMedia();

	        	// to avoid exception due to playing tab is different with focus tab
				if(audio_manager.isOnAudioPlayingPage())
					audio_manager.setupAudioList(this);

	        	finish();
			}
		}
		else if (resultCode == RESULT_CANCELED)
		{
			Toast.makeText(Note_addAudio.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED, getIntent());
            finish();
            return; // must add this
		}
	}


    void chooseAudioMedia()
    {
	    enSaveDb = true;
        startActivityForResult(Util.chooseMediaIntentByType(Note_addAudio.this,"audio/*"),
        					   Util.CHOOSER_SET_AUDIO);        
    }	
	

}