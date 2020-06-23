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

package com.cw.audio7.db;

import java.util.Date;

import com.cw.audio7.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.widget.Toast;

/**
 *  Data Base Class for Drawer
 *
 */
public class DB_drawer
{

    private Context mContext = null;
    private static DatabaseHelper mDbHelper ;
    private SQLiteDatabase mSqlDb;

    // Table name format: Drawer
    static String DB_DRAWER_TABLE_NAME = "Drawer";

	// Table name format: Folder1
	private static String DB_FOLDER_TABLE_PREFIX = "Folder";

	// Folder rows
    static final String KEY_FOLDER_ID = "folder_id"; //can rename _id for using BaseAdapter
    static final String KEY_FOLDER_TABLE_ID = "folder_table_id"; //can rename _id for using BaseAdapter
    public static final String KEY_FOLDER_TITLE = "folder_title";
    static final String KEY_FOLDER_CREATED = "folder_created";

	// Cursor
	public static Cursor mCursor_folder;


    /** Constructor */
	public DB_drawer(Context context)
    {
        mContext = context;
    }

    /**
     * DB functions
     *
     */
	public DB_drawer open() throws SQLException
	{
		mDbHelper = new DatabaseHelper(mContext);

		// Will call DatabaseHelper.onCreate()first time when WritableDatabase is not created yet
		mSqlDb = mDbHelper.getWritableDatabase();
        mCursor_folder = this.getFolderCursor();
		return DB_drawer.this;
	}

	public void close()
	{
        if((mCursor_folder != null) && (!mCursor_folder.isClosed()))
            mCursor_folder.close();
		mDbHelper.close();
	}

    // delete DB
	public void deleteDB()
	{
        mSqlDb = mDbHelper.getWritableDatabase();
        try {
	    	mSqlDb.beginTransaction();
	        mContext.deleteDatabase(DatabaseHelper.DB_NAME);
	        mSqlDb.setTransactionSuccessful();
	    }
	    catch (Exception e) {
	    }
	    finally {
	    	Toast.makeText(mContext,R.string.config_delete_DB_toast,Toast.LENGTH_SHORT).show();
	    	mSqlDb.endTransaction();
	    }
	}

    // insert folder table
    public void insertFolderTable(int id, boolean enDbOpenClose)
    {
    	if(enDbOpenClose)
            this.open();

    	// table for folder
		String tableCreated = DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(id));
        String DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
				DB_folder.KEY_PAGE_ID + " INTEGER PRIMARY KEY," +
				DB_folder.KEY_PAGE_TITLE + " TEXT," +
				DB_folder.KEY_PAGE_TABLE_ID + " INTEGER," +
				DB_folder.KEY_PAGE_STYLE + " INTEGER," +
				DB_folder.KEY_PAGE_CREATED + " INTEGER);";
        mSqlDb.execSQL(DB_CREATE);

