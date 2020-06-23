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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by CW on 2016/7/21.
 */
public class MailNotes {

    String mDefaultEmailAddr;
    SharedPreferences mPref_email;
    EditText editEMailAddrText;
    String mEMailBodyString;
    AlertDialog mDialog;
    Activity mAct;
    String mSentString;
    String[] mPicFileNameArray;

    public MailNotes(final Activity act, String sentString, String[] picFileArray)
    {
        mAct = act;
        mSentString = sentString;
        mPicFileNameArray = picFileArray;

        AlertDialog.Builder builder1;
        mPref_email = act.getSharedPreferences("email_addr", 0);
        editEMailAddrText = (EditText)act.getLayoutInflater()
                .inflate(R.layout.edit_text_dlg, null);
        builder1 = new AlertDialog.Builder(act);

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
                if (keyCode == KeyEvent.KEYCODE_BACK)
                {
                    mDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    //for keeping dialog when eMail address is empty
    class CustomListener implements View.OnClickListener
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
                // save to SD card
                attachmentFileName[0] = Util.getStorageDirName(mAct) + "_SEND_" + // file name
                                     Util.getCurrentTimeString() + // time
                                     ".xml"; // extension name

                attachmentFileName[1] = Util.getStorageDirName(mAct) + "_SEND_" + // file name
                        Util.getCurrentTimeString() + // time
                        ".txt"; // extension name


                Util util = new Util(mAct);

                // XML file
                util.exportToSdCardFile(attachmentFileName[0], // attachment name
                                        mSentString); // sent string

                mEMailBodyString = util.trimXMLtag(mSentString);

                // TXT file
                util.exportToSdCardFile(attachmentFileName[1], // attachment name
                                        mEMailBodyString); // sent string


                mPref_email.edit().putString("KEY_DEFAULT_EMAIL_ADDR", strEMailAddr).apply();

                // call next dialog
                sendEMail(strEMailAddr,  // eMail address
                          attachmentFileName, // attachment file name
                          mPicFileNameArray); // picture file name array. For page selection, this is null
                dialog.dismiss();
            }
            else
            {
                Toast.makeText(mAct,
                        R.string.toast_no_email_address,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Send e-Mail : send file by e-Mail
    public static String[] mAttachmentFileName;
    public final static int EMAIL = 101;
    void sendEMail(String strEMailAddr,  // eMail address
                   String[] attachmentFileName, // attachment name
                   String[] picFileNameArray) // attachment picture file name
    {
        mAttachmentFileName = attachmentFileName;
        // new ACTION_SEND intent
        Intent mEMailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE); // for multiple attachments

        // set type
        mEMailIntent.setType("text/plain");//can select which APP will be used to send mail

        // open issue: cause warning for Key android.intent.extra.TEXT expected ArrayList
        String text_body = mAct.getResources().getString(R.string.eMail_body)// eMail text (body)
                + " " + Util.getStorageDirName(mAct) + " (UTF-8)" + Util.NEW_LINE
                + mEMailBodyString;

        // attachment: message
        List<String> filePaths = new ArrayList<String>();

        for(int i=0;i<attachmentFileName.length;i++) {
            String messagePath = "file:///" + Environment.getExternalStorageDirectory().getPath() +
                    "/" + Util.getStorageDirName(mAct) + "/" +
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
                              Util.getStorageDirName(mAct) + // eMail subject
                                " " + mAct.getResources().getString(R.string.eMail_subject ))// eMail subject
                    .putExtra(Intent.EXTRA_TEXT,text_body) // eMail body (open issue)
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); // multiple eMail attachment

        Log.v(getClass().getSimpleName(),
                "attachment " + Uri.parse("file name is:"+ attachmentFileName));

        mAct.startActivityForResult(Intent.createChooser(mEMailIntent,
                mAct.getResources().getText(R.string.mail_chooser_title)) ,
                EMAIL);
    }

}
