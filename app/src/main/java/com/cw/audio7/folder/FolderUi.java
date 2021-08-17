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

package com.cw.audio7.folder;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class FolderUi extends Folder
{
	AppCompatActivity act;

	public FolderUi(AppCompatActivity _act){
		act = _act;
		mFolderTitles = new ArrayList<>();
		FolderSetup(act);
	};

}






