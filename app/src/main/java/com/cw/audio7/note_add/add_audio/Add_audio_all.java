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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.drawer.Drawer;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


public class Add_audio_all extends Fragment
{
    List<String> filePathArray = null;
    List<String> fileNames = null;
    public View rootView;
    int PAGES_PER_FOLDER = 7;
    AppCompatActivity act;
    Drawer drawer;
    Folder folder;
    TabsHost tabsHost;

    public Add_audio_all(Drawer _drawer, Folder _folder) {
        drawer = _drawer;
        folder = _folder;
        tabsHost = _folder.tabsHost;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.add_all, container, false);
//        System.out.println("Add_audio_all / _onCreateView");
        act = (AppCompatActivity) getActivity();

        TextView titleViewText = (TextView) rootView.findViewById(R.id.add_all_message);
        titleViewText.setText(R.string.note_add_all_title);

        // auto add all: no UI is needed
        Add_audio_all_asyncTask task = new Add_audio_all_asyncTask(act,rootView);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return rootView;
    }

    String appDir;
    String currFilePath;
    Integer existing_folders_count;
    Integer folders_count;
    Integer pages_count;
    boolean isDoing;

    @Override
    public void onResume() {
        super.onResume();
//        System.out.println("Add_audio_all / _onResume");
    }

    /**
     *  SD Card storage info:
     *
     *  Internal SD Card
     *  1. /storage/emulated/0/Music
     *         appDir = Environment.getExternalStorageDirectory().toString() +
     *                                     "/" +
     *                                     Util.getStorageDirName(getActivity());
     *
     *  2. /sdcard/Music
     *         appDir = System.getenv("EXTERNAL_STORAGE")+
     *                                     "/" +
     *                                     Util.getStorageDirName(getActivity()) ;
     *
     *  External SD Card
     *       appDir = "/storage/B8F3-5830/Music"; // for Nokia phone
     *       appDir = "/storage/8C01-308E/Music"; // for Sony phone
     *  1. /mnt/media_rw/8C01-308E/Music
     *  2. /storage/8C01-308E/Music
     *
     * */

    // Scan all storage devices and save audio links to DB
    void scan_and_save(String currFilePath, boolean beSaved)
    {
        List<String> list;
        list = getListInPath(currFilePath);

        if (list.size() > 0 ) {

            for (String file : list) {
                File fileDir = new File(currFilePath.concat("/").concat(file));

//                System.out.println("==>  file = " + file);
//                System.out.println("==>  fileDir = " + fileDir.getPath());

                //Skip some directories which could cause playing hang-up issue
                if( !fileDir.getAbsolutePath().contains("Android/data") &&
                    !fileDir.getAbsolutePath().contains("Android/media") &&
                    ( !fileDir.getAbsolutePath().contains("..") ||
                      (fileDir.getAbsolutePath().contains("..") &&  (file.length()!=2) ) ) )
                {
                    if (fileDir.isDirectory()) {

                        // add page
                        int dirs_count = 0;
                        int dirsFilesCount = 0;

                        // get page name
                        String pageName = fileDir.getName();
//                        System.out.println(" ");
//                        System.out.println("==>  dir Name = " + pageName);

                        if (fileDir.listFiles() != null) {
                            dirsFilesCount = fileDir.listFiles().length;
//                            System.out.println("--> dirsFilesCount : " + dirsFilesCount);
                            dirs_count = getDirsCount(fileDir.listFiles());
//                            System.out.println("--1 dirs_count : " + dirs_count);
                            int files_count =  dirsFilesCount - dirs_count;
//                            System.out.println("--2 files_count : " + files_count);
                        }

                        // check if audio files exist
                        if(beSaved) {
                            if ((dirs_count == 0) && (dirsFilesCount > 0)) {

                                // check if dir has audio files before Save
                                if(getAudioFilesCount(fileDir)>0) {

                                    // add new folder
                                    if ((pages_count % PAGES_PER_FOLDER) == 0) {
                                        folders_count = (pages_count / PAGES_PER_FOLDER) + 1 + existing_folders_count;

                                        if (folders_count > Drawer.getFoldersCount(act))
                                            addNewFolder(String.valueOf((pages_count / PAGES_PER_FOLDER) + 1));
                                    }

                                    // add new page
                                    addNewPage(pageName);
                                    pages_count++;
                                }
                            }
                        }

                        // recursive
                        scan_and_save(fileDir.getAbsolutePath(),beSaved);

                    } // if (fileDir.isDirectory())
                    else {
                        String audioUri =  "file://".concat(fileDir.getPath());
//                        System.out.println("----- audioUri = " + audioUri);
                        if(beSaved)
                            addNewNote(audioUri);
                    }
                } // if(!fileDir.getAbsolutePath().contains(".."))
            } // for (String file : list)
        } // if (list.size() > 0)
    }

    // Get audio files count
    int getAudioFilesCount(File dir)
    {
        int audioFilesCount = 0;
        File[] files = dir.listFiles();
        {
            // sort by alphabetic
            if (files != null) {
                Arrays.sort(files, new FileNameComparator());

                for (File file : files) {
                    // add for filtering non-audio file
                    if (!file.isDirectory() &&
                            (UtilAudio.hasAudioExtension(file))) {
                        audioFilesCount++;
                    }
                }
            }
        }
//        System.out.println("---------------- audioFilesCount = " + audioFilesCount);
        return  audioFilesCount;
    }

    // add new folder
    void addNewFolder(String folderName)
    {
        DB_drawer dB_drawer = new DB_drawer(act);
        int folders_count = dB_drawer.getFoldersCount(true);

        // get last folder Id
        long  lastFolderId = 0;
        for(int i=0; i<folders_count; i++)
        {
            if(dB_drawer.getFolderId(i,true) > lastFolderId)
                lastFolderId = dB_drawer.getFolderId(i,true);
        }

        // new last folder Id
        lastFolderId++;

        // insert new folder row
        dB_drawer.insertFolder((int)lastFolderId, folderName, true);

        // get last folder table Id
        int lastFolderTableId =0;
        for(int i=0;i<folders_count;i++)
        {
            if(dB_drawer.getFolderTableId(i,true)>lastFolderTableId)
                lastFolderTableId = dB_drawer.getFolderTableId(i,true);
        }

        // new last folder table Id
        lastFolderTableId++;

        // insert new folder table
        dB_drawer.insertFolderTable(lastFolderTableId, true);

        // set focus
        DB_folder.setFocusFolder_tableId(lastFolderTableId);
        Pref.setPref_focusView_folder_tableId(act, lastFolderTableId);
        Pref.setPref_focusView_page_tableId(act, 1);

        if(tabsHost != null) {
            tabsHost.setLastPageTableId(0);
            tabsHost.setFocus_tabPos(0);
        }
    }

    // add new page
    void addNewPage(String pageName)
    {
        // get focus folder position
        DB_drawer dB_drawer = new DB_drawer(act);
        int folders_count = dB_drawer.getFoldersCount(true);
        for (int pos = 0; pos < folders_count; pos++) {
            if (dB_drawer.getFolderTableId(pos, true) == Pref.getPref_focusView_folder_tableId(act))
                Folder.setFocus_folderPos(pos);
        }
        // get current Max page table Id
        int currentMaxPageTableId = 0;
        int pagesCount = folder.getFolder_pagesCount(act, Folder.getFocus_folderPos());

        DB_folder db_folder = new DB_folder(DB_folder.getFocusFolder_tableId());
        for (int i = 0; i < pagesCount; i++) {
            int id = db_folder.getPageTableId(i, true);
            if (id > currentMaxPageTableId)
                currentMaxPageTableId = id;
        }
        currentMaxPageTableId++;

        int newPageTableId = currentMaxPageTableId;

        // insert page name
        int style = Util.getNewPageStyle(act);
        db_folder.insertPage(DB_folder.getFocusFolder_tableName(), pageName, newPageTableId, style, true);

        // insert page table
        db_folder.insertPageTable(db_folder, DB_folder.getFocusFolder_tableId(), newPageTableId, true);

        // commit: final page viewed
        Pref.setPref_focusView_page_tableId(act, newPageTableId);

        if(tabsHost != null)
            tabsHost.setCurrentPageTableId(newPageTableId);
    }

    // add new note
    void addNewNote(String audioUri)
    {
            int currPageTableId;
            if(tabsHost != null)
                currPageTableId = TabsHost.getCurrentPageTableId();
            else
                currPageTableId = Pref.getPref_focusView_page_tableId(act);

            DB_page dB = new DB_page(currPageTableId);
            // insert
            if (!Util.isEmptyString(audioUri))
                dB.insertNote("", audioUri, "", 1);// add new note, get return row Id
    }

    // get list array in designated path
    List<String> getListInPath(String path)
    {
        File[] files = new File(path).listFiles();
        if(path == null)
        {
            Toast.makeText(getActivity(),"Please select audio file",Toast.LENGTH_SHORT).show();
        }
        else
        {
            filePathArray = new ArrayList<>();
            fileNames = new ArrayList<>();
            filePathArray.add("");

            if(currFilePath.equalsIgnoreCase(new File(appDir).getParent()))
                fileNames.add("ROOT");
            else if(currFilePath.equalsIgnoreCase(appDir))
                fileNames.add("..");
            else
                fileNames.add("..");

            // sort by alphabetic
            if(files != null) {
                Arrays.sort(files, new FileNameComparator());

                for (File file : files) {
                    // add for filtering non-audio file
                    if (!file.isDirectory() &&
                            (UtilAudio.hasAudioExtension(file))) {
                        filePathArray.add(file.getPath());
                        // file
                        fileNames.add(file.getName());
                    } else if (file.isDirectory()) {
                        filePathArray.add(file.getPath());
                        // directory
                        fileNames.add(file.getName());
                    }
                }
            }
        }
        return fileNames;
    }

    // Get directories count
    int  getDirsCount(File[] files)
    {
        int dirCount = 0;
        if(files != null)
        {
            // sort by alphabetic
            Arrays.sort(files, new FileNameComparator());

            for(File file : files)
            {
                if(file.isDirectory())
                    dirCount++;
            }
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

    /**
     *  Add all audio links to DB
     */
    void doAddAll()
    {
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();

        existing_folders_count = Drawer.getFoldersCount(act);
        folders_count = 0;
        pages_count = 0;
        isDoing = true;

        for(int i=0;i<storageList.size();i++) {
            System.out.println("-->  storageList[" + i +"] name = "+ storageList.get(i).getDisplayName());
            System.out.println("-->  storageList[" + i +"] path = "+ storageList.get(i).path);
            System.out.println("-->  storageList[" + i +"] display number = "+ storageList.get(i).display_number);

            String sdCardPath =  storageList.get(i).path;

            appDir = sdCardPath;

            if (appDir.contains("/mnt/media_rw"))
                appDir = appDir.replace("mnt/media_rw","storage");

            System.out.println("-->  storageList[" + i +"] appDir = "+ appDir);

            currFilePath = appDir;

            scan_and_save(currFilePath,true);
        }

        isDoing = false;
    }

    /**
     *  Async: show progress bar and do Add all
     */
    class Add_audio_all_asyncTask extends AsyncTask<Void, Integer, Void> {

        private ProgressBar progressBar;
        AppCompatActivity act;
        View rootView;
        private TextView messageText;

        Add_audio_all_asyncTask(AppCompatActivity _act, View _rootView) {
//            System.out.println("Add_audio_all / Add_audio_all_asyncTask / _constructor");
            act = _act;
            rootView = _rootView;

            Util.lockOrientation(act);

            messageText = (TextView) rootView.findViewById(R.id.add_all_message);
            messageText.setText(R.string.note_add_all_title_adding);

            progressBar = (ProgressBar) rootView.findViewById(R.id.add_all_progress);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (this.progressBar != null) {
                progressBar.setProgress(values[0]);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {

            // main function for adding audio links
            doAddAll();

            while (isDoing)
            {
                System.out.println("doing");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            progressBar.setVisibility(View.INVISIBLE);
            messageText.setText(R.string.note_add_all_title_finish);
            messageText.setVisibility(View.VISIBLE);

            // auto add all: no UI is needed
            Pref.setPref_will_create_default_content(act, false);
            Objects.requireNonNull(act).getSupportFragmentManager().popBackStack();
            act.finish();
            Intent intent  = new Intent(act,MainAct.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            act.startActivity(intent);
        } // onPostExecute
    }

}