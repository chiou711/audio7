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

import java.io.FileInputStream;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.cw.audio7.db.DB_drawer;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import android.content.Context;
import android.util.Xml;

public class ParseXmlToDB {

    private String pageName,title,body,audio;
    private DB_folder mDb_folder;
    private DB_page mDb_page;

    private Context mContext;
   
    private FileInputStream fileInputStream = null;
    public static boolean isParsing;
    String fileBody = "";
    private String strSplitter;
    private boolean mEnableInsertDB = true;
    int folderTableId;

    ParseXmlToDB(FileInputStream fileInputStream, Context context)
    {
        mContext = context;
        this.fileInputStream = fileInputStream;

        folderTableId = Pref.getPref_focusView_folder_tableId(mContext);
        mDb_folder = new DB_folder(MainAct.mAct, folderTableId);

        mDb_page = new DB_page(MainAct.mAct,TabsHost.getCurrentPageTableId());

        isParsing = true;
    }

    public String getTitle()
    {
    return title;
    }

    public String getBody()
    {
    return body;
    }

    public String getAudio()
    {
    return audio;
    }

    public String getPage()
    {
    return pageName;
    }

    public void parseXMLAndInsertDB(XmlPullParser myParser)
    {
        int event;
        String text=null;
        try
        {
            event = myParser.getEventType();
            boolean isEnd = false;
            while (event != XmlPullParser.END_DOCUMENT)
            {
                String name = myParser.getName(); //name: null,  item, title, description
                System.out.println("ParseXmlToDB / _parseXMLAndInsertDB / name = " + name);
                System.out.println("ParseXmlToDB / _parseXMLAndInsertDB / event = " + event);
                switch (event)
                {
                    case XmlPullParser.START_TAG:
                    if(name.equals("note"))
                    {
                        strSplitter = "--- note ---";
                    }
                    break;

                    case XmlPullParser.TEXT:
                        text = myParser.getText();
                        System.out.println("ParseXmlToDB / _parseXMLAndInsertDB / text = " + text);
                    break;

                    case XmlPullParser.END_TAG:
                        if(name.equals("folder_name"))
                        {
                            String folderName = text.trim();
                            if(mEnableInsertDB)
                            {
                                DB_drawer dB_drawer = new DB_drawer(MainAct.mAct);
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

                                DB_folder.setFocusFolder_tableId(lastFolderTableId);

                                Pref.setPref_focusView_folder_tableId(MainAct.mAct, lastFolderTableId);
                                Pref.setPref_focusView_page_tableId(MainAct.mAct, 1);
//                                TabsHost.setLastPageTableId(0);

                                TabsHost.setFocus_tabPos(0);
                            }
                            fileBody = fileBody.concat(Util.NEW_LINE + "*** " + "Folder:" + " " + folderName + " ***");
                        }
                        else if(name.equals("page_name"))
                        {
                            pageName = text.trim();
                            if(mEnableInsertDB)
                            {
                                int style = Util.getNewPageStyle(mContext);

                                // get last page table Id
                                mDb_folder = new DB_folder(MainAct.mAct,Pref.getPref_focusView_folder_tableId(mContext));
                                int lastPageTableId = 0;
                                for(int i=0; i<mDb_folder.getPagesCount(true); i++)
                                {
                                    if(mDb_folder.getPageTableId(i,true) > lastPageTableId)
                                        lastPageTableId = mDb_folder.getPageTableId(i,true);
                                }

                                // set last page table Id
                                TabsHost.setLastPageTableId(lastPageTableId);

                                // style is not set in XML file, so insert default style instead
                                mDb_folder.insertPage(DB_folder.getFocusFolder_tableName(),
                                                      pageName,
                                                      TabsHost.getLastPageTableId() + 1,
                                                      style ,
                                                      true);

                                // insert page table for new tab
                                mDb_folder.insertPageTable(mDb_folder,DB_folder.getFocusFolder_tableId(), TabsHost.getLastPageTableId() + 1, true );

                                // update last tab Id after Insert
                                TabsHost.setLastPageTableId(TabsHost.getLastPageTableId() + 1);//todo ??? logic error? should be max page Id?

                                // update from 0 to 1 if Import starts from Empty
                                int pgsCnt = mDb_folder.getPagesCount(true);
                                if((pgsCnt > 0) && (Pref.getPref_focusView_page_tableId(MainAct.mAct) ==0))
                                    Pref.setPref_focusView_page_tableId(MainAct.mAct, 1);
                            }
                            fileBody = fileBody.concat(Util.NEW_LINE + "=== " + "Page:" + " " + pageName + " ===");
                       }
                       else if(name.equals("title"))
                       {
                            text = text.replace("[n]"," ");
                            text = text.replace("[s]"," ");
                            title = text.trim();
                       }
                       else if(name.equals("body"))
                       {
                            body = text.trim();
                       }
                       else if(name.equals("audio"))
                       {
                            audio = text.trim();
                            audio = Util.getDefaultExternalStoragePath(audio);

                           // final column, do insert
                           text = text.replace("&apos;","\'");
                           text = text.replace("&quot;","\"");
                           text = text.replace("&amp;","\\&");

                           System.out.println("ParseXmlToDB / _parseXMLAndInsertDB / name 2 = " + name);
                           if(mEnableInsertDB)
                           {
                               DB_page.setFocusPage_tableId(TabsHost.getLastPageTableId());
                               if(title.length() !=0 || body.length() != 0 ||  audio.length() !=0 )
                               {
                                   if(!Util.isEmptyString(audio))
                                       mDb_page.insertNote(title,  audio,   body,1); // add mark for media
                                   else
                                       mDb_page.insertNote(title,  audio,   body,0);
                               }
                           }
                           fileBody = fileBody.concat(Util.NEW_LINE + strSplitter);
                           fileBody = fileBody.concat(Util.NEW_LINE + "title:" + " " + title);
                           fileBody = fileBody.concat(Util.NEW_LINE + "body:" + " " + body);
                           fileBody = fileBody.concat(Util.NEW_LINE + "audio:" + " " + audio);
                           fileBody = fileBody.concat(Util.NEW_LINE);

                       }
                       else if(name.equals("LiteNote"))
                       {
                           isEnd = true;
                       }
                    break;
                }

                if(!isEnd)
                    event = myParser.next();
                else {
                    event = XmlPullParser.END_DOCUMENT;
                    System.out.println("ParseXmlToDB / _parseXMLAndInsertDB / isEnd = " + isEnd);
                }
            }

            isParsing = false;
            System.out.println("ParseXmlToDB / _parseXMLAndInsertDB / isParsing = " + isParsing);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void handleXML()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    InputStream stream = fileInputStream;
                    XmlPullParser myParser = XmlPullParserFactory.newInstance().newPullParser();

                    // add for fixing ampersand will halt the system while parsing
                    // (to allow requesting that parser be as lenient as possible when parsing invalid XML)
                    myParser.setFeature(Xml.FEATURE_RELAXED, true);

                    myParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    myParser.setInput(stream, null);

                    //myParser.defineEntityReplacementText("amp", "&#38;#38;");
                    //cf. https://developer.android.com/reference/org/xmlpull/v1/XmlPullParser.html#defineEntityReplacementText(java.lang.String, java.lang.String)
                    //Note: The list of pre-defined entity names will always contain standard
                    // XML entities such as amp (&amp;), lt (&lt;), gt (&gt;), quot (&quot;), and apos (&apos;).
                    // Those cannot be redefined by this method!

                    parseXMLAndInsertDB(myParser);
                    stream.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    void enableInsertDB(boolean en)
    {
        mEnableInsertDB = en;
    }
}