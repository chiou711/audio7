/*
 * Copyright (C) 2020 CW Chiu
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

package com.cw.audio7.note_add.add_audio;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;

import static com.cw.audio7.main.MainAct.mFolderUi;

public class Add_audio_byFolder extends ListFragment
{
    private List<String> filePathArray = null;
    List<String> fileNames = null;
    public View rootView;
    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.import_sd_files_list, container, false);

        View view = rootView.findViewById(R.id.view_back_btn_bg);
        view.setBackgroundColor(ColorSet.getBarColor(getActivity()));

        // back button
        Button backButton = (Button) rootView.findViewById(R.id.view_back);
        backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

        // update button
        Button renewButton = (Button) rootView.findViewById(R.id.view_renew);
        renewButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_popup_sync , 0, 0, 0);

        // do cancel
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // do update
        renewButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // source dir: Download
                String srcDirName = "Download";//todo Could be empty
                String srcDirPath = Environment.getExternalStorageDirectory().toString() +
                        "/" +
                        srcDirName;
                System.out.println("srcDirPath = " + srcDirPath);

                /**
                 * Note about getExternalStorageDirectory:
                 * don't be confused by the word "external" here.
                 * This directory can better be thought as media/shared storage.
                 * It is a filesystem that can hold a relatively large amount of data and
                 * that is shared across all applications (does not enforce permissions).
                 * Traditionally this is an SD card, but it may also be implemented as built-in storage in a device
                 * that is distinct from the protected internal storage and can be mounted as a filesystem on a computer.
                 */
                // target dir
                String targetDirPath = Environment.getExternalStorageDirectory().toString() +
                        "/" +
                        Util.getStorageDirName(getActivity());

                // copy source files to target directory
                File srcDir = new File(srcDirPath);

                if(srcDir.exists()) {
                    for (File srcFile : srcDir.listFiles()) {
                        File targetFile = new File(targetDirPath + "/" + srcFile.getName());
                        System.out.println("targetFile.getName() = " + targetFile.getName());
                        try {
                            if (UtilAudio.hasAudioExtension(srcFile))
                                FileUtils.copyFile(srcFile, targetFile);
                        } catch (IOException e) {

                            e.printStackTrace();
                        }
                    }
                }

                // refresh list view
                File dir = new File(targetDirPath);
                showFilesList(dir.listFiles());
            }
        });

