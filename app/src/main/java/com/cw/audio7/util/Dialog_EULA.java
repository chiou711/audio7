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
/**
 * This file provides simple End User License Agreement
 * It shows a simple dialog with the license text, and two buttons.
 * If user clicks on 'cancel' button, app closes and user will not be granted access to app.
 * If user clicks on 'accept' button, app access is allowed and this choice is saved in preferences
 * so next time this will not show, until next upgrade.
 */
 
import com.cw.audio7.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class Dialog_EULA {

    private String EULA_PREFIX = "appEULA";
    private AppCompatActivity mAct;
    private SharedPreferences prefs;
    private String eulaKey;
    public DialogInterface.OnClickListener clickListener_Ok;
    public DialogInterface.OnClickListener clickListener_ReadAgreement;
    public DialogInterface.OnClickListener clickListener_No;
    public DialogInterface.OnClickListener clickListener_back;

    private String title;
    public String agreement;
    public String welcome;

    public Dialog_EULA(AppCompatActivity act ){
        mAct = act;
        // EULA title
        title = mAct.getString(R.string.app_name) +
                " v" +
                getPackageInfo().versionName;

        // EULA text
        agreement = mAct.getString(R.string.EULA_string);
        welcome = mAct.getString(R.string.welcome_string);
    }

    public boolean isEulaAlreadyAccepted() {
        PackageInfo versionInfo = getPackageInfo();
        prefs= PreferenceManager.getDefaultSharedPreferences(mAct);

        // The eulaKey changes every time you increment the version number in
        // the AndroidManifest.xml
        eulaKey = EULA_PREFIX + versionInfo.versionCode;

        return prefs.getBoolean(eulaKey, false); //default false
    }

    private PackageInfo getPackageInfo() {
        PackageInfo info = null;
        try {
            info = mAct.getPackageManager().getPackageInfo(
                    mAct.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return info;
    }
 
    public void applyPreference()
    {
        // Mark this version as read.
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(eulaKey, true);
        editor.apply();
    }

    public void show()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mAct)
                .setTitle(title)
                .setMessage(welcome)
                .setCancelable(false)
                .setNeutralButton(android.R.string.cancel,clickListener_No)
                .setNegativeButton(R.string.read_agreement, clickListener_ReadAgreement)
                .setPositiveButton(R.string.accept, clickListener_Ok);
        builder.create().show();
    }

    public void show_read_agreement()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mAct)
                .setTitle(title)
                .setMessage(agreement)
                .setCancelable(true)
                .setNegativeButton(R.string.btn_back,clickListener_back);
        builder.create().show();
    }
}