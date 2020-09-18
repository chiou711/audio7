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

package com.cw.audio7.note;

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
import com.cw.audio7.operation.audio.Audio_manager;
import com.cw.audio7.operation.audio.AudioPlayer_note;
import com.cw.audio7.operation.audio.BackgroundAudioService;
import com.cw.audio7.tabs.TabsHost;
import com.cw.audio7.util.ColorSet;
import com.cw.audio7.util.Util;
import com.cw.audio7.util.audio.UtilAudio;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;


/**
 * Created by cw on 2017/10/26.
 * Modified by cw on 2020/08/14
 */

public class AudioUi_note
{
    private AppCompatActivity act;
    TextView audio_title;
    private ViewGroup audioBlock;
    private String mAudioUriInDB;
    public ImageView audioPanel_next_btn;
    public ImageView audioPanel_previous_btn;
    View rootView;

    // constructor
    AudioUi_note(AppCompatActivity act, String audioUriInDB, View root_view)
    {
        this.act = act;
        mAudioUriInDB = audioUriInDB;
        rootView = root_view;
    }

    // initialize audio block
    void init_audio_block()
    {
        // audio block
        audioPanel_previous_btn = (ImageView) rootView.findViewById(R.id.audioPanel_previous);
        audioPanel_previous_btn.setImageResource(R.drawable.ic_media_previous);

        audioPanel_next_btn = (ImageView) rootView.findViewById(R.id.audioPanel_next);
        audioPanel_next_btn.setImageResource(R.drawable.ic_media_next);

        audio_title = (TextView) rootView.findViewById(R.id.pager_audio_title); // first setting
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

        audioBlock = (ViewGroup) rootView.findViewById(R.id.audioGroup);
        audioBlock.setBackgroundColor(ColorSet.color_black);

        mPager_audio_play_button = (ImageView) rootView.findViewById(R.id.pager_btn_audio_play);
    }

    // show audio block
    void showAudioBlock()
    {
        if(UtilAudio.hasAudioExtension(mAudioUriInDB))
        {
            audioBlock.setVisibility(View.VISIBLE);
            initAudioProgress(act,mAudioUriInDB);
        }
        else
            audioBlock.setVisibility(View.GONE);
    }

    // initialize audio progress
    public void initAudioProgress(AppCompatActivity act,String audioUriInDB)
    {
        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.pager_img_audio_seek_bar);
        ImageView mPager_audio_play_button = (ImageView) rootView.findViewById(R.id.pager_btn_audio_play);

        // set audio block listeners
        setAudioBlockListener(act, audioUriInDB);

        mProgress = 0;

        mAudioUriInDB = audioUriInDB;
        showAudioName(act);

        TextView audioTitle = (TextView) rootView.findViewById(R.id.pager_audio_title);
        audioTitle.setSelected(false);
        mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
        audioTitle.setTextColor(ColorSet.getPauseColor(act));
        audioTitle.setSelected(false);

        // current position
        int curHour = Math.round((float)(mProgress / 1000 / 60 / 60));
        int curMin = Math.round((float)((mProgress - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((mProgress - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
        String curr_pos_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
                                           String.format(Locale.ENGLISH,"%02d", curMin)+":" +
                                           String.format(Locale.ENGLISH,"%02d", curSec);

        TextView audio_curr_pos = (TextView) rootView.findViewById(R.id.pager_audio_current_pos);
        audio_curr_pos.setText(curr_pos_str);
        audio_curr_pos.setTextColor(ColorSet.color_white);

        // audio seek bar
        seekBar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
        seekBar.setMax(99); // It means 100% .0-99
        seekBar.setVisibility(View.VISIBLE);

        // get audio file length
        try
        {
            if(Util.isUriExisted(mAudioUriInDB, act)) {
                MediaPlayer mp = MediaPlayer.create(act, Uri.parse(mAudioUriInDB));
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

        TextView audio_length = (TextView) rootView.findViewById(R.id.pager_audio_file_length);
        audio_length.setText(strLength);
        audio_length.setTextColor(ColorSet.color_white);
    }

    // show audio name
    void showAudioName(AppCompatActivity act)
    {
        TextView audio_title_text_view = (TextView) rootView.findViewById(R.id.pager_audio_title);
        TextView audio_artist_text_view = (TextView) rootView.findViewById(R.id.pager_audio_artist);
        // title: set marquee
        if(Util.isUriExisted(mAudioUriInDB, act)) {
            String[] audio_name = Util.getDisplayNameByUriString(mAudioUriInDB, act);
            audio_title_text_view.setText(audio_name[0] );
            audio_artist_text_view.setText(audio_name[1]);
        }
        else {
            audio_title_text_view.setText("N/A");
            audio_artist_text_view.setText("");
        }

        audio_title_text_view.setSelected(false);
        audio_artist_text_view.setSelected(false);
    }

    // Set audio block
    public ImageView mPager_audio_play_button;
    private static int mProgress;
    private static int mediaFileLength; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class

    public static boolean isPausedAtSeekerAnchor;
    public static int mAnchorPosition;

    // set audio block listener
    private void setAudioBlockListener(final AppCompatActivity act, final String audioStr)
    {
        SeekBar seekBarProgress = (SeekBar) rootView.findViewById(R.id.pager_img_audio_seek_bar);
        ImageView mPager_audio_play_button = (ImageView) rootView.findViewById(R.id.pager_btn_audio_play);

        // set audio play and pause control image
        mPager_audio_play_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                isPausedAtSeekerAnchor = false;

                if( (Audio_manager.isRunnableOn_page)||
                    (BackgroundAudioService.mMediaPlayer == null) ) {
                    // use this flag to determine new play or not in note
                    BackgroundAudioService.mIsPrepared = false;
                }
                playAudioInPager(act,audioStr);
            }
        });

        // set seek bar listener
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
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
                    playAudioInPager(act,audioStr);
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
                    TextView audio_curr_pos = (TextView) rootView.findViewById(R.id.pager_audio_current_pos);
                    audio_curr_pos.setText(curr_play_time_str);
                }
            }
        });

