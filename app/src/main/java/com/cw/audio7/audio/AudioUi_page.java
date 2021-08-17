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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.folder.Folder;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import static com.cw.audio7.main.MainAct.audio_manager;

/**
 * Created by cw on 2017/10/21.
 */

public class AudioUi_page {

    AppCompatActivity mAct;
    private View audioPanel;

    private TextView audio_curr_pos;
    private SeekBar audio_seek_bar;
    public ImageView audio_previous_btn;
    public ImageView audio_play_btn;
    public ImageView audio_next_btn;

    private int mediaFileLength;
    private String audioUriStr;
    private Audio7Player audio7Player;
    TabsHost tabsHost;

    public AudioUi_page(AppCompatActivity act, TabsHost _tabsHost, Audio7Player _audio7Player, View panel_view, String audio_uri_str)
    {
        mAct = act;
        tabsHost = _tabsHost;
        audio7Player = _audio7Player;
        audioPanel = panel_view;
        audioUriStr = audio_uri_str;

        audio_manager.setAudioPlayMode(audio_manager.PAGE_PLAY_MODE);

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
                    int mPlayAudioPosition = (int) (((float)(audio7Player.getMediaFileLength() / 100)) * seekBar.getProgress());
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
                audio7Player.runAudioState();

                if(audio_manager.isOnAudioPlayingPage())
                {
                    audio7Player.scrollPlayingItemToBeVisible(tabsHost.getCurrentPage().recyclerView);
                    tabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
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
                    if(audio_manager.mAudioPos > 0) {
                        audio_manager.mAudioPos--;
                    }
                    else if( audio_manager.mAudioPos == 0)
                    {
                        if(Pref.getPref_cyclic_play_enable(mAct)) {
                            audio_manager.mAudioPos = Audio_manager.getPlayingPage_notesCount() - 1;
                        }
                        else {
                            audio_manager.mAudioPos = -1;
                            break;
                        }
                    }
                }
                while (audio_manager.getCheckedAudio(audio_manager.mAudioPos) == 0);

                if(audio_manager.mAudioPos == -1) {
                    audio_manager.stopAudioPlayer();
                    audio7Player.showAudioPanel(false);
                    tabsHost.reloadCurrentPage();
                    Toast.makeText(mAct,R.string.toast_cyclic_play_disabled,Toast.LENGTH_LONG).show();
                }
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
                System.out.println("AudioUi_page / audio_next_btn / onClick");
                int playingPage_notesCnt = Audio_manager.getPlayingPage_notesCount();
                do
                {
                    audio_manager.mAudioPos++;
                    if( audio_manager.mAudioPos >= playingPage_notesCnt) {
                        if(Pref.getPref_cyclic_play_enable(mAct)) {
                            audio_manager.mAudioPos = 0; //back to first index
                        }
                        else {
                            audio_manager.mAudioPos = playingPage_notesCnt;
                            break;
                        }
                    }
                }
                while (audio_manager.getCheckedAudio(audio_manager.mAudioPos) == 0); //todo  Invalid index 3, size is 3

                if(audio_manager.mAudioPos >= playingPage_notesCnt) {
                    audio_manager.stopAudioPlayer();
                    audio7Player.showAudioPanel(false);
                    tabsHost.reloadCurrentPage();
                    Toast.makeText(mAct,R.string.toast_cyclic_play_disabled,Toast.LENGTH_LONG).show();
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
        if(audio_manager.isOnAudioPlayingPage())
            audio7Player.scrollPlayingItemToBeVisible(tabsHost.getCurrentPage().recyclerView);

        if( tabsHost!=null ) {
            if( tabsHost.getCurrentPage().itemAdapter == null)
                tabsHost.reloadCurrentPage();
            else
                tabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
        }

        // show new audio length immediately
        audio7Player.initAudioBlock(audio_manager.getAudioStringAt(audio_manager.mAudioPos));

        audio7Player.runAudioState();
    }

}