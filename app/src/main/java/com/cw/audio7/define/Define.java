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

package com.cw.audio7.define;

import android.content.Context;

import com.cw.audio7.R;

/**
 * Created by CW on 2016/6/16.
 * Modified by CW on 2018/11/07
 *
 * build apk file size:
 * 1) prefer w/ assets files: 15,483 KB
 *
 * 2) default w/ assets files: 15,483 KB
 *
 * 3) default w/o assets files: 1,173 KB
 *
 * 4) release: 706 KB
 */
public class Define {

    public static int getAppBuildMode() {
        return app_build_mode;
    }

    //
    public final static int DEBUG_DEFAULT_BY_INITIAL = 0;
    public final static int DEBUG_DEFAULT_BY_ASSETS = 1;
    public final static int DEBUG_DEFAULT_BY_DOWNLOAD = 2;
    public final static int RELEASE_DEFAULT_BY_INITIAL = 3;
    public final static int RELEASE_DEFAULT_BY_ASSETS = 4;
    public final static int RELEASE_DEFAULT_BY_DOWNLOAD = 5;
    public final static String  DB_FILE_NAME = "audio7.db";

    public static void setAppBuildMode()
    {
        /**
         * Set APP build mode
         * Note:
         *  1. for AdMob: it works after Google Play store release
         *  2. for assets mode: need to enable build.gradle assets.srcDirs = ['preferred/assets/']
         */

        /** 1 debug, initial */
        int mode = DEBUG_DEFAULT_BY_INITIAL;

        /** 2 release, initial */
//        int mode  =  Define.RELEASE_DEFAULT_BY_INITIAL;

        setAppBuildMode(mode);
    }

    private static void setAppBuildMode(int appBuildMode) {
        app_build_mode = appBuildMode;

        switch (appBuildMode)
        {
            case DEBUG_DEFAULT_BY_INITIAL:
                CODE_MODE = DEBUG_MODE;
                DEFAULT_CONTENT = BY_INITIAL_TABLES;
                INITIAL_FOLDERS_COUNT = 2;  // Folder1, Folder2
                INITIAL_PAGES_COUNT = 1;// Page1_1
                break;

            case DEBUG_DEFAULT_BY_ASSETS:
                CODE_MODE = DEBUG_MODE;
                DEFAULT_CONTENT = BY_ASSETS;
                break;

            case DEBUG_DEFAULT_BY_DOWNLOAD:
                CODE_MODE = DEBUG_MODE;
                DEFAULT_CONTENT = BY_DOWNLOAD;
                break;

            case RELEASE_DEFAULT_BY_INITIAL:
                CODE_MODE = RELEASE_MODE;
                DEFAULT_CONTENT = BY_INITIAL_TABLES;
                INITIAL_FOLDERS_COUNT = 2;  // Folder1, Folder2
                INITIAL_PAGES_COUNT = 1;// Page1_1
                break;

            case RELEASE_DEFAULT_BY_ASSETS:
                CODE_MODE = RELEASE_MODE;
                DEFAULT_CONTENT = BY_ASSETS;
                break;

            case RELEASE_DEFAULT_BY_DOWNLOAD:
                CODE_MODE = RELEASE_MODE;
                DEFAULT_CONTENT = BY_DOWNLOAD;
                break;

            default:
                break;
        }
    }

    public static int app_build_mode = 0;

    /***************************************************************************
     * Set release/debug mode
     * - RELEASE_MODE
     * - DEBUG_MODE
     ***************************************************************************/
    public static int CODE_MODE;// = DEBUG_MODE; //DEBUG_MODE; //RELEASE_MODE;
    public static int DEBUG_MODE = 0;
    public static int RELEASE_MODE = 1;


    /****************************************************************************
     *
     * Flags for Default tables after App installation:
     * - default content: DEFAULT_CONTENT
     *      - by initial tables: INITIAL_FOLDERS_COUNT, INITIAL_PAGES_COUNT
     *      - by assets XML
     *      - by download XML
     * Note of flag setting: exclusive
     *
     * With default content
     * - true : un-mark preferred/assets/ line in build.gradle file
     * - false:    mark preferred/assets/ line in build.gradle file
     *
     * android {
     * ...
     *    sourceSets {
     *        main {
     *      // mark: W/O default content
     *      // un-mark: With default content
     *      // Apk file size will increase if assets directory is set at default location (src/main/assets)
     *           assets.srcDirs = ['preferred/assets/']
     *      }
     *    }
     * }
     *
     ************************************************************************************************************/

    /***
     *  With default content by XML file
     */
    public static int DEFAULT_CONTENT;
    // by none
    public static int BY_INITIAL_TABLES = 0;
    // by assets XML file
    public static int BY_ASSETS = 1;
    // by downloaded XML file
    public static int BY_DOWNLOAD = 2;

    /**
     * With initial tables: table count
     * - folder count: 2
     * - page count: 1
     */
    // initial table count
    public static int INITIAL_FOLDERS_COUNT;
    public static int INITIAL_PAGES_COUNT;

    /***************************************************************************
     * Enable AdMob at page bottom
     *
     ***************************************************************************/
    public static boolean ENABLE_ADMOB = true; //true; //false;


    // Apply system default for picture path
    public static boolean PICTURE_PATH_BY_SYSTEM_DEFAULT = true;


    // default style
    public static int STYLE_DEFAULT = 1;
    public static int STYLE_PREFER = 2;

    public static String getTabTitle(Context context, Integer Id)
    {
        String title;

        if(Define.DEFAULT_CONTENT != Define.BY_INITIAL_TABLES) {
            title = context.getResources().getString(R.string.prefer_page_name).concat(String.valueOf(Id));
        }
        else {
            title = context.getResources().getString(R.string.default_page_name).concat(String.valueOf(Id));
        }
        return title;
    }

}
