package com.cw.audio7.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cw.audio7.main.MainAct;

import static com.cw.audio7.main.MainAct.mFolderUi;

// cf https://stackoverflow.com/questions/12654820/is-it-possible-to-check-if-a-notification-is-visible-or-canceled
public class NotificationDismissReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		int notificationId = intent.getExtras().getInt("com.cw.audio7.notificationId");
		System.out.println("NotificationDismissReceiver / _onReceive / notificationId = " + notificationId);

		Audio_manager.stopAudioPlayer();
		mFolderUi.tabsHost.audio7Player.showAudioPanel(MainAct.mAct, false);

		// refresh
		mFolderUi.tabsHost.reloadCurrentPage();
	}
}
