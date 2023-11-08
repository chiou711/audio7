package com.cw.audio7.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.cw.audio7.audio.BackgroundAudioService.mAudio_manager;


// cf https://stackoverflow.com/questions/12654820/is-it-possible-to-check-if-a-notification-is-visible-or-canceled
public class NotificationDismissReceiver extends BroadcastReceiver {
	// onReceive when Remove notification
	@Override
	public void onReceive(Context context, Intent intent) {
		int notificationId = intent.getExtras().getInt("com.cw.audio7.notificationId");
		System.out.println("NotificationDismissReceiver / _onReceive / notificationId = " + notificationId);

		{
			if(mAudio_manager != null) {
				mAudio_manager.stopAudioPlayer();
				mAudio_manager.audio7Player.showAudioPanel(false);
			}

			// refresh //todo Check more
//			mFUI.tabsHost.reloadCurrentPage();
		}
	}
}
