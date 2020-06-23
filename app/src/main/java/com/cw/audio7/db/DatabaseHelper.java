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
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cw.audio7.define.Define;


// Data Base Helper 
class DatabaseHelper extends SQLiteOpenHelper
{
    static final String DB_NAME = Define.DB_FILE_NAME;
    private static int DB_VERSION = 1;
    
    DatabaseHelper(Context context)
    {
        super(context, DB_NAME , null, DB_VERSION);
    }

    @Override
    //Called when the database is created ONLY for the first time.
    public void onCreate(SQLiteDatabase sqlDb)
    {   
    	String tableCreated;
    	String DB_CREATE;
    	
    	System.out.println("DatabaseHelper / _onCreate");

		// Create Drawer table
		tableCreated = DB_drawer.DB_DRAWER_TABLE_NAME;
		DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
				DB_drawer.KEY_FOLDER_ID + " INTEGER PRIMARY KEY," +
				DB_drawer.KEY_FOLDER_TABLE_ID + " INTEGER," +
				DB_drawer.KEY_FOLDER_TITLE + " TEXT," +
				DB_drawer.KEY_FOLDER_CREATED + " INTEGER);";
		sqlDb.execSQL(DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    { //how to upgrade?
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//        System.out.println("DatabaseHelper / _onUpgrade DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }
    
    @Override
    public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion)
    { 
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//        System.out.println("DatabaseHelper / _onDowngrade / DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }

}
