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

package com.cw.audio7.operation.mail;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.main.MainAct;
import com.cw.audio7.R;
import com.cw.audio7.operation.List_selectPage;
import com.cw.audio7.util.BaseBackPressedListener;
import com.cw.audio7.util.Util;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class MailPagesFragment extends Fragment {
	Context mContext;
	TextView title;
	Intent mEMailIntent;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	List_selectPage list_selPage;
	public static View rootView;

	public MailPagesFragment(){}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.select_page_list, container, false);
//		getActivity().getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(getActivity())));

		// title
		title = (TextView) rootView.findViewById(R.id.select_list_title);
		title.setText(R.string.config_select_pages_mail_title);

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    list_selPage.selectAllPages(true);
                else
                    list_selPage.selectAllPages(false);
            }
        });

        // list view: selecting which pages to send
        mListView = (ListView)rootView.findViewById(R.id.listView1);

        // OK button: click to do next
        btnSelPageOK = (Button) rootView.findViewById(R.id.btnSelPageOK);
		btnSelPageOK.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_send, 0, 0, 0);
		btnSelPageOK.setText(R.string.mail_notes_btn);
        btnSelPageOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // input mail address: dialog
                System.out.println("OK");
                if(list_selPage.mChkNum > 0)
                {
                    inputEMailDialog(); // call next dialog
                }
                else
                    Toast.makeText(getActivity(),
                            R.string.delete_checked_no_checked_items,
                            Toast.LENGTH_SHORT).show();
            }
        });

        // cancel button
        Button btnSelPageCancel = (Button) rootView.findViewById(R.id.btnSelPageCancel);
		btnSelPageCancel.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        btnSelPageCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("SendMailFragment / Cancel button");
                getActivity().getSupportFragmentManager().popBackStack();
                mCheckTvSelAll.setVisibility(View.INVISIBLE);
            }
        });

        //Send e-Mail 1: show list for selection
        list_selPage = new List_selectPage(getActivity(),rootView , mListView);

		((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(MainAct.mAct));

		return rootView;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = getActivity();
	}

	// Send e-Mail 2
	// case A: input mail address from current activity
	// case B: input mail address from ViewNote activity
    String mDefaultEmailAddr;
    SharedPreferences mPref_email;
	EditText editEMailAddrText;
	String mEMailBodyString;
	AlertDialog mDialog;

	void inputEMailDialog()
	{
		AlertDialog.Builder builder1;

		mPref_email = getActivity().getSharedPreferences("email_addr", 0);
	    editEMailAddrText = (EditText)getActivity().getLayoutInflater()
	    							.inflate(R.layout.edit_text_dlg, null);
		builder1 = new AlertDialog.Builder(getActivity());

		// get default email address
		mDefaultEmailAddr = mPref_email.getString("KEY_DEFAULT_EMAIL_ADDR","@");
		editEMailAddrText.setText(mDefaultEmailAddr);

		builder1.setTitle(R.string.mail_notes_dlg_title)
				.setMessage(R.string.mail_notes_dlg_message)
				.setView(editEMailAddrText)
				.setNegativeButton(R.string.edit_note_button_back,
						new DialogInterface.OnClickListener()
				{   @Override
					public void onClick(DialogInterface dialog, int which)
					{/*cancel*/
                        dialog.dismiss();
                    }

				})
				.setPositiveButton(R.string.mail_notes_btn, null); //call override

		mDialog = builder1.create();
		mDialog.show();

		// override positive button
		Button enterButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		enterButton.setOnClickListener(new CustomListener(mDialog));


		// back
		mDialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
	}

	//for keeping dialog when eMail address is empty
	class CustomListener implements OnClickListener
	{
		private final Dialog dialog;
	    public CustomListener(Dialog dialog){
	    	this.dialog = dialog;
	    }

	    @Override
	    public void onClick(View v){
	    	String[] attachmentFileName={"",""};
	        String strEMailAddr = editEMailAddrText.getText().toString();
	        if(strEMailAddr.length() > 0)
	        {
	    	    Bundle extras = getActivity().getIntent().getExtras();

				// pages name
				String pagesName="";
				int countChecked=0;
				for(int i = 0; i< list_selPage.mListStrArr.size(); i++) {
					if(list_selPage.mCheckedTabs.get(i))
					{
						countChecked++;
						if(Util.isEmptyString(pagesName))
							pagesName = list_selPage.mListStrArr.get(i);//first title
						else
							pagesName = pagesName.concat("_" + list_selPage.mListStrArr.get(i) );
					}
				}

				// default file name: with tab title
				String defaultFileName;
				if((list_selPage.isCheckAll) ||
				   (list_selPage.mListStrArr.size() == countChecked) )
					defaultFileName = list_selPage.mFolderTitle + ".xml";
				else
					defaultFileName = pagesName + ".xml";

				// save to SD card
				attachmentFileName[0] = Util.getStorageDirName(getActivity()) + "_SEND_" + // file name
										defaultFileName +
										Util.getCurrentTimeString() + // time
										".xml"; // extension name

				attachmentFileName[1] = Util.getStorageDirName(getActivity()) + "_SEND_" + // file name
										defaultFileName +
										Util.getCurrentTimeString() + // time
										".txt"; // extension name

				Util util = new Util(getActivity());

				// for page selection
				String[] picFileNameArr = null;
		        if(extras == null)
		        {
		            // XML file
                    mEMailBodyString = util.exportToSdCard(attachmentFileName[0], // attachment name
                                                           list_selPage.mCheckedTabs);// checked page array
                    mEMailBodyString = util.trimXMLtag(mEMailBodyString);

                    // TXT file
                    util.exportToSdCardFile(attachmentFileName[1], // attachment name
                                            mEMailBodyString); // sent string
	        	}

	        	mPref_email.edit().putString("KEY_DEFAULT_EMAIL_ADDR", strEMailAddr).apply();

	        	// call next dialog
				sendEMail(strEMailAddr,  // eMail address
					      attachmentFileName, // attachment file name
						  picFileNameArr ); // picture file name array. For page selection, this is null
				dialog.dismiss();
	        }
	        else
	        {
    			Toast.makeText(getActivity(),
						R.string.toast_no_email_address,
						Toast.LENGTH_SHORT).show();
	        }
	    }
	}
	
	// Send e-Mail : send file by e-Mail
    public final static int EMAIL_PAGES = 102;
	public static String[] mAttachmentFileName;
	void sendEMail(String strEMailAddr,  // eMail address
			       String[] attachmentFileName, // attachment name
			       String[] picFileNameArray) // attachment picture file name
	{
		mAttachmentFileName = attachmentFileName;
		// new ACTION_SEND intent
		mEMailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE); // for multiple attachments

		// set type
		mEMailIntent.setType("text/plain");//can select which APP will be used to send mail

    	// open issue: cause warning for Key android.intent.extra.TEXT expected ArrayList
    	String text_body = mContext.getResources().getString(R.string.eMail_body)// eMail text (body)
			 	 			+ " " + Util.getStorageDirName(mContext) + " (UTF-8)" + Util.NEW_LINE
			 	 			+ mEMailBodyString;
    	
    	// attachment: message
		List<String> filePaths = new ArrayList<String>();
		for(int i=0;i<attachmentFileName.length;i++) {
			String messagePath = "file:///" + Environment.getExternalStorageDirectory().getPath() +
					"/" + Util.getStorageDirName(mContext) + "/" +
					attachmentFileName[i];// message file name
			filePaths.add(messagePath);
		}
    	
    	// attachment: pictures
    	if(picFileNameArray != null)
    	{
	    	for(int i=0;i<picFileNameArray.length;i++)
	    	{
	        	filePaths.add(picFileNameArray[i]);
	    	}
    	}
    	
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (String file : filePaths)
        {
            Uri uri = Uri.parse(file);
            uris.add(uri);
        }
    	
    	mEMailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{strEMailAddr}) // eMail address
	    			.putExtra(Intent.EXTRA_SUBJECT,
	    			          Util.getStorageDirName(mContext) + // eMail subject
	    					  " " + mContext.getResources().getString(R.string.eMail_subject ))// eMail subject
	    			.putExtra(Intent.EXTRA_TEXT,text_body) // eMail body (open issue)
	    			.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); // multiple eMail attachment
    	
	    Log.v(getClass().getSimpleName(),
			  "attachment " + Uri.parse("file name is:"+ attachmentFileName));
	    
	    getActivity().startActivityForResult(Intent.createChooser(mEMailIntent,
	    											getResources().getText(R.string.mail_chooser_title)) ,
                                             EMAIL_PAGES);
	} 
	
	@Override
	public void onPause() {
		super.onPause();

		if( null != mDialog)
			mDialog.dismiss();//fix leaked window
	}

}