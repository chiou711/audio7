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

import com.cw.audio7.main.MainAct;
import com.cw.audio7.R;
import com.cw.audio7.util.BaseBackPressedListener;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

public class Import_filesList extends ListFragment
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
                            if (srcFile.getName().contains("XML") || srcFile.getName().contains("xml"))
                                FileUtils.copyFile(srcFile, targetFile);
                        } catch (IOException e) {

                            e.printStackTrace();
                        }
                    }
                }

                // refresh list view
                File dir = new File(targetDirPath);
                getFiles(dir.listFiles());
            }
        });

        ((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(MainAct.mAct));

        return rootView;
    }

    @Override
    public void onCreate(Bundle bundle) 
    {
        super.onCreate(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        listView = getListView();
        String dirString = Environment.getExternalStorageDirectory().toString() +
                "/" +
                Util.getStorageDirName(getActivity());
        getFiles(new File(dirString).listFiles());
    }

    int selectedRow;
    String currFilePath;
    // on list item click
//    @Override
//    public void onListItemClick(ListView l, View v, int position, long rowId)
    public void onListItemClick(long rowId)
    {
        selectedRow = (int)rowId;
        if(selectedRow == 0)
        {
        	//root
            getFiles(new File("/").listFiles());
        }
        else
        {
            currFilePath = filePathArray.get(selectedRow);
            final File file = new File(currFilePath);
            if(file.isDirectory())
            {
            	//directory
                getFiles(file.listFiles());
            }
            else
            {
            	// view the selected file's content
            	if( file.isFile() &&
                   (file.getName().contains("XML") ||
                    file.getName().contains("xml")     ))
            	{
                    View view1 = getActivity().findViewById(R.id.view_back_btn_bg);
                    view1.setVisibility(View.GONE);
                    View view2 = getActivity().findViewById(R.id.file_list_title);
                    view2.setVisibility(View.GONE);

                    Import_fileView fragment = new Import_fileView();
                    final Bundle args = new Bundle();
                    args.putString("KEY_FILE_PATH", currFilePath);
                    fragment.setArguments(args);
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.file_list_linear, fragment,"import_view").addToBackStack(null).commit();
            	}
            	else
            	{
            		Toast.makeText(getActivity(),R.string.file_not_found,Toast.LENGTH_SHORT).show();
                    String dirString = new File(currFilePath).getParent();
                    File dir = new File(dirString);
                    getFiles(dir.listFiles());
            	}
            }
        }
    }

    void getFiles(File[] files)
    {
        if(files == null)
        {
        	Toast.makeText(getActivity(),R.string.toast_import_SDCard_no_file,Toast.LENGTH_SHORT).show();
//        	getActivity().finish();
            getActivity().getSupportFragmentManager().popBackStack();
        }
        else
        {
//        	System.out.println("files length = " + files.length);
            filePathArray = new ArrayList<>();
            fileNames = new ArrayList<>();
            filePathArray.add("");
            fileNames.add("ROOT");

            // sort by alphabetic
            Arrays.sort(files, new FileNameComparator());

	        for(File file : files)
	        {
                // add for filtering non-XML file
                if(file.getName().contains("XML") || file.getName().contains("xml"))
                {
                    filePathArray.add(file.getPath());
                    fileNames.add(file.getName());
                }
	        }
//	        fileListAdapter = new ArrayAdapter<>(getActivity(),
//	        									 R.layout.import_sd_files_list_row,
//	        									 fileNames);
            FileNameAdapter fileListAdapter = new FileNameAdapter(getActivity(),
                                                                  R.layout.import_sd_files_list_row,
                                                                  fileNames);
	        setListAdapter(fileListAdapter);
        }
    }

    // Directory group and file group, both directory and file are sorted alphabetically
    // cf. https://stackoverflow.com/questions/24404055/sort-filelist-folders-then-files-both-alphabetically-in-android
    private class FileNameComparator implements Comparator<File> {
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

            TextView tv = (TextView)convertView.findViewById(R.id.text1);
            String appName = getString(R.string.app_name);
            tv.setText(fileNames.get(position));
            if(fileNames.get(position).equalsIgnoreCase("sdcard")   ||
               fileNames.get(position).equalsIgnoreCase(appName)    ||
               fileNames.get(position).equalsIgnoreCase("audio7") || //todo need to change for different app name
               fileNames.get(position).equalsIgnoreCase("Download")   )
                tv.setTypeface(null, Typeface.BOLD);
            else
                tv.setTypeface(null, Typeface.NORMAL);

            final int item = position;
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onListItemClick(item);
                }
            });
            return convertView;
        }
    }
}