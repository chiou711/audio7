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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.audio7.R;
import com.cw.audio7.main.MainAct;
import com.cw.audio7.note.NoteUi;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;
import com.cw.audio7.util.preferences.Pref;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import static com.cw.audio7.main.MainAct.audio_manager;
import static com.cw.audio7.main.MainAct.mAudioHandler;
import static com.cw.audio7.main.MainAct.audio_runnable;
import static com.cw.audio7.main.MainAct.mFolderUi;

/**
 * Created by cw on 2017/10/26.
 * Modified by cw on 2020/10/09
 */

public class AudioUi_note
{
    public ViewGroup audioPanel;

    public TextView audio_title;
    public TextView audio_artist;
    public TextView audio_curr_pos;
    public SeekBar audio_seek_bar;
    public TextView audio_length;
    public TextView audio_number; //todo
    public ImageView audio_previous_btn;
    public ImageView audio_play_btn;
    public ImageView audio_next_btn;

    private static int mediaFileLength; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
    public static boolean isPausedAtSeekerAnchor;
    public static int mAnchorPosition;
    View rootView;
    private String audioUriStr;

    // constructor
    public AudioUi_note(AppCompatActivity act, View root_view, String audio_uri_str)
    {
        rootView = root_view;

        if(root_view != null)
            audioPanel = (ViewGroup) rootView.findViewById(R.id.audioGroup);
        else
            audioPanel = (ViewGroup) act.findViewById(R.id.audioGroup);

        audioUriStr = audio_uri_str;

        System.out.println("AudioUi_note / constructor /  audioUriStr = " + audioUriStr );

        audio_manager.setAudioPlayMode(audio_manager.NOTE_PLAY_MODE);

        // set audio block listeners
        setAudioBlockListener(act);
    }

    // set audio block listener
    public void setAudioBlockListener(final AppCompatActivity act)
    {
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

        // set audio play and pause control image
        audio_play_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                System.out.println("AudioUi_note / _onClick / audio_play_btn");
                isPausedAtSeekerAnchor = false;

                if( (BackgroundAudioService.mMediaPlayer == null) ) {
                    // use this flag to determine new play or not in note
                    BackgroundAudioService.mIsPrepared = false;

                    if( (mFolderUi.tabsHost.audio7Player != null) &&
                        (mAudioHandler != null) )
                        mAudioHandler.removeCallbacks(audio_runnable);
                }

                playAudioInNotePager(act,audioUriStr);
            }
        });

        // set seek bar listener
        audio_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                mediaFileLength = mFolderUi.tabsHost.audio7Player.getMediaFileLength();
                System.out.println("AudioUi_note / audio_seek_bar / _setOnSeekBarChangeListener / mediaFileLength = "+
                        mediaFileLength);
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
                    playAudioInNotePager(act, audio_manager.getAudioStringAt(audio_manager.mAudioPos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // audio player is one time mode in pager
                if(audio_manager.getAudioPlayMode() == audio_manager.PAGE_PLAY_MODE)
                    audio_manager.stopAudioPlayer(act);
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

//                System.out.println("AudioUi_note /  audio_next_btn / _onClick / NoteUi.getNotesCnt() = " + NoteUi.getNotesCnt());
//                System.out.println("AudioUi_note /  audio_next_btn / _onClick / NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());

                audio_manager.stopAudioPlayer(act);

                int new_pos = NoteUi.getFocus_notePos()-1;
                if( new_pos < 0 )
                {
                    if(Pref.getPref_cyclic_play_enable(act)) {
                        NoteUi.setFocus_notePos(NoteUi.getNotesCnt()-1);
                        viewPager.setCurrentItem(NoteUi.getNotesCnt()-1);
                    } else {
                        Toast.makeText(act,R.string.toast_cyclic_play_disabled,Toast.LENGTH_SHORT).show();
                        audio_manager.stopAudioPlayer(act);
                        return;
                    }
                }
                else {
                    NoteUi.setFocus_notePos(new_pos);
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                }

            }
        });

        audio_next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPager viewPager;

                if(rootView != null)
                    viewPager = rootView.findViewById(R.id.tabs_pager);
                else
                    viewPager = act.findViewById(R.id.tabs_pager);

//                System.out.println("AudioUi_note /  audio_next_btn / _onClick / NoteUi.getNotesCnt() = " + NoteUi.getNotesCnt());
//                System.out.println("AudioUi_note /  audio_next_btn / _onClick / NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());

                audio_manager.stopAudioPlayer(act);

                int new_pos = NoteUi.getFocus_notePos()+1;
                if( new_pos >= NoteUi.getNotesCnt())
                {
                    if(Pref.getPref_cyclic_play_enable(act)) {
                        NoteUi.setFocus_notePos(0);
                        viewPager.setCurrentItem(0);
                    } else {
                        Toast.makeText(act,R.string.toast_cyclic_play_disabled,Toast.LENGTH_SHORT).show();
                        audio_manager.stopAudioPlayer(act);
                        return;
                    }
                }
                else {
                    NoteUi.setFocus_notePos(new_pos);
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }

            }
        });
    }

    //  play audio in pager
    public void playAudioInNotePager(AppCompatActivity act, String audioUriStr)
    {
        System.out.println("AudioUi_note / _playAudioInNotePager");

        String[] audioName = Util.getDisplayNameByUriString(audioUriStr, act);
        if(UtilAudio.hasAudioExtension(audioUriStr) ||
           UtilAudio.hasAudioExtension(audioName[0]))
        {
            audio_manager.mAudioPos = NoteUi.getFocus_notePos();
            MainAct.mPlaying_pageTableId = mFolderUi.tabsHost.getCurrentPageTableId();

            // new instance
            mFolderUi.tabsHost.audio7Player = new Audio7Player(act, audioPanel, audioUriStr);
            mFolderUi.tabsHost.audio7Player.setAudioPanel(audioPanel);

            audio_manager.setupAudioList(act);
            mFolderUi.tabsHost.audio7Player.runAudioState();
        }
    }

}