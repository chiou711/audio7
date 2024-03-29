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

package com.cw.audio7.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.cw.audio7.folder.Folder;
import com.cw.audio7.page.Checked_notes_option;
import com.cw.audio7.R;
import com.cw.audio7.db.DB_folder;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;
import com.cw.audio7.define.Define;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class Util
{
    SharedPreferences mPref_vibration;
    private static Context mContext;
	private Activity mAct;
    private static DB_folder mDbFolder;
    public static String NEW_LINE = "\r" + System.getProperty("line.separator");

	public static int CHOOSER_SET_AUDIO = 5;

	private int defaultBgClr;
	private int defaultTextClr;

	public static final int PERMISSIONS_REQUEST_STORAGE = 12;
	public static final int PERMISSIONS_REQUEST_STORAGE_ADD_NEW = 13;
	public static final int PERMISSIONS_REQUEST_STORAGE_IMPORT = 14;
	public static final int PERMISSIONS_REQUEST_STORAGE_EXPORT = 15;

	public Util(){}
    
	public Util(AppCompatActivity activity) {
		mContext = activity;
		mAct = activity;
	}
	
	public Util(Context context) {
		mContext = context;
	}
	
	// set vibration time
	public void vibrate()
	{
		mPref_vibration = mContext.getSharedPreferences("vibration", 0);
    	if(mPref_vibration.getString("KEY_ENABLE_VIBRATION","yes").equalsIgnoreCase("yes"))
    	{
			Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
			if(!mPref_vibration.getString("KEY_VIBRATION_TIME","25").equalsIgnoreCase(""))
			{
				int vibLen = Integer.valueOf(mPref_vibration.getString("KEY_VIBRATION_TIME","25"));
				mVibrator.vibrate(vibLen); //length unit is milliseconds
				System.out.println("vibration len = " + vibLen);
			}
    	}
	}
	
	// export to SD card: for checked pages
	public String exportToSdCard(AppCompatActivity act,Folder folder,String filename, List<Boolean> checkedTabs)
	{   
		//first row text
		String data ="";

		//get data from DB
		data = queryDB(act,folder,data,checkedTabs);
		
		// sent data
		data = addXmlTag(data);
		String string = data;

        exportToSdCardFile(filename,data);

        //System.out.println("---------------- export to sd String: " + string);
		return string;
	}
	
	// Export data to be SD Card file
	public void exportToSdCardFile(String filename,String data)
	{
	    // SD card path + "/" + directory path
	    String dirString = Environment.getExternalStorageDirectory().toString() +
	    		              "/" +
	    		              Util.getStorageDirName(mContext);

		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();
		File file = new File(dir, filename);
		file.setReadOnly();

//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(file);
//		} catch (IOException e1) {
//			System.out.println("_FileWriter error");
//			e1.printStackTrace();
//		}
//		BufferedWriter bw = new BufferedWriter(fw);

		BufferedWriter bw = null;
		OutputStreamWriter osw = null;

		int BUFFER_SIZE = 8192;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(file.getPath()), "UTF-8");
			bw = new BufferedWriter(osw,BUFFER_SIZE);

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		try {
			bw.write(data);
			bw.flush();
			osw.close();
			bw.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
    /**
     * Query current data base
     *
     */
    private String queryDB(AppCompatActivity act,Folder folder,String data, List<Boolean> checkedTabs)
    {
    	String curData = data;
    	
    	// folder
    	int folderTableId = Pref.getPref_focusView_folder_tableId(mContext);
    	mDbFolder = new DB_folder(folderTableId);

    	// page
    	int tabCount = checkedTabs.size();
    	for(int i=0;i<tabCount;i++)
    	{
            if(checkedTabs.get(i))
				curData = curData.concat(getStringWithXmlTag(act,folder,i, ID_FOR_TABS));
    	}
    	return curData;
    	
    }
    
    // get current time string
    public static String getCurrentTimeString()
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		
//		int hour = cal.get(Calendar.HOUR);//12h 
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		int mSec = cal.get(Calendar.MILLISECOND);
		
		String strTime = year 
				+ "" + String.format(Locale.US,"%02d", month)
				+ "" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "_" + String.format(Locale.US,"%02d", hour)
				+ "" + String.format(Locale.US,"%02d", min)
				+ "" + String.format(Locale.US,"%02d", sec) 
				+ "_" + String.format(Locale.US,"%03d", mSec);
//		System.out.println("time = "+  strTime );
		return strTime;
    }
    
    // get time string
    public static String getTimeString(Long time)
    {
    	if(time == null)
    		return "";

		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		int hour = cal.get(Calendar.HOUR);//12h 
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		
		String strTime = year 
				+ "-" + String.format(Locale.US,"%02d", month)
				+ "-" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "    " + String.format(Locale.US,"%02d", hour)
				+ ":" + String.format(Locale.US,"%02d", min)
				+ ":" + String.format(Locale.US,"%02d", sec) ;
//		System.out.println("time = "+  strTime );
		
		return strTime;
    }
    
//    void deleteAttachment(String mAttachmentFileName)
//    {
//		// delete file after sending
//		String attachmentPath_FileName = Environment.getExternalStorageDirectory().getPath() + "/" +
//										 mAttachmentFileName;
//		File file = new File(attachmentPath_FileName);
//		boolean deleted = file.delete();
//		if(deleted)
//			System.out.println("delete file is OK");
//		else
//			System.out.println("delete file is NG");
//    }
    
    // add mark to current page
	public void addMarkToCurrentPage(AppCompatActivity act,DialogInterface dialogInterface,final int action)
	{
		mDbFolder = new DB_folder(Pref.getPref_focusView_folder_tableId(act));
	    ListView listView = ((AlertDialog) dialogInterface).getListView();
	    final ListAdapter originalAdapter = listView.getAdapter();
	    final int style = Util.getCurrentPageStyle(act, TabsHost.getFocus_tabPos());
        CheckedTextView textViewDefault = new CheckedTextView(mAct) ;
        defaultBgClr = textViewDefault.getDrawingCacheBackgroundColor();
        defaultTextClr = textViewDefault.getCurrentTextColor();

	    listView.setAdapter(new ListAdapter()
	    {
	        @Override
	        public int getCount() {
	            return originalAdapter.getCount();
	        }
	
	        @Override
	        public Object getItem(int id) {
	            return originalAdapter.getItem(id);
	        }
	
	        @Override
	        public long getItemId(int id) {
	            return originalAdapter.getItemId(id);
	        }
	
	        @Override
	        public int getItemViewType(int id) {
	            return originalAdapter.getItemViewType(id);
	        }
	
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            View view = originalAdapter.getView(position, convertView, parent);
	            //set CheckedTextView in order to change button color
	            CheckedTextView textView = (CheckedTextView)view;
	            if(mDbFolder.getPageTableId(position,true) == TabsHost.getCurrentPageTableId())
	            {
		            textView.setTypeface(null, Typeface.BOLD_ITALIC);
		            textView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		            textView.setTextColor(ColorSet.mText_ColorArray[style]);
			        if(style%2 == 0)
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
			        else
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_light);

                    if(action == Checked_notes_option.MOVE_TO)
                        textView.setCheckMarkDrawable(null);
	            }
	            else
	            {
		        	textView.setTypeface(null, Typeface.NORMAL);
		            textView.setBackgroundColor(defaultBgClr);
		            textView.setTextColor(defaultTextClr);
		            textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
	            }
	            return view;
	        }

	        @Override
	        public int getViewTypeCount() {
	            return originalAdapter.getViewTypeCount();
	        }

	        @Override
	        public boolean hasStableIds() {
	            return originalAdapter.hasStableIds();
	        }
	
	        @Override
	        public boolean isEmpty() {
	            return originalAdapter.isEmpty();
	        }

	        @Override
	        public void registerDataSetObserver(DataSetObserver observer) {
	            originalAdapter.registerDataSetObserver(observer);
	
	        }
	
	        @Override
	        public void unregisterDataSetObserver(DataSetObserver observer) {
	            originalAdapter.unregisterDataSetObserver(observer);
	
	        }
	
	        @Override
	        public boolean areAllItemsEnabled() {
	            return originalAdapter.areAllItemsEnabled();
	        }
	
	        @Override
	        public boolean isEnabled(int position) {
	            return originalAdapter.isEnabled(position);
	        }
	    });
	}
	
	// get App default storage directory name
	static public String getStorageDirName(Context context)
	{
//		return context.getResources().getString(R.string.app_name);

		Resources currentResources = context.getResources();
		Configuration conf = new Configuration(currentResources.getConfiguration());
		conf.locale = Locale.ENGLISH; // apply English to avoid reading directory error
		Resources newResources = new Resources(context.getAssets(), 
											   currentResources.getDisplayMetrics(),
											   conf);
		String appName = newResources.getString(R.string.app_name);

		// restore locale
		new Resources(context.getAssets(), 
					  currentResources.getDisplayMetrics(), 
					  currentResources.getConfiguration());
		
//		System.out.println("Util / _getStorageDirName / appName = " + appName);
		return appName;		
	}
	
	// get style
	static public int getNewPageStyle(Context context)
	{
		SharedPreferences mPref_style;
		mPref_style = context.getSharedPreferences("style", 0);
		return mPref_style.getInt("KEY_STYLE",Define.STYLE_DEFAULT);
	}
	
	
	// set button color
	private static String[] mItemArray = new String[]{"1","2","3","4","5","6","7","8","9","10"};
    public static void setButtonColor(RadioButton rBtn,int iBtnId)
    {
    	if(iBtnId%2 == 0)
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_dark);
    	else
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_light);
		rBtn.setBackgroundColor(ColorSet.mBG_ColorArray[iBtnId]);
		rBtn.setText(mItemArray[iBtnId]);
		rBtn.setTextColor(ColorSet.mText_ColorArray[iBtnId]);
    }
	
    // get current page style
	static public int getCurrentPageStyle(AppCompatActivity act,int page_pos)
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db = new DB_folder(focusFolder_tableId);
        return db.getPageStyle(page_pos, true);
	}

	// get style count
	static public int getStyleCount()
	{
		return ColorSet.mBG_ColorArray.length;
	}

    private static int ID_FOR_TABS = -1;
    public static int ID_FOR_NOTES = -2;
    /**
     * Get string with XML tags
     * @param tabPos tab position
     * @param noteId: ID_FOR_TABS for checked tabs(pages), ID_FOR_NOTES for checked notes
     * @return string with tags
     */
	public static String getStringWithXmlTag(AppCompatActivity act, Folder folder,int tabPos, long noteId)
	{
		String PAGE_TAG_B = "<page>";
		String PAGE_NAME_TAG_B = "<page_name>";
		String PAGE_NAME_TAG_E = "</page_name>";
		String NOTE_ITEM_TAG_B = "<note>";
		String NOTE_ITEM_TAG_E = "</note>";
		String TITLE_TAG_B = "<title>";
		String TITLE_TAG_E = "</title>";
		String BODY_TAG_B = "<body>";
		String BODY_TAG_E = "</body>";
		String AUDIO_TAG_B = "<audio>";
		String AUDIO_TAG_E = "</audio>";
		String PAGE_TAG_E = "</page>";

		String sentString = NEW_LINE;

		int pageTableId = folder.tabsHost.mTabsPagerAdapter.getItem(tabPos).page_tableId;
		List<Long> noteIdArray = new ArrayList<>();

		DB_page dbPage = new DB_page(pageTableId);
        dbPage.open();

        int count = dbPage.getNotesCount(false);

		if(noteId == ID_FOR_TABS)
		{
			for (int i = 0; i < count; i++)
                noteIdArray.add(i, dbPage.getNoteId(i, false));
		}
        else if(noteId == ID_FOR_NOTES)
        {
            // for checked notes
            int j=0;
            for (int i = 0; i < count; i++)
            {
                if(dbPage.getNoteMarking(i,false) == 1) {
                    noteIdArray.add(j, dbPage.getNoteId(i, false));
                    j++;
                }
            }
        }
		else
			noteIdArray.add(0, noteId);//only one for View note case

        dbPage.close();

		// when page has page name only, no notes
		if(noteIdArray.size() == 0)
		{
			sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
			sentString = sentString.concat(NEW_LINE + PAGE_NAME_TAG_B + mDbFolder.getCurrentPageTitle() + PAGE_NAME_TAG_E);
			sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_B);
			sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + TITLE_TAG_E);
			sentString = sentString.concat(NEW_LINE + BODY_TAG_B +  BODY_TAG_E);

			sentString = sentString.concat(NEW_LINE + AUDIO_TAG_B + AUDIO_TAG_E);
			sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_E);
			sentString = sentString.concat(NEW_LINE + PAGE_TAG_E );
			sentString = sentString.concat(NEW_LINE);
		}
		else
		{
			for(int i=0;i< noteIdArray.size();i++)
			{
				dbPage.open();

				Cursor cursorNote = dbPage.queryNote(noteIdArray.get(i));
                String title = cursorNote.getString(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_TITLE));
				title = replaceEscapeCharacter(title);

				String body = cursorNote.getString(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_BODY));
				body = replaceEscapeCharacter(body);

				String audioUrl = cursorNote.getString(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_AUDIO_URI));
				audioUrl = replaceEscapeCharacter(audioUrl);

				int mark = cursorNote.getInt(cursorNote.getColumnIndexOrThrow(DB_page.KEY_NOTE_MARKING));
				String srtMark = (mark == 1)? "[s]":"[n]";

				dbPage.close();

				if(i==0)
				{
					DB_folder db_folder = new DB_folder(Pref.getPref_focusView_folder_tableId(act));
					sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
					sentString = sentString.concat(NEW_LINE + PAGE_NAME_TAG_B + db_folder.getCurrentPageTitle() + PAGE_NAME_TAG_E );
				}

				sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_B);
				sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + srtMark + title + TITLE_TAG_E);
				sentString = sentString.concat(NEW_LINE + BODY_TAG_B + body + BODY_TAG_E);
				sentString = sentString.concat(NEW_LINE + AUDIO_TAG_B + audioUrl + AUDIO_TAG_E);
				sentString = sentString.concat(NEW_LINE + NOTE_ITEM_TAG_E);
				sentString = sentString.concat(NEW_LINE);
				if(i==noteIdArray.size()-1)
					sentString = sentString.concat(NEW_LINE +  PAGE_TAG_E);

			}
		}
		return sentString;
	}


    // replace special character (e.q. amp sign) for avoiding XML paring exception 
	//      &   &amp;
	//      >   &gt;
	//      <   &lt;
	//      '   &apos;
	//      "   &quot;
	private static String replaceEscapeCharacter(String str)
	{
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll(">", "&gt;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll("'", "&apos;");
        str = str.replaceAll("\"", "&quot;");
        return str;
	}
	
	// add XML tag
	public static String addXmlTag(String str)
	{
		String ENCODING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String XML_TAG_B = NEW_LINE + "<LiteNote>";
        String XML_TAG_E = NEW_LINE + "</LiteNote>";
        
        String data = ENCODING + XML_TAG_B;
        
        data = data.concat(str);
		data = data.concat(XML_TAG_E);
		
		return data;
	}

	// trim XML tag
	public String trimXMLtag(String string) {
		string = string.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>","");
		string = string.replace("<LiteNote>","");
		string = string.replace("<page>","");
		string = string.replace("<page_name>","=== Page: ");
		string = string.replace("</page_name>"," ===");
		string = string.replace("<note>","--- note ---");
		string = string.replace("[s]","");
		string = string.replace("[n]","");
		string = string.replace("<title></title>"+NEW_LINE,"");
        string = string.replace("<body></body>"+NEW_LINE,"");
        string = string.replace("<audio></audio>"+NEW_LINE,"");
		string = string.replace("<title>","Title: ");
		string = string.replace("</title>","");
		string = string.replace("<body>","Body: ");
		string = string.replace("</body>","");
		string = string.replace("<audio>","Audio: ");
		string = string.replace("</audio>","");		
		string = string.replace("</note>","");
		string = string.replace("</page>"," ");
		string = string.replace("</LiteNote>","");
		string = string.trim();
		return string;
	}
	
	
	// get local real path from URI
	public static String getLocalRealPathByUri(Context context, Uri contentUri) {
		  Cursor cursor = null;
		  try { 
		    String[] proj = { MediaStore.Images.Media.DATA };
		    cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
		    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    cursor.moveToFirst();
		    return cursor.getString(column_index);
		  }
		  catch (Exception e){
			return null;  
		  }
		  finally {
		    if (cursor != null) {
		      cursor.close();
		    }
		  }
	}
	
	// get display name by URI string
	public static String[] getDisplayNameByUriString(String uriString, Context activity) {
//		String display_name = "";
		String scheme = getUriScheme(uriString);
		String[] displayName = new String[2];

		if (Util.isEmptyString(uriString) || Util.isEmptyString(scheme)) {
		    displayName[0] = "";
		    displayName[1] = "";
			return displayName;
		}

		Uri uri = Uri.parse(uriString);
		//System.out.println("Uri string = " + uri.toString());
		//System.out.println("Uri last segment = " + uri.getLastPathSegment());
		if(scheme.equalsIgnoreCase("content"))
		{
	        String[] proj = { MediaStore.MediaColumns.DISPLAY_NAME };
	        Cursor cursor = null;
	        try{
	        	cursor = activity.getContentResolver().query(uri, proj, null, null, null);
	        }
	        catch (Exception e)
	        {
	        	Toast toast = Toast.makeText(activity, "Uri is not accessible", Toast.LENGTH_SHORT);
				toast.show();
	        }
	        
            if((cursor != null) && cursor.moveToFirst()) //reset the cursor
            {
                int col_index=-1;
                do
                {
                	col_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
	                displayName[0] = cursor.getString(col_index);
	                displayName[1] = "";
                }while(cursor.moveToNext());
                cursor.close();
            }
		}
		else if(scheme.equalsIgnoreCase("http") ||
				scheme.equalsIgnoreCase("https")   )
		{
            // if display name can not be displayed, then show last segment instead
			displayName[0] = uri.getLastPathSegment();
			displayName[1] = "";
		}
		else if(scheme.equalsIgnoreCase("file")  )
		{
			if(UtilAudio.hasAudioExtension(uriString))
			{
				// Get MP3 title from MP3 file
				String audio_artist = null;
				String audio_title = null;
				MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//				System.out.println("Util / _getDisplayNameByUriString / uri = " + uri);
				if(!Util.isEmptyString(uriString))
				{
					try
					{
						mmr.setDataSource(activity.getApplicationContext(),uri);
					}
					catch(Exception e)
					{

					}
					audio_title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
					audio_artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
					if(audio_artist == null) {
						displayName[0] = audio_title ;
						displayName[1] = "";
					}
					else {
						displayName[0] = audio_title;
						displayName[1] = audio_artist;
					}
				}

			}
			else {
				displayName[0] = uri.getLastPathSegment();
				displayName[1] = "";
			}
		}
//		System.out.println("displayName[0] = " + displayName[0]);
//		System.out.println("displayName[1] = " + displayName[1]);

		// MP3 with path but no title and no artist
		if(Util.isEmptyString(displayName[0]) && Util.isEmptyString(displayName[1]) &&UtilAudio.hasAudioExtension(uriString)) {
			displayName[0] = new File(uriString).getName();
			displayName[1] = "";
		}

        return displayName;
	}
	
	// get scheme by Uri string
	public static String getUriScheme(String string)
	{
 		Uri uri = Uri.parse(string);
		return uri.getScheme();
	}
	
	
	// is URI existed for Activity
	public static boolean isUriExisted(String uriString, Activity activity)
	{
		boolean bFileExist = false;
		if(!Util.isEmptyString(uriString))
		{
			Uri uri = Uri.parse(uriString);

			// when scheme is content and check local file
			File file = null;
			try
			{
				file = new File(uri.getPath());
			}
			catch(Exception e)
			{
				System.out.println("Util / _isUriExisted / local file not found exception");
			}

			if(file != null)
			{
				if(file.exists())
					bFileExist = true;
				else
                {
                    // for some file (eg. Universal Image Loader @#&=+-_.,!()~'%20.png ) ,_file.exists will return false,
                    // after _createNewFile, will create a file whose file size is zero, _file.exists will return true
                    try {
                        file.createNewFile();
//                        System.out.println("Util / _isUriExisted / 0 size file is created");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // check again after _createNewFile
                    bFileExist = file.exists() ? true: false;
                }
			}
			else
				bFileExist = false;

			// when scheme is content and check remote file
			if(!bFileExist)
			{
				try
				{
					ContentResolver cr = activity.getContentResolver();
					cr.openInputStream(uri);
					bFileExist = true;
				}
				catch (FileNotFoundException exception)
				{
					System.out.println("Util / _isUriExisted / remote file not found exception");
			    }
				catch (SecurityException se)
				{
					System.out.println("Util / _isUriExisted / remote security exception");
				}
				catch (Exception e)
				{
					System.out.println("Util / _isUriExisted / remote exception");
				}
			}
//			System.out.println("Util / _isUriExisted / bFileExist (content)= " + bFileExist);

			// when scheme is https or http
			try
			{
				// init
				if(Patterns.WEB_URL.matcher(uriString).matches())
					bFileExist = false;

				//enhance URL judgement
				String scheme  = Util.getUriScheme(uriString);
				if(scheme.equalsIgnoreCase("http")|| scheme.equalsIgnoreCase("https") )
				{
					if(Util.isNetworkConnected(activity))
					{
						try
						{
							boolean isEnd = false;
							int i = 0;
							while(!isEnd)
							{
								// check if network connection is OK
								Util.tryUrlConnection(uriString, activity);
								// wait for response
//								Thread.sleep(Util.oneSecond/10);
								Thread.sleep(Util.oneSecond/2);

								// check response
								if(200 <= Util.mResponseCode && Util.mResponseCode <= 399) {
									System.out.println("bFileExist 1 = " + bFileExist + " / count = " + i);
									bFileExist = true;
									isEnd = true;
								}
								else if (404 == Util.mResponseCode)
								{
									bFileExist = false;
									isEnd = true;
								}
								else {
									bFileExist = false;
									System.out.println("bFileExist = 2 " + bFileExist + " / count = " + i);
									i++;
									if (i == 3)
										isEnd = true; // no more try
								}
							}
						}
						catch (Exception e1)
						{
							e1.printStackTrace();
						}
					}
					else
						bFileExist =  false;
				}
			}
			catch (Exception e)
			{

		    }
//			System.out.println("Util / _isUriExisted / bFileExist (web url)= " + bFileExist);
		}
		return bFileExist;
	}
	
	// is Empty string
	public static boolean isEmptyString(String str)
	{
		boolean empty = true;
		if( str != null )
		{
			if(str.length() > 0 )
				empty = false;
		}
		return empty;
	}
	
    static String mStringUrl;
    public static int mResponseCode;
    static String mResponseMessage;
	public static int oneSecond = 1000;
    
	// check network connection
    public static boolean isNetworkConnected(Activity act)
    {
    	final ConnectivityManager conMgr = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
    	final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
    	if (activeNetwork != null && activeNetwork.isConnected()) {
    		System.out.println("network is connected");
    		return true;
    	} else {
    		System.out.println("network is NOT connected");
    		return false;
    	} 
    }
    
    // try Url connection
    protected static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    static public void tryUrlConnection(String strUrl, final Activity act) throws Exception 
    {
//    	mStringUrl = strUrl.replaceFirst("^https", "http");
    	mResponseCode = 0;
    	mStringUrl = strUrl;
    	Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() 
            {
        	    try
        	    {
        			String encodedUrl = Uri.encode(mStringUrl, ALLOWED_URI_CHARS);
        			HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
        			conn.setRequestMethod("HEAD");
        			conn.setConnectTimeout(oneSecond); // cause exception if connection error
        			conn.setReadTimeout(oneSecond*4);
        			mResponseCode = conn.getResponseCode();
        	        mResponseMessage = conn.getResponseMessage();
        	    } 
        	    catch (IOException exception) 
        	    {
        	    	mResponseCode = 0;
        	    	mResponseMessage = "io exception";
        	    	System.out.println("------------------ tryUrlConnection / io exception");
        	    	exception.printStackTrace();
				} 
        	    catch (Exception e) 
        	    {
        	    	System.out.println("------------------ tryUrlConnection / exception");
					e.printStackTrace();
				}
        	    System.out.println("Response Code : " + mResponseCode +
        	    				   " / Response Message: " + mResponseMessage );
            }
        });    	
    }    
    

	// get Url array of directory files
    public final static int AUDIO = 0;
    public static String[] getUrlsByFiles(File[] files,int type)
    {
        if(files == null)
        {
        	return null;
        }
        else
        {
        	String path[] = new String[files.length];
            int i=0;
            
	        for(File file : files)
	        {
		        if(  (type == AUDIO) && (UtilAudio.hasAudioExtension(file))   )
	            {
		            if(i< files.length)
		            {
//		            	path[i] = "file:///" + file.getPath();
		            	path[i] = "file://" + file.getAbsolutePath();
//		            	System.out.println("Util / _getUrlsByFiles / path[i] = " + path[i]);
		            	i++;
		            }
	            }
	        }
	        return path;
        }
    }		    
    
	// show saved file name
	public static void showSavedFileToast(Activity act,String string,int delay)
	{
		final Toast toast = Toast.makeText(act,
						string,
						Toast.LENGTH_SHORT);

        toast.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, delay);
	}

	static public boolean isLandscapeOrientation(Activity act)
	{
		int currentOrientation = act.getResources().getConfiguration().orientation; //todo Error java.lang.NullPointerException: Attempt to invoke virtual method 'android.content.res.Resources android.app.Activity.getResources()' on a null object reference

		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
			return true;
		else
			return false;
	}

	static public boolean isPortraitOrientation(Activity act)
	{
		int currentOrientation = act.getResources().getConfiguration().orientation;

		if (currentOrientation == Configuration.ORIENTATION_PORTRAIT)
			return true;
		else
			return false;
	}


	static public void lockOrientation(Activity act) {
	    int currentOrientation = act.getResources().getConfiguration().orientation;
	    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	    }
	    else {
		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
//		       act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	    }
	}

	static public void unlockOrientation(Activity act) {
	    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	// get time format string
	static public String getTimeFormatString(long duration)
	{
		long hour = TimeUnit.MILLISECONDS.toHours(duration);
		long min = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hour);
		long sec = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.HOURS.toSeconds(hour) - TimeUnit.MINUTES.toSeconds(min);
		String str = String.format(Locale.US,"%2d:%02d:%02d", hour, min, sec);
		return str;
	}
	

    @TargetApi(Build.VERSION_CODES.KITKAT)
	public static Intent chooseMediaIntentByType(Activity act,String type)
    {
	    // set multiple actions in Intent 
	    // Refer to: http://stackoverflow.com/questions/11021021/how-to-make-an-intent-with-multiple-actions
        PackageManager pkgMgr = act.getPackageManager();
		Intent intentSaf;
		Intent intent;
        Intent openInChooser;
        List<ResolveInfo> resInfoSaf;
		List<ResolveInfo> resInfo;
		List<LabeledIntent> intentList = new ArrayList<>();

        // SAF support starts from Kitkat
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
			// BEGIN_INCLUDE (use_open_document_intent)
	        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        	intentSaf = new Intent(Intent.ACTION_OPEN_DOCUMENT);

	        // Filter to only show results that can be "opened", such as a file (as opposed to a list
	        // of contacts or time zones)
        	intentSaf.addCategory(Intent.CATEGORY_OPENABLE);
        	intentSaf.setType(type);

        	// get extra SAF intents
	        resInfoSaf = pkgMgr.queryIntentActivities(intentSaf, 0);

	        for (int i = 0; i < resInfoSaf.size(); i++)
	        {
	            // Extract the label, append it, and repackage it in a LabeledIntent
	            ResolveInfo ri = resInfoSaf.get(i);
	            String packageName = ri.activityInfo.packageName;
				intentSaf.setComponent(new ComponentName(packageName, ri.activityInfo.name));

				// add span (CLOUD)
		        Spannable saf_span = new SpannableString(" (CLOUD)");
		        saf_span.setSpan(new ForegroundColorSpan(android.graphics.Color.RED), 0, saf_span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        CharSequence newSafLabel = TextUtils.concat(ri.loadLabel(pkgMgr), saf_span.toString());
//	        	System.out.println("Util / _chooseMediaIntentByType / SAF label " + i + " = " + newSafLabel );
//				extraIntentsSaf[i] = new LabeledIntent(intentSaf, packageName, newSafLabel, ri.icon);

				intentList.add(new LabeledIntent(intentSaf,packageName,newSafLabel,ri.icon));
	        }
        }
        
        // get extra non-SAF intents
		intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(type);
        resInfo = pkgMgr.queryIntentActivities(intent, 0);
        for (int i = 0; i < resInfo.size(); i++)
        { ResolveInfo ri = resInfo.get(i);
			String packageName = ri.activityInfo.packageName;
			intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
			intentList.add(new LabeledIntent(intent,packageName,ri.loadLabel(pkgMgr),ri.icon));
        }


        // remove duplicated item
        for(int i=0;i<intentList.size();i++)
		{
			ComponentName name1 = intentList.get(i).getComponent();
//			System.out.println("---> intentList.size() = " + intentList.size());
//			System.out.println("---> name1 = " + name1);
			for(int j=i+1;j<intentList.size();j++)
			{
				ComponentName name2 = intentList.get(j).getComponent();
//				System.out.println("---> intentList.size() = " + intentList.size());
//				System.out.println("---> name2 = " + name2);
				if( name1.equals(name2)) {
//					System.out.println("---> will remove");
					intentList.remove(j);
					j=intentList.size();
				}
			}
		}

		// check
		for(int i=0; i<intentList.size() ; i++)
		{
			System.out.println("--> intent list ("+ i +")" + intentList.get(i).toString());
		}
        
        // OK to put extra
        CharSequence charSeq = "";
        
		 if(type.startsWith("audio"))
        	charSeq = act.getResources().getText(R.string.add_new_chooser_audio);

		openInChooser = Intent.createChooser(intentList.remove(intentList.size()-1), charSeq);//remove duplicated item
		LabeledIntent[] extraIntentsFinal = intentList.toArray(new LabeledIntent[intentList.size()]);
        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntentsFinal);
                	
        return openInChooser;
    }

    public static void setScrollThumb(Context context, View view)
    {
		// Change scroll thumb by reflection
		// ref: http://stackoverflow.com/questions/21806852/change-the-color-of-scrollview-programmatically?lq=1
		try
		{
		    Field mScrollCacheField = View.class.getDeclaredField("mScrollCache");
		    mScrollCacheField.setAccessible(true);
		    Object mScrollCache = mScrollCacheField.get(view);

		    Field scrollBarField = mScrollCache.getClass().getDeclaredField("scrollBar");
		    scrollBarField.setAccessible(true);
		    Object scrollBar = scrollBarField.get(mScrollCache);

		    Method method = scrollBar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
		    method.setAccessible(true);
		    // Set drawable
		    method.invoke(scrollBar, context.getResources().getDrawable(R.drawable.fastscroll_thumb_default_holo));
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		}	    	
    }
    
	public static boolean isTimeUp;
	public static Timer longTimer;
	public synchronized static void setupLongTimeout(long timeout)
	{
	  if(longTimer != null) 
	  {
	    longTimer.cancel();
	    longTimer = null;
	  }
	  
	  if(longTimer == null) 
	  {
	    longTimer = new Timer();
	    longTimer.schedule(new TimerTask() 	{
											  public void run(){
												longTimer.cancel();
												longTimer = null;
												//do your stuff, i.e. finishing activity etc.
												isTimeUp = true;
											  }
											}, timeout /*in milliseconds*/);
					  }
	}

	// set full screen
	public static void setFullScreen(Activity act)
	{
//		System.out.println("Util / _setFullScreen");
		Window win = act.getWindow();
		
		if (Build.VERSION.SDK_INT < 16) 
		{ 
			win.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
						 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} 
		else 
		{
			//ref: https://stackoverflow.com/questions/28983621/detect-soft-navigation-bar-availability-in-android-device-progmatically
			Resources res = act.getResources();
			int id = res.getIdentifier("config_showNavigationBar", "bool", "android");
			boolean hasNavBar = ( id > 0 && res.getBoolean(id));

//			System.out.println("Util / _setFullScreen / hasNavBar = " + hasNavBar);

            // flags
            int uiOptions = //View.SYSTEM_UI_FLAG_LAYOUT_STABLE | //??? why this flag will add bottom offset
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

            if (Build.VERSION.SDK_INT >= 19)
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            //has navigation bar
			if(hasNavBar)
			{
				uiOptions = uiOptions
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			}

            View decorView = act.getWindow().getDecorView();
			decorView.setSystemUiVisibility(uiOptions);
		}
	}
	
	// set NOT full screen
	public static void setNotFullScreen(Activity act)
	{
//		System.out.println("Util / _setNotFullScreen");
        Window win = act.getWindow();
        
		if (Build.VERSION.SDK_INT < 16) 
		{ 
			win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			win.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
						 WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} 
		else 
		{
            // show the status bar and navigation bar
		    View decorView = act.getWindow().getDecorView();
//			int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;//top is overlaid by action bar
			int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;//normal
//			int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;//the usual system chrome is deemed too distracting.
//			int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;//full screen
            decorView.setSystemUiVisibility(uiOptions);
		}
	}


	// set full screen for no immersive sticky
	public static void setFullScreen_noImmersive(Activity act)
	{
		// show the status bar and navigation bar
		View decorView = act.getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;//full screen
		decorView.setSystemUiVisibility(uiOptions);
	}

	// set normal screen
	public static void setNormalScreen(Activity act)
	{
		View decorView = act.getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;//not full screen
		decorView.setSystemUiVisibility(uiOptions);
	}

	// Get external storage path for different devices
	public static String getDefaultExternalStoragePath(String path)
	{
		if(path.contains("/storage/emulated/0"))
			path = path.replace("/storage/emulated/0", Environment.getExternalStorageDirectory().getAbsolutePath() );
		else if( path.contains("/mnt/internal_sd"))
			path = path.replace("/mnt/internal_sd", Environment.getExternalStorageDirectory().getAbsolutePath());
		return path;
	}

	// check if WRITE_EXTERNAL_STORAGE permission is granted
	public static boolean willRequest_permission_WRITE_EXTERNAL_STORAGE(Activity act, int requestCode){

		int permissionWriteExtStorage = ActivityCompat.checkSelfPermission(act,
				Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if(permissionWriteExtStorage != PackageManager.PERMISSION_GRANTED ) {
			// request permission
			ActivityCompat.requestPermissions(act,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
										Manifest.permission.READ_EXTERNAL_STORAGE},
					requestCode);
			return true;
		}
		else {
			// granted
			return false;
		}
	}

}
