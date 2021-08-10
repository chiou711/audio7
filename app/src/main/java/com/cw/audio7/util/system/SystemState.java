package com.cw.audio7.util.system;

import android.content.Context;
import android.os.PowerManager;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

public class SystemState {

	// is Screen on
	public static boolean isScreenOn(AppCompatActivity act) {
		PowerManager pm = (PowerManager) act.getSystemService(Context.POWER_SERVICE);
		boolean isScreenOn = Objects.requireNonNull(pm).isScreenOn();
//		System.out.println("SystemState / _isScreenOn / isScreenOn = " + isScreenOn);
		return isScreenOn;
	}

	// is Screen off
	public static boolean isScreenOff(AppCompatActivity act) {
		return !isScreenOn(act);
	}


}
