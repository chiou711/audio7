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

package com.cw.audio7.main;

import com.cw.audio7.R;

/**
 * Created by cw on 2017/10/7.
 */

public class MenuId {
        /***
         * Menu identifiers
         */
        // add new
        public static final int ADD_NEW_NOTE = R.id.ADD_NEW_NOTE;

        // play
        public static final int OPEN_PLAY_SUBMENU = R.id.PLAY;
        public static final int PLAY_OR_STOP_AUDIO = R.id.PLAY_OR_STOP_MUSIC;

        // cyclic play
        public static final int PLAY_CYCLIC = R.id.PLAY_CYCLIC;

        // background play
        public static final int PLAY_BACKGROUND = R.id.PLAY_BACKGROUND;

        // checked operation
        public static final int CHECKED_OPERATION = R.id.HANDLE_CHECKED_NOTES;

        // note operation
        public static final int ENABLE_NOTE_LARGE_VIEW = R.id.ENABLE_NOTE_LARGE_VIEW;
        public static final int ENABLE_NOTE_SELECT = R.id.ENABLE_NOTE_SELECT;
        public static final int ENABLE_NOTE_DRAG_AND_DROP = R.id.ENABLE_NOTE_DRAG_AND_DROP;

        // page operation
        public static final int ADD_NEW_PAGE = R.id.ADD_NEW_PAGE;
        public static final int CHANGE_PAGE_COLOR = R.id.CHANGE_PAGE_COLOR;
        public static final int SHIFT_PAGE = R.id.SHIFT_PAGE;
        public static final int DELETE_PAGES = R.id.DELETE_PAGES;

        // operation
//        public static final int EXPORT_TO_SD_CARD = R.id.EXPORT_TO_SD_CARD;
//        public static final int IMPORT_FROM_SD_CARD = R.id.IMPORT_FROM_SD_CARD;

        // config
        public static final int CONFIG = R.id.CONFIG;

        // about
        public static final int ABOUT = R.id.ABOUT;

        // folder operation
        public static  final int ADD_NEW_FOLDER = R.id.ADD_NEW_FOLDER;
        public static final int ENABLE_FOLDER_DRAG_AND_DROP = R.id.ENABLE_FOLDER_DRAG_AND_DROP;
        public static final int DELETE_FOLDERS = R.id.DELETE_FOLDERS;

        // pager menu
        public static  final int VIEW_NOTE_CHECK = R.id.VIEW_NOTE_CHECK;
        public static  final int VIEW_NOTE_EDIT = R.id.VIEW_NOTE_EDIT;

        // recording menu
        public static  final int ACTION_SETTINGS = R.id.ACTION_SETTINGS;
}
