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

package com.cw.audio7.audio;

import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2017/10/21.
 */

public class AudioUi_page {

    AppCompatActivity mAct;
    public ViewGroup audioPanel;

    public TextView audio_title;
    public TextView audio_artist; //todo
    public TextView audio_curr_pos;
    public SeekBar audio_seek_bar;
    public TextView audio_length;
    public TextView audio_number;
    public ImageView audio_previous_btn;
    public ImageView audio_play_btn;
    public ImageView audio_next_btn;

    public int mediaFileLength;
    private String audioUriStr;

    public AudioUi_page(AppCompatActivity act,View root_view,String audio_uri_str)
    {
        mAct = act;
        audioPanel = root_view.findViewById(R.id.audio_panel);
        audioUriStr = audio_uri_str;

        System.out.println("AudioUi_page / constructor / audioUriStr = " + audioUriStr);

        // set audio block listeners
        setAudioBlockListener(mAct);
    }

    /**
     * init audio block
     */
    public void initAudioPanel(View root_view)
    {
        System.out.println("AudioUi_page / _initAudioPanel");
        audioPanel = root_view.findViewById(R.id.audio_panel);
        setAudioBlockListener(mAct);
    }


    // set audio block listener
    public void setAudioBlockListener(AppCompatActivity act) {
        audio_curr_pos = (TextView) audioPanel.findViewById(R.id.audioPanel_current_pos);
        audio_seek_bar = (SeekBar) audioPanel.findViewById(R.id.seek_bar);
        audio_previous_btn = (ImageView) audioPanel.findViewById(R.id.audioPanel_previous);
        audio_play_btn = (ImageView) audioPanel.findViewById(R.id.audioPanel_play);
        audio_next_btn = (ImageView) audioPanel.findViewById(R.id.audioPanel_next);

        // audio length
        try
        {
            if(Util.isUriExisted(audioUriStr, act)) {
                MediaPlayer mp = MediaPlayer.create(act, Uri.parse(audioUriStr));
                mediaFileLength = mp.getDuration();
                mp.release();
            }
        }
        catch(Exception e)
        {
            System.out.println("AudioUi_note / _initAudioProgress / exception");
        }

        // Seek bar listener
        audio_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                System.out.println("AudioUi_note / _setOnSeekBarChangeListener / mediaFileLength = " + mediaFileLength);
                if( BackgroundAudioService.mMediaPlayer != null  )
                {
                    int mPlayAudioPosition = (int) (((float)(Audio7Player.getMediaFileLength() / 100)) * seekBar.getProgress());
                    BackgroundAudioService.mMediaPlayer.seekTo(mPlayAudioPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser)
                {
                    // show progress change
                    int currentPos = mediaFileLength *progress/(seekBar.getMax()+1);
                    int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
                    int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
                    int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
                    String curr_time_str = String.format(Locale.US,"%2d", curHour)+":" +
                            String.format(Locale.US,"%02d", curMin)+":" +
                            String.format(Locale.US,"%02d", curSec);
                    // set current play time
                    audio_curr_pos.setText(curr_time_str);
                }
            }
        });

        // Audio play and pause button on click listener
        audio_play_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
//                System.out.println("AudioUi_page / _initAudioBlock / audioPanel_play_button / _onClick");
                Audio_manager.audio7Player.runAudioState();

                if(Audio7Player.isOnAudioPlayingPage())
                {
                    Audio_manager.audio7Player.scrollPlayingItemToBeVisible(TabsHost.getCurrentPage().recyclerView); //todo ??? hang up
                    TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
                }
            }
        });

        // Audio play previous on click button listener
        audio_previous_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                do {
                    if(Audio_manager.mAudioPos > 0) {
                        Audio_manager.mAudioPos--;
                    }
                    //todo add option for circle
                    else if( Audio_manager.mAudioPos == 0)
                    {
                        if(Pref.getPref_cyclic_play_enable(mAct)) {
                            Audio_manager.mAudioPos = Audio_manager.getPlayingPage_notesCount() - 1;
                        }
                        else {
                            Audio_manager.mAudioPos = -1;
                            Toast.makeText(mAct,R.string.toast_cyclic_play_disabled, Toast.LENGTH_SHORT).show();
                            TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
                while (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0);

                if(Audio_manager.mAudioPos == -1)
                    Audio_manager.stopAudioPlayer();
                else
                    nextAudio_panel();
            }
        });

        // Audio play next on click button listener
        audio_next_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                do
                {
                    Audio_manager.mAudioPos++;
                    //todo add option for circle
                    if( Audio_manager.mAudioPos >= Audio_manager.getPlayingPage_notesCount()) {
                        if(Pref.getPref_cyclic_play_enable(mAct)) {
                            Audio_manager.mAudioPos = 0; //back to first index
                        }
                        else {
                            Audio_manager.mAudioPos = Audio_manager.getPlayingPage_notesCount();
                            Toast.makeText(mAct,R.string.toast_cyclic_play_disabled,Toast.LENGTH_SHORT).show();
                            TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
                while (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0);

                if(Audio_manager.mAudioPos == Audio_manager.getPlayingPage_notesCount()) {
                    //todo Add boundary check?
                    Audio_manager.stopAudioPlayer();
                }
                else
                    nextAudio_panel();
            }
        });
    }

    /**
     * Play next audio at AudioUi_page
     */
    private void nextAudio_panel()
    {
        System.out.println("AudioUi_page / _nextAudio_panel");

        // cancel playing
        if(BackgroundAudioService.mMediaPlayer != null)
        {
            if(BackgroundAudioService.mMediaPlayer.isPlaying())
            {
                BackgroundAudioService.mMediaPlayer.pause();
            }

            BackgroundAudioService.mMediaPlayer.release();
            BackgroundAudioService.mMediaPlayer = null;
        }

        // gif case: add this will cause program hang up
        if(Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP)
            Audio_manager.audio7Player.scrollPlayingItemToBeVisible(TabsHost.getCurrentPage().recyclerView);

        if(TabsHost.getCurrentPage().itemAdapter == null)
            TabsHost.reloadCurrentPage();
        else
            TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();

        // show new audio length immediately
        Audio_manager.audio7Player.initAudioBlock(Audio_manager.getAudioStringAt(Audio_manager.mAudioPos));

        Audio_manager.audio7Player.runAudioState();
    }

}