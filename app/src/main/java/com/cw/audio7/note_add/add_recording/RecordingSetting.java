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

package com.cw.audio7.note_add.add_recording;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.cw.audio7.R;
import com.cw.audio7.util.preferences.Pref;

import androidx.fragment.app.Fragment;

public class RecordingSetting extends Fragment
{
	public RecordingSetting(){}
	static View mRootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		System.out.println("================ RecordingSetting / onCreateView ==================");

		mRootView = inflater.inflate(R.layout.recording_setting, container, false);

		setAudioRecQualityOption();

		return mRootView;
	}   	

	/**
	 *  set audio recorder quality option
	 *  
	 */

	CheckedTextView textView;
	void setAudioRecQualityOption()
	{
		//  set current
		boolean isHighQuality = Pref.getPref_recorder_high_quality(getActivity());
		textView = mRootView.findViewById(R.id.RecordingQualityTitle);
		
		if(isHighQuality)
			textView.setChecked(true);
		else
			textView.setChecked(false);

		// Select new 
		textView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean currentCheck = ((CheckedTextView)textView).isChecked();
				((CheckedTextView)textView).setChecked(!currentCheck);

				if(((CheckedTextView)textView).isChecked())
					Pref.setPref_recorder_high_quality(getActivity(),true);
				else
					Pref.setPref_recorder_high_quality(getActivity(),false);
			}
		});
	}

}