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


import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by CW on 2016/6/3.
 * ref: http://stackoverflow.com/questions/5448653/how-to-implement-onbackpressed-in-android-fragments
 */

public class BaseBackPressedListener implements OnBackPressedListener {
    AppCompatActivity activity;

    public BaseBackPressedListener(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public void doBack() {
        System.out.println("BaseBackPressedListener / _doBack");
//        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        activity.getSupportFragmentManager().popBackStack();
    }
}