//        ((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(MainAct.mAct));

        return rootView;
    }

    @Override
    public void onCreate(Bundle bundle) 
    {
        super.onCreate(bundle);
    }

    String appDir;

    @Override
    public void onResume() {
        super.onResume();
        listView = getListView();
        appDir = Environment.getExternalStorageDirectory().toString() +
                                    "/" +
                                    Util.getStorageDirName(getActivity());
        currFilePath = appDir;

        File dir = new File(appDir);
        if(!dir.exists())
            dir.mkdir();

        showFilesList(new File(appDir).listFiles());
    }

    int selectedRow;
    String currFilePath;

    // on list item click
    public void onListItemClick(long rowId)
    {
        AppCompatActivity act = (AppCompatActivity) getActivity();

        System.out.println("--- onListItemClick / currFilePath = " + currFilePath);

        selectedRow = (int)rowId;
        System.out.println("--- onListItemClick / selectedRow = " + selectedRow);
        if(selectedRow == 0)
        {
            System.out.println("--- onListItemClick / selectedRow = 0 / currFilePath = " + currFilePath);

            if(currFilePath.equals("/storage")) {
                Toast.makeText(act,R.string.toast_storage_directory_top,Toast.LENGTH_SHORT).show();
                showFilesList(new File(currFilePath).listFiles());
                return;
            }

            File parentDir = null;
            String parentPath;
            do {
                File currDir = new File(currFilePath);
                parentPath = currDir.getParent();
                System.out.println("--- onListItemClick / selectedRow = 0 / parentPath = " + parentPath);

                if (parentPath != null) {
                    parentDir = new File(parentPath);
                    currFilePath = parentPath;
                    System.out.println("--- onListItemClick / selectedRow = 0 / new currFilePath = " + currFilePath);
                }
            } while (parentDir.listFiles() == null);

            showFilesList(parentDir.listFiles());
        }
        else
        {
//            System.out.println("Add_audio_byFolder / _onListItemClick / is dir");
            currFilePath = filePathArray.get(selectedRow);
            System.out.println("Add_audio_byFolder / _onListItemClick / currFilePath = " + currFilePath);

            //TODO 20201122 temp for open /storage/emulated/0
            if(currFilePath.equals("/storage/emulated"))
                currFilePath = currFilePath.concat("/0");

            final File file = new File(currFilePath);
            if(file.isDirectory())
            {
                int dirCount = 0;
                int filesCount = 0;

                if(file.listFiles() != null) {
                    dirCount = showFilesList(file.listFiles());
                    filesCount = file.listFiles().length;
                }

                System.out.println( "=> dirCount = " + dirCount);
                System.out.println( "=> filesCount = " + filesCount);

                // check if audio files exist
                if( (dirCount ==0 ) && (filesCount>0) ) {
                    // get current Max page table Id
                    int currentMaxPageTableId = 0;
                    int pagesCount = mFolderUi.getFolder_pagesCount(act, mFolderUi.getFocus_folderPos());
                    DB_folder db_folder = new DB_folder(act, DB_folder.getFocusFolder_tableId());

                    for (int i = 0; i < pagesCount; i++) {
                        int id = db_folder.getPageTableId(i, true);
                        if (id > currentMaxPageTableId)
                            currentMaxPageTableId = id;
                    }
                    currentMaxPageTableId++;

                    int newPageTableId = currentMaxPageTableId;

                    // get page name
                    String pageName = file.getName();

                    // insert page name
                    int style = Util.getNewPageStyle(act);
                    db_folder.insertPage(DB_folder.getFocusFolder_tableName(), pageName, newPageTableId, style, true);

                    // insert table for new page
                    db_folder.insertPageTable(db_folder, DB_folder.getFocusFolder_tableId(), newPageTableId, true);

                    // commit: final page viewed
                    Pref.setPref_focusView_page_tableId(act, newPageTableId);

                    mFolderUi.tabsHost.setCurrentPageTableId(newPageTableId);

                    //add directory audio links
                    addAudio_byDir(file.listFiles(),pageName);
                }
            }
            else
            {
                System.out.println("--- onListItemClick / not directory");
            	// view the selected file's content
            	if( file.isFile() && UtilAudio.hasAudioExtension(file) ) 	{
                    View view1 = getActivity().findViewById(R.id.view_back_btn_bg);
                    view1.setVisibility(View.GONE);
                    View view2 = getActivity().findViewById(R.id.file_list_title);
                    view2.setVisibility(View.GONE);
            	}
            	else
            	{
            		Toast.makeText(getActivity(),R.string.file_not_found,Toast.LENGTH_SHORT).show();
                    String dirString = new File(currFilePath).getParent();
                    File dir = new File(dirString);
                    showFilesList(dir.listFiles());
            	}
            }
        }
    }

    int showFilesList(File[] files)
    {
        int dirCount = 0;
        if(files == null)
        {
//            System.out.println("Add_audio_byFolder / _showFilesList / files = null");
        	Toast.makeText(getActivity(),"Please select audio file",Toast.LENGTH_SHORT).show();
        }
        else
        {
//        	System.out.println("Add_audio_byFolder / _showFilesList / files length = " + files.length);
            filePathArray = new ArrayList<>();
            fileNames = new ArrayList<>();
            filePathArray.add("");

            if(currFilePath.equalsIgnoreCase("/storage"))
                fileNames.add("ROOT");
//            else if(currFilePath.equalsIgnoreCase(appDir))
//                fileNames.add("..");
            else
                fileNames.add("..");

            // sort by alphabetic
            Arrays.sort(files, new FileNameComparator());

	        for(File file : files)
	        {
                // add for filtering non-audio file
                if(!file.isDirectory() && UtilAudio.hasAudioExtension(file) ) {
                    filePathArray.add(file.getPath());
                    // file
                    fileNames.add(file.getName());
                }
                else if(file.isDirectory())
                {
//                    System.out.println("Add_audio_byFolder / _showFilesList / dir file.getPath() = " + file.getPath());

                    //TODO 20201122 temp for Skip /storage/self
                    if(!file.getPath().equals("/storage/self")) {
                        dirCount++;
                        filePathArray.add(file.getPath());

                        // directory
                        String dirName = file.getName();
//                        System.out.println("Add_audio_byFolder / _showFilesList / dirName 1 = " + dirName);
                        // get volume name under root
                        dirName = StorageUtils.getVolumeName(dirName);
//                        System.out.println("Add_audio_byFolder / _showFilesList / dirName 2 = " + dirName);
                        fileNames.add("[ " + dirName + " ]");
                    }
                }
	        }

            FileNameAdapter fileListAdapter = new FileNameAdapter(getActivity(),
                                                                  R.layout.import_sd_files_list_row,
                                                                  fileNames);
	        setListAdapter(fileListAdapter);
        }

        return dirCount;
    }

    // Directory group and file group, both directory and file are sorted alphabetically
    // cf. https://stackoverflow.com/questions/24404055/sort-filelist-folders-then-files-both-alphabetically-in-android
    private class FileNameComparator implements Comparator<File> {
        // lhs: left hand side
        // rhs: right hand side
        public int compare(File lhsS, File rhsS){
            File lhs = new File(lhsS.toString().toLowerCase(Locale.US));
            File rhs= new File(rhsS.toString().toLowerCase(Locale.US));
            if (lhs.isDirectory() && !rhs.isDirectory()){
                // Directory before File
                return -1;
            } else if (!lhs.isDirectory() && rhs.isDirectory()){
                // File after directory
                return 1;
            } else {
                // Otherwise in Alphabetic order...
                return lhs.getName().compareTo(rhs.getName());
            }
        }
    }


    // File name array for setting focus and file name, note: without generic will cause unchecked or unsafe operations warning
    class FileNameAdapter extends ArrayAdapter<String>
    {
        FileNameAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView,ViewGroup parent) {
            if(convertView == null)
            {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.import_sd_files_list_row, parent, false);
            }

            convertView.setFocusable(true);
            convertView.setClickable(true);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    System.out.println("Add_audio_byFolder / position  = " + position);
                    onListItemClick(position);
                }
            });

            TextView tv = (TextView)convertView.findViewById(R.id.text1);
            String appName = getString(R.string.app_name);

            String fileName = fileNames.get(position);
            tv.setText(fileName);
            if(fileName.equalsIgnoreCase("sdcard")   ||
                    fileName.equalsIgnoreCase(appName)    ||
                    fileName.equalsIgnoreCase("[ audio7 ]") || //todo need to change for different app name
                    fileName.equalsIgnoreCase("[ Download ]")   ) {
                tv.setTypeface(null, Typeface.BOLD);
            }
            else {
                tv.setTypeface(null, Typeface.NORMAL);
            }

            return convertView;
        }
    }

    // add audio
    void addAudio_byDir( File[] files,String pageName)
    {
        if(files == null)
        {
            Toast.makeText(getActivity(),"Please select audio folder",Toast.LENGTH_SHORT).show();
        }
        else {
//        	System.out.println("files length = " + files.length);

            // sort by alphabetic
            Arrays.sort(files, new FileNameComparator());

            for (File file : files) {
                // add for filtering non-audio file
                if (!file.isDirectory() && UtilAudio.hasAudioExtension(file) ) {
                    String uriStr = "file://".concat(file.getPath());

                    DB_page dB = new DB_page(getActivity(), mFolderUi.tabsHost.getCurrentPageTableId());
                    if( !Util.isEmptyString(uriStr))
                    {
                        // insert
                        // set marking to 1 for default
                        //System.out.println("Add_audio_byFolder / _addAudio_byDir / uriStr = " + uriStr);

                        dB.insertNote("",  uriStr, "",  1);// add new note, get return row Id
                    }

                    if(!Util.isEmptyString(uriStr))
                    {
                        String[] audioName = Util.getDisplayNameByUriString(uriStr, getActivity());
                    }

                } else if (file.isDirectory()) {
                    System.out.println("=> is directory ,  file.getPath() = " +  file.getPath());
                }
            }

            Util.showSavedFileToast(getActivity(),pageName +" Added",500);
        }
    }
}