		if(enDbOpenClose)
            this.close();
    }

    // delete folder table
    public void dropFolderTable(int tableId,boolean enDbOpenClose)
    {
    	if(enDbOpenClose)
    	    this.open();
		//format "Folder1"
    	String DB_FOLDER_TABLE_NAME = DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(tableId));
        String dB_drop_table = "DROP TABLE IF EXISTS " + DB_FOLDER_TABLE_NAME + ";";
        mSqlDb.execSQL(dB_drop_table);
        if(enDbOpenClose)
            this.close();
    }


	/*
	 * Drawer table columns for folder row
	 *
	 *
	 */
    private String[] strFolderColumns = new String[] {
        KEY_FOLDER_ID + " AS " + BaseColumns._ID,
			KEY_FOLDER_TABLE_ID,
			KEY_FOLDER_TITLE,
			KEY_FOLDER_CREATED
      };


    public Cursor getFolderCursor() {
        return mSqlDb.query(DB_DRAWER_TABLE_NAME,
				 strFolderColumns,
				 null,
				 null,
				 null,
				 null,
				 null
				 );
    }

    public long insertFolder(int tableId, String title,boolean enDbOpenClose)
    {
        System.out.println("DB_drawer / _insertFolder/ tableId = " + tableId + " / title = " + title);
        if(enDbOpenClose)
            this.open();
        Date now = new Date();
        ContentValues args = new ContentValues();
        args.put(KEY_FOLDER_TABLE_ID, tableId);
        args.put(KEY_FOLDER_TITLE, title);
        args.put(KEY_FOLDER_CREATED, now.getTime());
        long rowId = mSqlDb.insert(DB_DRAWER_TABLE_NAME, null, args);
        if(enDbOpenClose)
            this.close();
        return rowId;
    }

    public long deleteFolderId(int id,boolean enDbOpenClose)
    {
        if(enDbOpenClose)
            this.open();
        long rowsNumber = mSqlDb.delete(DB_DRAWER_TABLE_NAME, KEY_FOLDER_ID + "='" + id +"'", null);
        if(enDbOpenClose)
            this.close();
        return  rowsNumber;
    }
    
    
    // update folder
    public boolean updateFolder(long rowId, int drawerFolderTableId, String drawerTitle,boolean enDbOpenClose) {
        if(enDbOpenClose)
            this.open();
        ContentValues args = new ContentValues();
        Date now = new Date();  
        args.put(KEY_FOLDER_TABLE_ID, drawerFolderTableId);
        args.put(KEY_FOLDER_TITLE, drawerTitle);
       	args.put(KEY_FOLDER_CREATED, now.getTime());

        int cUpdateItems = mSqlDb.update(DB_DRAWER_TABLE_NAME, args, KEY_FOLDER_ID + "=" + rowId, null);
        boolean bUpdate = cUpdateItems > 0? true:false;
        if(enDbOpenClose)
            this.close();
        return bUpdate;
    }    
    
    public long getFolderId(int position,boolean enDbOpenClose)
    {
        if(enDbOpenClose)
            this.open();
    	mCursor_folder.moveToPosition(position);
    	// note: KEY_FOLDER_ID + " AS " + BaseColumns._ID
        long column = -1;
        try {
            column = (long) mCursor_folder.getInt(mCursor_folder.getColumnIndex(BaseColumns._ID));
        }
        catch (Exception e) {
            System.out.println("DB_drawer / _getFolderId / exception ");
        }
        if(enDbOpenClose)
            this.close();
        return column;
    }
    
    public int getFoldersCount(boolean enDbOpenClose)
    {
        if(enDbOpenClose)
            this.open();
    	int count = mCursor_folder.getCount();
        if(enDbOpenClose)
            this.close();
    	return count;
    }
    
    public int getFolderTableId(int position,boolean enDbOpenClose)
    {
        if(enDbOpenClose)
            this.open();
        mCursor_folder.moveToPosition(position);
        int id = mCursor_folder.getInt(mCursor_folder.getColumnIndex(KEY_FOLDER_TABLE_ID));

        if(enDbOpenClose)
            this.close();
        return id;

    }

	public String getFolderTitle(int position,boolean enDbOpenClose)
	{
        if(enDbOpenClose)
            this.open();
		mCursor_folder.moveToPosition(position);
        String str="";
        try {
            str = mCursor_folder.getString(mCursor_folder.getColumnIndex(KEY_FOLDER_TITLE));
        }
        catch (Exception e)
        {
            System.out.println("DB_drawer / _getFolderTitle / getString error / empty string");
        }
        if(enDbOpenClose)
            this.close();
        return str;
	}

    public void listFolders()
    {
        int count = getFoldersCount(true);
        System.out.println("DB_drawer / _listFolders / folders count = " + count);
        for (int i = 0; i < count; i++)
        {
            String title = getFolderTitle(i,true);
            System.out.println("DB_drawer / _listFolders / position = " + i + " / folder title = " + title);
        }
    }
}