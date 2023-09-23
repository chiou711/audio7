package com.cw.audio7.note_add.add_recording;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.cw.audio7.R;
import com.cw.audio7.main.MenuId;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

//refer https://github.com/dkim0419/SoundRecorder

public class Add_recording_act extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    public static AppCompatActivity mAct;
    public static FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;
    FragmentManager fm;

    public Add_recording_act() {
        System.out.println("Add_recording_act / constructor");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Add_recording_act / _onCreate");

        setContentView(R.layout.add_recording_act);

        initActionBar(R.string.note_recording);

        mAct = this;

        // add on back stack changed listener
        mOnBackStackChangedListener = this;
        getSupportFragmentManager().addOnBackStackChangedListener(mOnBackStackChangedListener);

        // recorder fragment
        Add_recording rf = new Add_recording();
        fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
        ft.replace(R.id.container, rf).commit();
        fm.executePendingTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    0 /*BuildDev.RECORD_AUDIO*/);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recording_menu, menu);

        if(fm.getBackStackEntryCount() == 1 )
            menu.findItem(R.id.ACTION_SETTINGS).setVisible(false);
        else
            menu.findItem(R.id.ACTION_SETTINGS).setVisible(true);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Go back: check if Configure fragment now
        if( (item.getItemId() == android.R.id.home ))
        {
            if(fm.getBackStackEntryCount() > 0 )
            {
                fm.popBackStack();
				initActionBar(R.string.note_recording);
                return true;
            }
            else
	            super.onBackPressed();
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case MenuId.ACTION_SETTINGS:

	            setContentView(R.layout.add_recording_act);

				initActionBar(R.string.action_settings);

                RecordingSetting cf = new RecordingSetting();
                FragmentTransaction ft = fm.beginTransaction();
                ft.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                ft.replace(R.id.container, cf).addToBackStack("recorder_setting").commit();
                fm.executePendingTransactions();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void initActionBar(int resId)
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.recorder_toolbar);
        toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        if (toolbar != null)
            setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
	    if (actionBar != null) {
		    actionBar.setTitle(resId);
		    actionBar.setDisplayHomeAsUpEnabled(true);
		    actionBar.setDisplayShowHomeEnabled(true);
	    }
    }

    @Override
    public void onBackStackChanged() {
        int backStackEntryCount = fm.getBackStackEntryCount();
        System.out.println("Add_recording_act / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1)
        {
            System.out.println("Add_recording_act / _onBackStackChanged / RecordingSetting");
        }
        else if(backStackEntryCount == 0) // init
        {
            System.out.println("Add_recording_act / _onBackStackChanged / Add_recording");
            initActionBar(R.string.note_recording);
        }
    }

    // callback of granted permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        System.out.println("Add_recording_act / _onRequestPermissionsResult / grantResults.length =" + grantResults.length);
        switch (requestCode) {
            case 0:
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                } else
                    finish();
                break;
        }
    }

}