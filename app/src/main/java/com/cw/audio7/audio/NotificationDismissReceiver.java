package com.cw.audio7.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.cw.audio7.main.MainAct.mFolderUi;

// cf https://stackoverflow.com/questions/12654820/is-it-possible-to-check-if-a-notification-is-visible-or-canceled
public class NotificationDismissReceiver extends BroadcastReceiver {
	// onReceive when Remove notification
	@Override
	public void onReceive(Context context, Intent intent) {
		int notificationId = intent.getExtras().getInt("com.cw.audio7.notificationId");
		System.out.println("NotificationDismissReceiver / _onReceive / notificationId = " + notificationId);

		if(mFolderUi.tabsHost != null ) {
			mFolderUi.tabsHost.stopAudioPlayer();

			// refresh
			mFolderUi.tabsHost.reloadCurrentPage();
		}
	}
}
