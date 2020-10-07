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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.preferences.Pref;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2017/10/21.
 */

public class AudioUi_page {

    AppCompatActivity mAct;
    View audio_panel;

    public TextView audio_title;
    public TextView audio_artist; //todo
    public TextView audio_curr_pos;
    public SeekBar audio_seek_bar;
    public TextView audio_length;
    public TextView audio_number;
    public ImageView audio_previous_btn;
    public ImageView audio_play_btn;
    public ImageView audio_next_btn;

    public static int mProgress;
    public int media_file_length;
    private String mAudioUriInDB;
    public AudioUi_page(AppCompatActivity act,String audioUriStr)
    {
        this.mAct = act;
        mAudioUriInDB = audioUriStr;
    }

    /**
     * init audio block
     */
    public void initAudioBlock(AppCompatActivity act)
    {
        System.out.println("AudioUi_page / _initAudioBlock");

        audio_panel = act.findViewById(R.id.audio_panel);

        if(audio_panel == null)
            return;

        audio_title = (TextView) audio_panel.findViewById(R.id.audio_panel_title);

        // scroll audio title to start position at landscape orientation
        // marquee of audio title is enabled for Portrait, not Landscape
        if (Util.isLandscapeOrientation(mAct))
        {
            audio_title.setMovementMethod(new ScrollingMovementMethod());
            audio_title.scrollTo(0,0);
        }
        else {
            // set marquee
            audio_title.setSingleLine(true);
            audio_title.setSelected(true);
        }


        // text view for audio info
        audio_curr_pos = (TextView) act.findViewById(R.id.audioPanel_current_pos);

        // init audio seek bar
        audio_seek_bar = (SeekBar)act.findViewById(R.id.audioPanel_seek_bar);
        audio_seek_bar.setMax(99); // It means 100% .0-99
        audio_seek_bar.setProgress(mProgress);

        audio_length = (TextView) act.findViewById(R.id.audioPanel_file_length);

        // seek bar behavior is not like other control item
        //, it is seen when changing drawer, so set invisible at xml
        audio_seek_bar.setVisibility(View.VISIBLE);

        // show audio file audio length of playing
        // get audio file length
        try
        {
            if(Util.isUriExisted(mAudioUriInDB, act)) {
                MediaPlayer mp = MediaPlayer.create(act, Uri.parse(mAudioUriInDB));
                media_file_length = mp.getDuration();
                mp.release();
            }
        }
        catch(Exception e)
        {
            System.out.println("AudioUi_note / _initAudioProgress / exception");
        }

//        System.out.println("AudioUi_page / _initAudioBlock / audioLen = " + media_length);
        int fileHour = Math.round((float)(media_file_length / 1000 / 60 / 60));
        int fileMin = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000) / 1000 / 60));
        int fileSec = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
        String file_len_str =  String.format(Locale.US,"%2d", fileHour)+":" +
                String.format(Locale.US,"%02d", fileMin)+":" +
                String.format(Locale.US,"%02d", fileSec);
        audio_length.setText(file_len_str);

        // audio number
        audio_number = (TextView) act.findViewById(R.id.audioPanel_audio_number);
        // show playing audio item message
        String message = mAct.getResources().getString(R.string.menu_button_play) +
                "#" +
                (Audio_manager.mAudioPos +1);
        audio_number.setText(message);

        // buttons
        audio_play_btn = (ImageView) act.findViewById(R.id.audioPanel_play);

        audio_previous_btn = (ImageView) act.findViewById(R.id.audioPanel_previous);
        audio_previous_btn.setImageResource(R.drawable.ic_media_previous);

        audio_next_btn = (ImageView) act.findViewById(R.id.audioPanel_next);
        audio_next_btn.setImageResource(R.drawable.ic_media_next);

        // set audio block listeners
        setAudioBlockListener();
    }


    // set audio block listener
    private void setAudioBlockListener() {
        // Seek bar listener
        audio_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if( BackgroundAudioService.mMediaPlayer != null  )
                {
                    int mPlayAudioPosition = (int) (((float)(AudioPlayer_page.media_file_length / 100)) * seekBar.getProgress());
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
                    int currentPos = AudioPlayer_page.media_file_length *progress/(seekBar.getMax()+1);
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
                TabsHost.audioPlayer_page.runAudioState();

                // update audio panel
                updateAudioPanel_page((ImageView)v, audio_title); // here v is audio play button

                if(AudioPlayer_page.isOnAudioPlayingPage())
                {
                    TabsHost.audioPlayer_page.scrollPlayingItemToBeVisible(TabsHost.getCurrentPage().recyclerView); //todo ??? hang up
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

                if(Audio_manager.mAudioPos == Audio_manager.getPlayingPage_notesCount())
                    Audio_manager.stopAudioPlayer();
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

        // new audio player instance
        TabsHost.audioPlayer_page.runAudioState();

        // update audio panel
        updateAudioPanel_page(audio_play_btn, audio_title);

        // gif case: add this will cause program hang up
        if(Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP)
            TabsHost.audioPlayer_page.scrollPlayingItemToBeVisible(TabsHost.getCurrentPage().recyclerView);

        TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
    }

    // update page audio panel
    public void updateAudioPanel_page(ImageView playBtn, TextView titleTextView)
    {
        System.out.println("UtilAudio/ _updateAudioPanel / Audio_manager.getPlayerState() = " + Audio_manager.getPlayerState());
        titleTextView.setBackgroundColor(ColorSet.color_black);
        if(Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PLAY)
        {
            titleTextView.setTextColor(ColorSet.getHighlightColor(MainAct.mAct));
            titleTextView.setSelected(true);
            playBtn.setImageResource(R.drawable.ic_media_pause);
        }
        else if( (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PAUSE) ||
                (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_STOP)    )
        {
            titleTextView.setSelected(false);
            titleTextView.setTextColor(ColorSet.getPauseColor(MainAct.mAct));
            playBtn.setImageResource(R.drawable.ic_media_play);
        }

    }

    void updateAudioProgress()
    {
//		System.out.println("AudioUI_page / _update_audioPanel_progress");

        // get current playing position
        int currentPos = 0;
        if(BackgroundAudioService.mMediaPlayer != null)
            currentPos = BackgroundAudioService.mMediaPlayer.getCurrentPosition();

        int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
        int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));

        // set current playing time
        String timeString;
        if(media_file_length != 0) {
            timeString = String.format(Locale.US, "%2d", curHour) + ":" +
                    String.format(Locale.US, "%02d", curMin) + ":" +
                    String.format(Locale.US, "%02d", curSec);
        }
        else {
            timeString = String.format(Locale.US, "%2d", 0) + ":" +
                    String.format(Locale.US, "%02d", 0) + ":" +
                    String.format(Locale.US, "%02d", 0);
        }
        audio_curr_pos.setText(timeString);
        System.out.println("AudioPlayer_page / _update_audioPanel_progress / timeString = " + timeString);

        // set current progress
        AudioUi_page.mProgress = (int)(((float)currentPos/ media_file_length)*100);

        if(media_file_length > 0 )
            audio_seek_bar.setProgress(AudioUi_page.mProgress); // This math construction give a percentage of "was playing"/"media length"
    }

}