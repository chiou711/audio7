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

import java.io.File;
import java.io.FilenameFilter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

public class DeleteFileAlarmReceiver extends BroadcastReceiver 
{
   private static final String EXTRA_FILENAME = "com.cwc.audio7.extras.filename";

   public DeleteFileAlarmReceiver(){}

   public DeleteFileAlarmReceiver(Context context, long timeMilliSec, String[] filename)
   {
   	    for(int i=0; i<filename.length;i++) {
			Intent intent = new Intent(context, DeleteFileAlarmReceiver.class);
			intent.putExtra(EXTRA_FILENAME, filename[i]);

			AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
			alarmMgr.set(AlarmManager.RTC_WAKEUP, timeMilliSec, pendIntent);
		}
   }

   @Override
   public void onReceive(final Context context, Intent intent) 
   {
	   System.out.println("DeleteFileAlarmReceiver / _onReceive");
		// Note: if launch Send mail twice, file name founded is the first one, not the second one
	    // so, delete any file starts with audio7_SEND and ends with txt
		
	    // SD card path + "/" + directory path
	    String folderString = Environment.getExternalStorageDirectory().toString() + 
	    		              "/" + 
        					  Util.getStorageDirName(context);
	    File folder = new File( folderString);       
		
		File[] files = folder.listFiles( new FilenameFilter() 
		{
		    @Override
		    public boolean accept( final File dir,
		                           final String name ) {
//		        return name.matches( "audio7_SEND.*\\.txt" ); // starts with audio7_SEND, ends with txt
//		        return name.matches( ".*\\.txt" ); // end with txt
//		        return name.matches("(audio7_SEND.+(\\.(?i)(txt))$)" );
				boolean isMatch = false;
				if(name.matches("("+ Util.getStorageDirName(context)+"_SEND.+(\\.(?i)(xml))$)" ) ||
				   name.matches("("+ Util.getStorageDirName(context)+"_SEND.+(\\.(?i)(txt))$)" )    )
				{
					isMatch = true;
				}
		        return isMatch;
		    }
		} );
		for ( final File fileFound : files )
		{
			System.out.println("fileFound = " + fileFound.getName());
		    if ( !fileFound.delete() )
			{
		        System.err.println( "Can't remove " + fileFound.getAbsolutePath() );
		    }
		}
   }
}
