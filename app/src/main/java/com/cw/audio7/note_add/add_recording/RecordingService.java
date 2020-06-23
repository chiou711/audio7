package com.cw.audio7.note_add.add_recording;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.db.DB_page;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimerTask;

public class RecordingService extends Service {

    private static final String LOG_TAG = "RecordingService";

    private String mFileName = null;
    private String mFilePath = null;

    private MediaRecorder mRecorder = null;

    private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private TimerTask mIncrementTimerTask = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRecording();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mRecorder != null) {
            stopRecording();
        }

        super.onDestroy();
    }

    public void startRecording() {
        setFileNameAndPath();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);

        if(Pref.getPref_recorder_high_quality(Add_recording_act.mAct))
        {
//            System.out.println("RecordingService / _startRecording / high quality is true");
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setAudioEncodingBitRate(192000);
        }
//        else
//            System.out.println("RecordingService / _startRecording / high quality is false");


        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    public void setFileNameAndPath(){
        String timeStr = Util.getCurrentTimeString();
        File f;

        do{
            mFileName = getString(R.string.default_file_name)
                    + "_" + timeStr + ".mp3";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/" + getString(R.string.app_name_dir) + "/RecordingNote"+ "/" + mFileName;

            f = new File(mFilePath);
        }while (f.exists() && !f.isDirectory());
    }

    public void stopRecording() {
        try {
            mRecorder.stop();
            mRecorder.release();
        } catch (Exception e) {
            Log.e(LOG_TAG, "stop() failed");
        }

        //remove notification
        if (mIncrementTimerTask != null) {
            mIncrementTimerTask.cancel();
            mIncrementTimerTask = null;
        }

        mRecorder = null;

        try {
            System.out.println("RecordingService / _stopRecording / mFilePath = " + mFilePath);
            DB_page dB;
            Long noteId;
            String audioUriInDB;
            Toast.makeText(this, getString(R.string.toast_recording_finish) + " " + mFilePath, Toast.LENGTH_SHORT).show();

            dB = new DB_page(this, TabsHost.getCurrentPageTableId());

            noteId = null; // set null for Insert
            audioUriInDB = "file://" + mFilePath;
            if( !Util.isEmptyString(audioUriInDB))
            {
                // insert
                // set marking to 1 for default
                noteId = dB.insertNote("", "", audioUriInDB, "", "", "", 1, (long) 0);// add new note, get return row Id
                System.out.println("RecordingService / _stopRecording / noteId = " + noteId);
            }

        } catch (Exception e){
            Log.e(LOG_TAG, "exception", e);
        }
    }
}
