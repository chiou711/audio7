/*
 * Copyright (C) 2020 CW Chiu
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cw.audio7.R;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.note.NoteUi;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;


/**
 * Created by cw on 2017/10/26.
 * Modified by cw on 2020/10/07
 */

public class AudioUi_note
{
    private AppCompatActivity act;
    private ViewGroup audioBlock;

    public TextView audio_title;
    public TextView audio_artist;
    public TextView audio_curr_pos;
    public SeekBar audio_seek_bar;
    public TextView audio_length;
    public TextView audio_number; //todo
    public ImageView audio_previous_btn;
    public ImageView audio_play_btn;
    public ImageView audio_next_btn;

    private static int mProgress;
    private static int mediaFileLength; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
    public static boolean isPausedAtSeekerAnchor;
    public static int mAnchorPosition;
    View rootView;
    private String audioUriStr;

    // constructor
    public AudioUi_note(AppCompatActivity act, View root_view)
    {
        this.act = act;
        rootView = root_view;
    }

    // initialize audio block
    public void initAudioBlock(String audioUriStr)
    {
        setAudioUriStr(audioUriStr);

        audioBlock = (ViewGroup) rootView.findViewById(R.id.audioGroup);
        audioBlock.setBackgroundColor(ColorSet.color_black);

        audio_title = (TextView) rootView.findViewById(R.id.pager_audio_title); // first setting
        audio_artist = (TextView) rootView.findViewById(R.id.pager_audio_artist);

        audio_title.setTextColor(ColorSet.color_white);
        if (Util.isLandscapeOrientation(act))
        {
            audio_title.setMovementMethod(new ScrollingMovementMethod());
            audio_title.scrollTo(0,0);
        }
        else
        {
            audio_title.setSingleLine(true);
            audio_title.setSelected(true);
        }

        audio_title.setTextColor(ColorSet.getPauseColor(act));
        showAudioName(act,audioUriStr);
        audio_title.setSelected(false);

        // audio progress
        audio_curr_pos = (TextView) rootView.findViewById(R.id.pager_audio_current_pos);

        // current position
        mProgress = 0;
        int curHour = Math.round((float)(mProgress / 1000 / 60 / 60));
        int curMin = Math.round((float)((mProgress - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((mProgress - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
        String curr_pos_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
                String.format(Locale.ENGLISH,"%02d", curMin)+":" +
                String.format(Locale.ENGLISH,"%02d", curSec);

        audio_curr_pos.setText(curr_pos_str);
        audio_curr_pos.setTextColor(ColorSet.color_white);

        // audio seek bar
        audio_seek_bar = (SeekBar) rootView.findViewById(R.id.pager_img_audio_seek_bar);
        audio_length = (TextView) rootView.findViewById(R.id.pager_audio_file_length);

        audio_seek_bar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
        audio_seek_bar.setMax(99); // It means 100% .0-99
        audio_seek_bar.setVisibility(View.VISIBLE);

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
        // set audio file length
        int fileHour = Math.round((float)(mediaFileLength / 1000 / 60 / 60));
        int fileMin = Math.round((float)((mediaFileLength - fileHour * 60 * 60 * 1000) / 1000 / 60));
        int fileSec = Math.round((float)((mediaFileLength - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));

        String strHour = String.format(Locale.ENGLISH,"%2d", fileHour);
        String strMinute = String.format(Locale.ENGLISH,"%02d", fileMin);
        String strSecond = String.format(Locale.ENGLISH,"%02d", fileSec);
        String strLength = strHour + ":" + strMinute+ ":" + strSecond;

        audio_length.setText(strLength);
        audio_length.setTextColor(ColorSet.color_white);

        // audio buttons
        audio_previous_btn = (ImageView) rootView.findViewById(R.id.audioPanel_previous);
        audio_previous_btn.setImageResource(R.drawable.ic_media_previous);

        audio_play_btn = (ImageView) rootView.findViewById(R.id.pager_btn_audio_play);
        audio_play_btn.setImageResource(R.drawable.ic_media_play);

        audio_next_btn = (ImageView) rootView.findViewById(R.id.audioPanel_next);
        audio_next_btn.setImageResource(R.drawable.ic_media_next);

        // set audio block listeners
        setAudioBlockListener(act);

    }

    public String getAudioUriStr() {
        return audioUriStr;
    }

    public void setAudioUriStr(String audioUriStr) {
        this.audioUriStr = audioUriStr;
    }

    // show audio name
    void showAudioName(AppCompatActivity act,String audioUriStr)
    {
        // title: set marquee
        if(Util.isUriExisted(audioUriStr, act)) {
            String[] audio_name = Util.getDisplayNameByUriString(audioUriStr, act);
            audio_title.setText(audio_name[0] );
            audio_artist.setText(audio_name[1]);
        }
        else {
            audio_title.setText("N/A");
            audio_artist.setText("");
        }

        audio_title.setSelected(false);
        audio_artist.setSelected(false);
    }

    // set audio block listener
    private void setAudioBlockListener(final AppCompatActivity act)
    {
        // set audio play and pause control image
        audio_play_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                isPausedAtSeekerAnchor = false;

                if( (Audio_manager.isRunnableOn_page)||
                    (BackgroundAudioService.mMediaPlayer == null) ) {
                    // use this flag to determine new play or not in note
                    BackgroundAudioService.mIsPrepared = false;
                    if(AudioPlayer_page.mAudioHandler != null)
                        AudioPlayer_page.mAudioHandler.removeCallbacks(AudioPlayer_page.page_runnable);
                }
                playAudioInPager(act,getAudioUriStr());
            }
        });

        // set seek bar listener
        audio_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if( BackgroundAudioService.mMediaPlayer != null  )
                {
                    int mPlayAudioPosition = (int) (((float)(mediaFileLength / 100)) * seekBar.getProgress());
                    BackgroundAudioService.mMediaPlayer.seekTo(mPlayAudioPosition);
                }
                else
                {
                    // note audio: slide seek bar anchor from stop to pause
                    isPausedAtSeekerAnchor = true;
                    mAnchorPosition = (int) (((float)(mediaFileLength / 100)) * seekBar.getProgress());
                    playAudioInPager(act,getAudioUriStr());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // audio player is one time mode in pager
                if(Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)
                    Audio_manager.stopAudioPlayer();
            }

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
                    String curr_play_time_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
                        String.format(Locale.ENGLISH,"%02d", curMin)+":" +
                        String.format(Locale.ENGLISH,"%02d", curSec);
                    // set current play time
                    audio_curr_pos.setText(curr_play_time_str);
                }
            }
        });

