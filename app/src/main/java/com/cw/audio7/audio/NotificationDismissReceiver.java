package com.cw.audio7.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cw.audio7.main.MainAct;
import com.cw.audio7.tabs.TabsHost;

public class NotificationDismissReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		int notificationId = intent.getExtras().getInt("com.cw.audio7.notificationId");
		System.out.println("NotificationDismissReceiver / _onReceive / notificationId = " + notificationId);

		Audio_manager.stopAudioPlayer();
		Audio_manager.removeRunnable();
		Audio_manager.audio7Player.showAudioPanel(MainAct.mAct, false);

		// refresh
		TabsHost.reloadCurrentPage();
	}
}
