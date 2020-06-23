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

package com.cw.audio7.tabs;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cw.audio7.R;
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.operation.audio.AudioPlayer_page;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by cw on 2017/10/21.
 */

public class AudioUi_page {

    AppCompatActivity mAct;
    View audio_panel;
    public TextView audioPanel_curr_pos;
    public TextView audio_panel_title_textView;
    public ImageView audioPanel_play_button;
    public ImageView audioPanel_next_btn;
    public ImageView audioPanel_previous_btn;
    public SeekBar seekBarProgress;
    public static int mProgress;
    RecyclerView listView;

    public AudioUi_page(AppCompatActivity act, RecyclerView _listView)
    {
        this.mAct = act;
        listView = _listView;
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

        audio_panel_title_textView = (TextView) audio_panel.findViewById(R.id.audio_panel_title);

        // scroll audio title to start position at landscape orientation
        // marquee of audio title is enabled for Portrait, not Landscape
        if (Util.isLandscapeOrientation(mAct))
        {
            audio_panel_title_textView.setMovementMethod(new ScrollingMovementMethod());
            audio_panel_title_textView.scrollTo(0,0);
        }
        else {
            // set marquee
            audio_panel_title_textView.setSingleLine(true);
            audio_panel_title_textView.setSelected(true);
        }

        // update play button status
        audioPanel_play_button = (ImageView) act.findViewById(R.id.audioPanel_play);

        audioPanel_previous_btn = (ImageView) act.findViewById(R.id.audioPanel_previous);
        audioPanel_previous_btn.setImageResource(R.drawable.ic_media_previous);

        audioPanel_next_btn = (ImageView) act.findViewById(R.id.audioPanel_next);
        audioPanel_next_btn.setImageResource(R.drawable.ic_media_next);

        // text view for audio info
        audioPanel_curr_pos = (TextView) act.findViewById(R.id.audioPanel_current_pos);
        TextView audioPanel_file_length = (TextView) act.findViewById(R.id.audioPanel_file_length);
        TextView audioPanel_audio_number = (TextView) act.findViewById(R.id.audioPanel_audio_number);

        // init audio seek bar
        seekBarProgress = (SeekBar)act.findViewById(R.id.audioPanel_seek_bar);
        seekBarProgress.setMax(99); // It means 100% .0-99
        seekBarProgress.setProgress(mProgress);

        // seek bar behavior is not like other control item
        //, it is seen when changing drawer, so set invisible at xml
        seekBarProgress.setVisibility(View.VISIBLE);

        // show audio file audio length of playing
        int media_length = AudioPlayer_page.media_file_length;
//        System.out.println("AudioUi_page / _initAudioBlock / audioLen = " + media_length);
        int fileHour = Math.round((float)(media_length / 1000 / 60 / 60));
        int fileMin = Math.round((float)((media_length - fileHour * 60 * 60 * 1000) / 1000 / 60));
        int fileSec = Math.round((float)((media_length - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
        String file_len_str =  String.format(Locale.US,"%2d", fileHour)+":" +
                String.format(Locale.US,"%02d", fileMin)+":" +
                String.format(Locale.US,"%02d", fileSec);
        audioPanel_file_length.setText(file_len_str);

        // show playing audio item message
        String message = mAct.getResources().getString(R.string.menu_button_play) +
                "#" +
                (Audio_manager.mAudioPos +1);
        audioPanel_audio_number.setText(message);

        //
        // Set up listeners
        //

        // Seek bar listener
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
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
                    audioPanel_curr_pos.setText(curr_time_str);
                }
            }
        });

        // Audio play and pause button on click listener
        audioPanel_play_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
//                System.out.println("AudioUi_page / _initAudioBlock / audioPanel_play_button / _onClick");
                TabsHost.audioPlayer_page.runAudioState();

                // update status
                UtilAudio.updateAudioPanel((ImageView)v, audio_panel_title_textView); // here v is audio play button

                if(AudioPlayer_page.isOnAudioPlayingPage())
                {
                    TabsHost.audioPlayer_page.scrollHighlightAudioItemToVisible(TabsHost.getCurrentPage().recyclerView);
                    TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
                }
            }
        });

        // Audio play previous on click button listener
        audioPanel_previous_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                do {
                    if(Audio_manager.mAudioPos > 0)
                        Audio_manager.mAudioPos--;
                    else if( Audio_manager.mAudioPos == 0)
                    {
                        Audio_manager.mAudioPos = Audio_manager.getPlayingPage_notesCount()-1;
                    }
                }
                while (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0);

                nextAudio_panel();
            }
        });

        // Audio play next on click button listener
        audioPanel_next_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                do
                {
                    Audio_manager.mAudioPos++;
                    if( Audio_manager.mAudioPos >= Audio_manager.getPlayingPage_notesCount())
                        Audio_manager.mAudioPos = 0; //back to first index
                }
                while (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0);

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

        // update status
        UtilAudio.updateAudioPanel(audioPanel_play_button, audio_panel_title_textView);

        if(Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP)
            TabsHost.audioPlayer_page.scrollHighlightAudioItemToVisible(TabsHost.getCurrentPage().recyclerView);

        TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
    }

}
