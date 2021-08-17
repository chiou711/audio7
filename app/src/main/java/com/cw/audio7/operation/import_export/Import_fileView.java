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

package com.cw.audio7.operation.import_export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.cw.audio7.R;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class Import_fileView extends Fragment
{
    TextView mTitleViewText;
    TextView mBodyViewText;
    String filePath;
    File mFile;
    View rootView;
	AppCompatActivity act;

	public Import_fileView() {
	}

	public Import_fileView(AppCompatActivity _act) {
		act = _act;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.import_sd_file_view,container, false);
		System.out.println("Import_fileView / onCreate");

		mTitleViewText = (TextView) rootView.findViewById(R.id.view_title);
		mBodyViewText = (TextView) rootView.findViewById(R.id.view_body);

        Import_fileView_asyncTask task = null;
		if(savedInstanceState == null) {
			task = new Import_fileView_asyncTask(act,rootView,filePath);
			task.enableSaveDB(false);// view
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			mFile = new File(filePath);
			mTitleViewText.setText(mFile.getName());
			mBodyViewText.setText(task.importObject.fileBody);
		}

		int style = 2;
		//set title color
		mTitleViewText.setTextColor(ColorSet.mText_ColorArray[style]);
		mTitleViewText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		//set body color
		mBodyViewText.setTextColor(ColorSet.mText_ColorArray[style]);
		mBodyViewText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		// back button
		Button backButton = (Button) rootView.findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

		// import button
		Button confirmButton = (Button) rootView.findViewById(R.id.view_confirm);
		confirmButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_revert, 0, 0, 0);
		confirmButton.setText(R.string.config_import);

		// delete button
		Button deleteButton = (Button) rootView.findViewById(R.id.view_delete);
		deleteButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete , 0, 0, 0);

		// do cancel
		backButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
                backToListFragment();
			}
		});

		// delete the file whose content is showing
		deleteButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view)
			{
				Util util = new Util(getActivity());
				util.vibrate();

                mFile = new File(filePath);
				String fileName = mFile.getName();

				AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
				builder1.setTitle(R.string.confirm_dialog_title)
						.setMessage(getResources().getString(R.string.confirm_dialog_message_file) +
								" (" + fileName +")" )
						.setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog1, int which1) {/*nothing to do*/}
						})
						.setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog1, int which1)
							{
								mFile.delete();

                                // refresh directory list
                                backToListFragment();
								String dirString = new File(filePath).getParent();
								File dir = new File(dirString);
                                Import_filesList fragment = ((Import_filesList)getActivity().getSupportFragmentManager().findFragmentByTag("import"));
                                fragment.getFiles(dir.listFiles());
							}
						})
						.show();
			}
		});

		// confirm to import view to DB
		confirmButton.setOnClickListener(new View.OnClickListener()
		{

			public void onClick(View view)
			{
				Import_fileView_asyncTask task = new Import_fileView_asyncTask(act,rootView,filePath);
				task.enableSaveDB(true);//confirm
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});


		return rootView;
	}

	void backToListFragment()
    {
        getActivity().getSupportFragmentManager().popBackStack();
        View view1 = getActivity().findViewById(R.id.view_back_btn_bg);
        view1.setVisibility(View.VISIBLE);
        View view2 = getActivity().findViewById(R.id.file_list_title);
        view2.setVisibility(View.VISIBLE);
    }

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Bundle arguments = getArguments();
        filePath = arguments.getString("KEY_FILE_PATH");
    }

    // Import default content by XML file
    public static void importDefaultContentByXml(AppCompatActivity act, File xmlFile)
    {
		System.out.println("Import_fileView / _importDefaultContentByXml / xmlFileName = " + xmlFile.getName());

	    TabsHost.setLastPageTableId(0);

        FileInputStream fileInputStream = null;

        try
        {
            fileInputStream = new FileInputStream(xmlFile);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        // import data by HandleXmlByFile class
		ParseXmlToDB importObject = new ParseXmlToDB(fileInputStream,act);
        importObject.enableInsertDB(true);
        importObject.handleXML();
        while(ParseXmlToDB.isParsing);
	    {
		    try { Thread.sleep(100); } catch (InterruptedException e) {}
	    }
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}