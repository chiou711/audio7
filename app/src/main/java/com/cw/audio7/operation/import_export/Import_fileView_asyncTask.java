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

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2017/9/16.
 */
// Show progress progressBar
class Import_fileView_asyncTask extends AsyncTask<Void, Integer, Void> {

    private ProgressBar progressBar;
    private boolean enableSaveDB;
    private AppCompatActivity act;
    private View rootView;
    private TextView titleViewText;
    private TextView bodyViewText;
    private View file_view;
    private File file;

    Import_fileView_asyncTask(AppCompatActivity _act, View _rootView, String _filePath)
    {
        act = _act;
        rootView = _rootView;

        Util.lockOrientation(act);

        titleViewText = (TextView) rootView.findViewById(R.id.view_title);
        bodyViewText = (TextView) rootView.findViewById(R.id.view_body);

        file_view = rootView.findViewById(R.id.view_file);
        file_view.setVisibility(View.GONE);

        progressBar = (ProgressBar) rootView.findViewById(R.id.import_progress);
        progressBar.setVisibility(View.VISIBLE);

        file = new File(_filePath);
    }

    void enableSaveDB(boolean enable)
    {
        enableSaveDB = enable;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (this.progressBar != null ){
            progressBar.setProgress(values[0]);
        }
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        insertSelectedFileContentToDB(enableSaveDB);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        progressBar.setVisibility(View.GONE);
        file_view.setVisibility(View.VISIBLE);

        if(enableSaveDB)
        {
//            backToListFragment();
            act.getSupportFragmentManager().popBackStack();

            View view1 = act.findViewById(R.id.view_back_btn_bg);
            view1.setVisibility(View.VISIBLE);
            View view2 = act.findViewById(R.id.file_list_title);
            view2.setVisibility(View.VISIBLE);

//				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            Util.unlockOrientation(act);
            Toast.makeText(act, R.string.toast_import_finished,Toast.LENGTH_SHORT).show();
        }
        else
        {
            // show Import content
            titleViewText.setText(file.getName());
            bodyViewText.setText(importObject.fileBody);
        }
    }

    ParseXmlToDB importObject;
    private void insertSelectedFileContentToDB(boolean enableInsertDB)
    {
        FileInputStream fileInputStream = null;
        try
        {
            fileInputStream = new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        // import data by HandleXmlByFile class
        if(fileInputStream != null) {
            importObject = new ParseXmlToDB(fileInputStream, act);
            importObject.enableInsertDB(enableInsertDB);
            importObject.handleXML();
            while (ParseXmlToDB.isParsing)
            {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }
        }
    }
}

