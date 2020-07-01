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
        static final int ADD_NEW_NOTE = R.id.ADD_NEW_NOTE;

        // play
        static final int OPEN_PLAY_SUBMENU = R.id.PLAY;
        static final int PLAY_OR_STOP_AUDIO = R.id.PLAY_OR_STOP_MUSIC;

        // checked operation
        static final int CHECKED_OPERATION = R.id.HANDLE_CHECKED_NOTES;

        // note operation
        static final int ENABLE_NOTE_SELECT = R.id.ENABLE_NOTE_SELECT;
        static final int ENABLE_NOTE_DRAG_AND_DROP = R.id.ENABLE_NOTE_DRAG_AND_DROP;

        // page operation
        static final int ADD_NEW_PAGE = R.id.ADD_NEW_PAGE;
        static final int CHANGE_PAGE_COLOR = R.id.CHANGE_PAGE_COLOR;
        static final int SHIFT_PAGE = R.id.SHIFT_PAGE;
        static final int DELETE_PAGES = R.id.DELETE_PAGES;

        // operation
        static final int SEND_PAGES = R.id.SEND_PAGES;
        static final int EXPORT_TO_SD_CARD = R.id.EXPORT_TO_SD_CARD;
        static final int IMPORT_FROM_SD_CARD = R.id.IMPORT_FROM_SD_CARD;

        // config
        static final int CONFIG = R.id.CONFIG;

        // about
        static final int ABOUT = R.id.ABOUT;

        // folder operation
        static final int ADD_NEW_FOLDER = R.id.ADD_NEW_FOLDER;
        static final int ENABLE_FOLDER_DRAG_AND_DROP = R.id.ENABLE_FOLDER_DRAG_AND_DROP;
        static final int DELETE_FOLDERS = R.id.DELETE_FOLDERS;
}
