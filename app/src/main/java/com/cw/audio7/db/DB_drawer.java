/*
 * Copyright (C) 2021 CW Chiu
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

import static com.cw.audio7.main.MainAct.dbHelper;

/**
 *  Data Base Class for Drawer
 *
 */
public class DB_drawer
{

    final private Context context;
    private SQLiteDatabase sqlDb;

    // Table name format: Drawer
    static String DB_DRAWER_TABLE_NAME = "Drawer";

	// Table name format: Folder1
	final private static String DB_FOLDER_TABLE_PREFIX = "Folder";

	// Folder rows
    static final String KEY_FOLDER_ID = "folder_id"; //can rename _id for using BaseAdapter
    static final String KEY_FOLDER_TABLE_ID = "folder_table_id"; //can rename _id for using BaseAdapter
    public static final String KEY_FOLDER_TITLE = "folder_title";
    static final String KEY_FOLDER_CREATED = "folder_created";

	// Cursor
	public Cursor cursor_folder;

    /** Constructor */
	public DB_drawer(Context context)
    {
        this.context = context;
    }

    /**
     * DB functions
     *
     */
	public DB_drawer open() throws SQLException
	{
		// Will call DatabaseHelper.onCreate()first time when WritableDatabase is not created yet
		try {
			sqlDb = dbHelper.getWritableDatabase();
			cursor_folder = this.getFolderCursor();
			return DB_drawer.this;
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	public void close()
	{
        if((cursor_folder != null) && (!cursor_folder.isClosed()))
            cursor_folder.close();
		dbHelper.close();
	}

    // delete DB
	public void deleteDB()
	{
        sqlDb = dbHelper.getWritableDatabase();
        try {
	    	sqlDb.beginTransaction();
	        context.deleteDatabase(DatabaseHelper.DB_NAME);
	        sqlDb.setTransactionSuccessful();
	    }
	    catch (Exception e) {
        	e.printStackTrace();
	    }
	    finally {
	    	Toast.makeText(context,R.string.config_delete_DB_toast,Toast.LENGTH_SHORT).show();
	    	sqlDb.endTransaction();
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
        sqlDb.execSQL(DB_CREATE);

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
        sqlDb.execSQL(dB_drop_table);
        if(enDbOpenClose)
            this.close();
    }


	/*
	 * Drawer table columns for folder row
	 *
	 *
	 */
    final private String[] strFolderColumns = new String[] {
        KEY_FOLDER_ID + " AS " + BaseColumns._ID,
			KEY_FOLDER_TABLE_ID,
			KEY_FOLDER_TITLE,
			KEY_FOLDER_CREATED
      };


    public Cursor getFolderCursor() {
        return sqlDb.query(DB_DRAWER_TABLE_NAME,
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
        long rowId = sqlDb.insert(DB_DRAWER_TABLE_NAME, null, args);
        if(enDbOpenClose)
            this.close();
        return rowId;
    }

    public long deleteFolderId(int id,boolean enDbOpenClose)
    {
        if(enDbOpenClose)
            this.open();
        long rowsNumber = sqlDb.delete(DB_DRAWER_TABLE_NAME, KEY_FOLDER_ID + "='" + id +"'", null);
        if(enDbOpenClose)
            this.close();
        return  rowsNumber;
    }
    
    
    // update folder
    public void  updateFolder(long rowId, int drawerFolderTableId, String drawerTitle,boolean enDbOpenClose) {
        if(enDbOpenClose)
            this.open();
        ContentValues args = new ContentValues();
        Date now = new Date();  
        args.put(KEY_FOLDER_TABLE_ID, drawerFolderTableId);
        args.put(KEY_FOLDER_TITLE, drawerTitle);
       	args.put(KEY_FOLDER_CREATED, now.getTime());

        sqlDb.update(DB_DRAWER_TABLE_NAME, args, KEY_FOLDER_ID + "=" + rowId, null);
        if(enDbOpenClose)
            this.close();
    }
    
    public long getFolderId(int position,boolean enDbOpenClose)
    {
        if(enDbOpenClose)
            this.open();
    	cursor_folder.moveToPosition(position);
    	// note: KEY_FOLDER_ID + " AS " + BaseColumns._ID
        long column = -1;
        try {
            column = (long) cursor_folder.getInt(cursor_folder.getColumnIndex(BaseColumns._ID));
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

        int count = 0;
        if(cursor_folder != null)
	        count = cursor_folder.getCount();

        if(enDbOpenClose)
            this.close();
    	return count;
    }
    
    public int getFolderTableId(int position,boolean enDbOpenClose)
    {
        if(enDbOpenClose)
            this.open();
        cursor_folder.moveToPosition(position);
        int id = cursor_folder.getInt(cursor_folder.getColumnIndex(KEY_FOLDER_TABLE_ID));

        if(enDbOpenClose)
            this.close();
        return id;

    }

	public String getFolderTitle(int position,boolean enDbOpenClose)
	{
        if(enDbOpenClose)
            this.open();
		cursor_folder.moveToPosition(position);
        String str="";
        try {
            str = cursor_folder.getString(cursor_folder.getColumnIndex(KEY_FOLDER_TITLE));
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