        ImageView audioPanel_previous_btn = (ImageView) rootView.findViewById(R.id.audioPanel_previous);
        audioPanel_previous_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPager viewPager = rootView.findViewById(R.id.tabs_pager);
                NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()-1);
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });

        ImageView audioPanel_next_btn = (ImageView) rootView.findViewById(R.id.audioPanel_next);
        audioPanel_next_btn.setOnClickListener(new View.OnClickListener() {
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
    private void playAudioInPager(AppCompatActivity act, String audioStr)
    {
        System.out.println("AudioUi_note / _playAudioInPager");

        if(Audio_manager.getAudioPlayMode()  == Audio_manager.PAGE_PLAY_MODE)
            Audio_manager.stopAudioPlayer();

        String[] audioName = Util.getDisplayNameByUriString(audioStr, act);
        if(UtilAudio.hasAudioExtension(audioStr) ||
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
        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.pager_img_audio_seek_bar);
        int currentPos=0;

        if(BackgroundAudioService.mMediaPlayer != null)
            currentPos = BackgroundAudioService.mMediaPlayer.getCurrentPosition();

        int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
        int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
        String curr_time_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
                                             String.format(Locale.ENGLISH,"%02d", curMin)+":" +
                                             String.format(Locale.ENGLISH,"%02d", curSec);
        TextView audio_curr_pos = (TextView) rootView.findViewById(R.id.pager_audio_current_pos);
        // set current play time and the play length of audio file
        if(audio_curr_pos != null)
        {
            audio_curr_pos.setText(curr_time_str);
        }

        mProgress = (int)(((float)currentPos/ mediaFileLength)*100);

        if(seekBar != null)
            seekBar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
    }

    // update note audio panel
    public void updateAudioPanel_note(AppCompatActivity act)
    {
        System.out.println("AudioUi_note / _updateAudioPanel_note");
        ImageView audio_play_btn = (ImageView) rootView.findViewById(R.id.pager_btn_audio_play);

        if(Audio_manager.getAudioPlayMode() != Audio_manager.NOTE_PLAY_MODE)
            return;

        TextView audioTitle = (TextView) rootView.findViewById(R.id.pager_audio_title);
        TextView audioArtist = (TextView) rootView.findViewById(R.id.pager_audio_artist);
        // update playing state
        if(Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PLAY)
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_pause);
            showAudioName(act);
            audioTitle.setTextColor(ColorSet.getHighlightColor(act) );
            audioTitle.setSelected(true);
            audioArtist.setTextColor(ColorSet.getHighlightColor(act) );
            audioArtist.setSelected(true);
        }
        else if( (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_PAUSE) ||
                (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_STOP)    )
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_play);
            showAudioName(act);
            audioTitle.setTextColor(ColorSet.getPauseColor(act));
            audioTitle.setSelected(false);
            audioArtist.setTextColor(ColorSet.getPauseColor(act));
            audioArtist.setSelected(false);
        }
    }

}