        audio_previous_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPager viewPager = rootView.findViewById(R.id.tabs_pager);
                NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()-1);
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });

        audio_next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPager viewPager = rootView.findViewById(R.id.tabs_pager);
                NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()+1);
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });
    }

    AudioPlayer_note audioPlayer_note;
    //  play audio in pager
    private void playAudioInPager(AppCompatActivity act, String audioUriStr)
    {
        System.out.println("AudioUi_note / _playAudioInPager");

        if(Audio_manager.getAudioPlayMode()  == Audio_manager.PAGE_PLAY_MODE)
            Audio_manager.stopAudioPlayer();

        String[] audioName = Util.getDisplayNameByUriString(audioUriStr, act);
        if(UtilAudio.hasAudioExtension(audioUriStr) ||
           UtilAudio.hasAudioExtension(audioName[0]))
        {
            AudioPlayer_note.mAudioPos = NoteUi.getFocus_notePos();
            Audio_manager.mAudioPos = NoteUi.getFocus_notePos();
            MainAct.mPlaying_pageTableId = TabsHost.getCurrentPageTableId();

            Audio_manager.setAudioPlayMode(Audio_manager.NOTE_PLAY_MODE);

            // new instance
            if(audioPlayer_note == null) {
                audioPlayer_note = new AudioPlayer_note(act, this);
                audioPlayer_note.prepareAudioInfo();
            }

            if(audioPlayer_note != null)
                audioPlayer_note.runAudioState();

            updateAudioPanel_note(act);
        }
    }

    // update audio progress
    public void updateAudioProgress()
    {
        int currentPos=0;

        if(BackgroundAudioService.mMediaPlayer != null)
            currentPos = BackgroundAudioService.mMediaPlayer.getCurrentPosition();

        int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
        int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
        String curr_time_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
                                             String.format(Locale.ENGLISH,"%02d", curMin)+":" +
                                             String.format(Locale.ENGLISH,"%02d", curSec);
        System.out.println("AudioPlayer_note / _updateAudioProgress / curr_time_str = " + curr_time_str);

        // set current play time and the play length of audio file
        if(audio_curr_pos != null)
        {
            audio_curr_pos.setText(curr_time_str);
        }

        mProgress = (int)(((float)currentPos/ mediaFileLength)*100);

        if(audio_seek_bar != null)
            audio_seek_bar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
    }

    // update note audio panel
    public void updateAudioPanel_note(AppCompatActivity act)
    {
        System.out.println("AudioUi_note / _updateAudioPanel_note");

        if(Audio_manager.getAudioPlayMode() != Audio_manager.NOTE_PLAY_MODE)
            return;

        // update playing state
        if(Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PLAY)
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_pause);
            showAudioName(act,getAudioUriStr());
            audio_title.setTextColor(ColorSet.getHighlightColor(act) );
            audio_title.setSelected(true);
            audio_artist.setTextColor(ColorSet.getHighlightColor(act) );
            audio_artist.setSelected(true);
        }
        else if( (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PAUSE) ||
                (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_STOP)    )
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_play);
            showAudioName(act,audioUriStr);
            audio_title.setTextColor(ColorSet.getPauseColor(act));
            audio_title.setSelected(false);
            audio_artist.setTextColor(ColorSet.getPauseColor(act));
            audio_artist.setSelected(false);
        }
    }

}