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
 * Created by CW on 2016/7/16.
 */
import android.app.Activity;
import android.graphics.Color;

import com.cw.audio7.R;

public class ColorSet
{
    // style
    // 0,2,4,6,8: dark background, 1,3,5,7,9: light background
    public static int[] mBG_ColorArray = new int[]{Color.rgb(34,34,34), //#222222
            Color.rgb(255,255,255),
            Color.rgb(38,87,51), //#265733
            Color.rgb(186,249,142),
            Color.rgb(87,38,51),//#572633
            Color.rgb(249,186,142),
            Color.rgb(38,51,87),//#263357
            Color.rgb(142,186,249),
            Color.rgb(87,87,51),//#575733
            Color.rgb(249,249,140)};
    public static int[] mText_ColorArray = new int[]{Color.rgb(255,255,255),
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            Color.rgb(0,0,0),
            Color.rgb(255,255,255),
            Color.rgb(0,0,0)};

    public static int color_white = Color.rgb(255,255,255);
    public static int color_black = Color.rgb(0,0,0);
    public static int color_gray = Color.rgb(120,120,120);

    public static int getBarColor(Activity act)
    {
        return act.getResources().getColor(R.color.bar_color);
    }

    public static int getButtonColor(Activity act)
    {
        return act.getResources().getColor(R.color.button_color);
    }

    public static int getHighlightColor(Activity act)
    {
        return act.getResources().getColor(R.color.highlight_color);
    }

    public static int getPauseColor(Activity act)
    {
        return act.getResources().getColor(R.color.pause_color);
    }
}
