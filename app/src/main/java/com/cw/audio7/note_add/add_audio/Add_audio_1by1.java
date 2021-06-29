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
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
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
import com.cw.audio7.db.DB_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;

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

public class Add_audio_1by1 extends ListFragment
{
    private List<String> filePathArray = null;
    List<String> fileNames = null;
    public View rootView;
    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println("Add_audio_1by1 / _onCreateView");
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
                            if (UtilAudio.hasAudioExtension(srcFile) )
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

        return rootView;
    }

    @Override
    public void onCreate(Bundle bundle) 
    {
        super.onCreate(bundle);
    }

    String appDir;

    List<Boolean> checkedArr;

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
            currFilePath = filePathArray.get(selectedRow);
            System.out.println("Add_audio / _onListItemClick / currFilePath = " + currFilePath);

            //TODO 20201122 temp for open /storage/emulated/0
            if(currFilePath.equals("/storage/emulated"))
                currFilePath = currFilePath.concat("/0");

            final File file = new File(currFilePath);
            if(file.isDirectory())
            {
            	//directory
                if(file.listFiles() != null)
                    showFilesList(file.listFiles());
            }
            else
            {
            	// view the selected file's content
            	if( file.isFile() && UtilAudio.hasAudioExtension(file)) 	{
                    View view1 = getActivity().findViewById(R.id.view_back_btn_bg);
                    view1.setVisibility(View.GONE);
                    View view2 = getActivity().findViewById(R.id.file_list_title);
                    view2.setVisibility(View.GONE);

                    // add path to DB
                    addAudio(currFilePath);

                    checkedArr.set(selectedRow,true);
                    fileListAdapter.notifyDataSetChanged();
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

    FileNameAdapter fileListAdapter;
    void showFilesList(File[] files)
    {
        checkedArr = new ArrayList<>();

        if(files == null)
        {
        	Toast.makeText(getActivity(),"Please select audio file",Toast.LENGTH_SHORT).show();
        }
        else
        {
        	System.out.println("files length = " + files.length);
            filePathArray = new ArrayList<>();
            fileNames = new ArrayList<>();
            filePathArray.add("");

            if(currFilePath.equalsIgnoreCase(new File(appDir).getParent()))
                fileNames.add("ROOT");
//            else if(currFilePath.equalsIgnoreCase(appDir))
//                fileNames.add("..");
            else
                fileNames.add("..");

            // init for top row
            checkedArr.add(false);

            // sort by alphabetic
            Arrays.sort(files, new FileNameComparator());

	        for(File file : files)
	        {
	            // init for each unchecked
                checkedArr.add(false);

                // add for filtering non-audio file
                if(file.isDirectory())
                {
                    filePathArray.add(file.getPath());

                    // directory
                    String dirName = file.getName();
                    System.out.println("Add_audio_1by1 / _showFilesList / dirName  = " + dirName);

                    // get volume name under root
                    dirName = StorageUtils.getVolumeName(dirName);
//                        System.out.println("Add_audio_1by1 / _showFilesList / dirName (with volume name) = " + dirName);

                    fileNames.add("[ " + dirName +" ]");
                }
                else if(!file.isDirectory() &&
                    UtilAudio.hasAudioExtension(file) ) {
                    System.out.println("Add_audio_1by1 / _showFilesList / file.getName()  = " + file.getName());
                    filePathArray.add(file.getPath());
                    // file
                    String uriStr = getAudioUriString(file.getPath());
                    fileNames.add(uriStr);
                }
	        }

//            System.out.println("checkedArr size = " + checkedArr.size());

            fileListAdapter = new FileNameAdapter(getActivity(),
                                                                  R.layout.import_sd_files_list_row,
                                                                  fileNames);
	        setListAdapter(fileListAdapter);
        }
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

    private class ViewHolder {
        TextView audioTitle;
        TextView audioArtist;
    }

    // File name array for setting focus and file name, note: without generic will cause unchecked or unsafe operations warning
    class FileNameAdapter extends ArrayAdapter<String>
    {
        FileNameAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView,ViewGroup parent) {
            System.out.println("-- _getView / position = " + position);

            ViewHolder viewHolder;
            if(convertView == null)
            {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.add_link_1by1_list_row , parent, false);
                viewHolder = new ViewHolder();
                viewHolder.audioTitle = (TextView)convertView.findViewById(R.id.title_1by1);
                viewHolder.audioArtist = (TextView)convertView.findViewById(R.id.artist_1by1);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            View row = convertView.findViewById(R.id.add_audio_1by1);
            row.setFocusable(true);
            row.setClickable(true);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onListItemClick(position);
                }
            });

            if( (checkedArr.size()>0) && checkedArr.get(position)) {
                viewHolder.audioTitle.setBackgroundColor(ColorSet.getHighlightColor(getActivity()));
                viewHolder.audioArtist.setBackgroundColor(ColorSet.getHighlightColor(getActivity()));
            }
            else {
                viewHolder.audioTitle.setBackgroundColor(Color.WHITE);
                viewHolder.audioArtist.setBackgroundColor(Color.WHITE);
            }

            String appName = getString(R.string.app_name);

            String fileName = fileNames.get(position);

            // audio title / artist
            if(UtilAudio.hasAudioExtension(fileName)) {
                String[] audioName = Util.getDisplayNameByUriString(fileName, getActivity());
                viewHolder.audioTitle.setText(audioName[0]);
                viewHolder.audioTitle.setTypeface(null, Typeface.BOLD);
                viewHolder.audioArtist.setText(audioName[1]);
                viewHolder.audioArtist.setTypeface(null, Typeface.ITALIC);
            } else { // dir
                viewHolder.audioTitle.setText(fileName);

                // bold for special directory
                if(fileName.equalsIgnoreCase("sdcard")   ||
                        fileName.equalsIgnoreCase(appName)    ||
                        fileName.equalsIgnoreCase("[ audio7 ]") || //todo need to change for different app name
                        fileName.equalsIgnoreCase("[ Download ]")   ) {
                    viewHolder.audioTitle.setTypeface(null, Typeface.BOLD);
                }
                else {
                    viewHolder.audioTitle.setTypeface(null, Typeface.NORMAL);
                }

                viewHolder.audioArtist.setText("");
            }

            return convertView;
        }
    }

    // add audio
    void addAudio(String path)
    {
        String uriStr = getAudioUriString(path);
        DB_page dB = new DB_page(getActivity(), mFolderUi.tabsHost.getCurrentPageTableId());
        if( !Util.isEmptyString(uriStr)) {
            // insert new link, set marking to 1 for default
            dB.insertNote("",  uriStr, "",  1);
        }

        if(!Util.isEmptyString(uriStr)) {
            String[] audioName = Util.getDisplayNameByUriString(uriStr, getActivity());
            Util.showSavedFileToast(getActivity(),audioName[0]+" / " +audioName[1],1000);
        }

    }
    
    // Get audio Uri string by path
    String getAudioUriString(String path) {
        path = "file://".concat(path);
        Uri selectedUri = Uri.parse(path);
        System.out.println("Add_audio / _getAudioUriString / selectedUri = " + selectedUri);

        String scheme = selectedUri.getScheme();
        String audioUri = selectedUri.toString();

        // check if content scheme points to local file
        if((scheme!= null) && scheme.equalsIgnoreCase("content")) {
            String realPath = Util.getLocalRealPathByUri(getActivity(), selectedUri);

            if(realPath != null)
                audioUri = "file://".concat(realPath);
        }
        return audioUri;
    }
}