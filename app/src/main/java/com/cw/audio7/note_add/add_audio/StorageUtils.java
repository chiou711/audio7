package com.cw.audio7.note_add.add_audio;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;


import com.cw.audio7.main.MainAct;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import static android.content.Context.STORAGE_SERVICE;


// refer to: https://stackoverflow.com/questions/5694933/find-location-of-a-removable-sd-card
public class StorageUtils {

	private static final String TAG = "StorageUtils";

	public static class StorageInfo {

		public final String path;
		public final boolean internal;
		public final boolean readonly;
		public final int display_number;

		StorageInfo(String path, boolean internal, boolean readonly, int display_number) {
			this.path = path;
			this.internal = internal;
			this.readonly = readonly;
			this.display_number = display_number;
		}

		public String getDisplayName() {
			StringBuilder res = new StringBuilder();
			if (internal) {
				res.append("Internal SD card");
			} else if (display_number > 1) {
				res.append("SD card " + display_number);
			} else {
				res.append("SD card");
			}
			if (readonly) {
				res.append(" (Read only)");
			}
			return res.toString();
		}
	}

	public static List<StorageInfo> getStorageList() {

		List<StorageInfo> list = new ArrayList<StorageInfo>();
		String def_path = Environment.getExternalStorageDirectory().getPath();
//		System.out.println("---- def_path = " + def_path);

		String def_path_state = Environment.getExternalStorageState();
//		System.out.println("---- def_path_state = " + def_path_state);

		boolean def_path_internal = !Environment.isExternalStorageRemovable();
//		System.out.println("---- def_path_internal = " + def_path_internal);

		boolean def_path_available = def_path_state.equals(Environment.MEDIA_MOUNTED)
														|| def_path_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
//		System.out.println("---- def_path_available = " + def_path_available);

		boolean def_path_readonly = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
//		System.out.println("---- def_path_readonly = " + def_path_readonly);

		BufferedReader buf_reader = null;

		try {
			HashSet<String> paths = new HashSet<String>();
			buf_reader = new BufferedReader(new FileReader("/proc/mounts"));
			String line;
			int cur_display_number = 1;
			Log.d(TAG, "/proc/mounts");

			while ((line = buf_reader.readLine()) != null) {
				Log.d(TAG, line);
//				System.out.println("---- line = " + line);
				if (line.contains("vfat") || line.contains("/mnt")) {
					StringTokenizer tokens = new StringTokenizer(line, " ");
					String unused = tokens.nextToken(); //device
					String mount_point = tokens.nextToken(); //mount point
					if (paths.contains(mount_point)) {
						continue;
					}
					unused = tokens.nextToken(); //file system
					List<String> flags = Arrays.asList(tokens.nextToken().split(",")); //flags
					boolean readonly = flags.contains("ro");

					if (mount_point.equals(def_path)) {
						paths.add(def_path);
						list.add(0, new StorageInfo(def_path, def_path_internal, readonly, -1));
					} else if (line.contains("/dev/block/vold")) {
						if (!line.contains("/mnt/secure")
								&& !line.contains("/mnt/asec")
								&& !line.contains("/mnt/obb")
								&& !line.contains("/dev/mapper")
								&& !line.contains("tmpfs")) {
							//USB disk is added here
							paths.add(mount_point);
							list.add(new StorageInfo(mount_point, false, readonly, cur_display_number++));
						}
					}
				}
			}

			if (!paths.contains(def_path) && def_path_available) {
				// add /storage/emulated/0 here
				paths.add(def_path);
				list.add(0, new StorageInfo(def_path, def_path_internal, def_path_readonly, -1));
			}

//			System.out.println("paths.size() =" + paths.size() );
//			for(int i=0;i<paths.size();i++ )
//				System.out.println("path["+i+"] =" + paths.toArray()[i] );


		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (buf_reader != null) {
				try {
					buf_reader.close();
				} catch (IOException ex) {}
			}
		}

		return list;
	}

	public static String getVolumeName(String dirName) {
		String volumeName = dirName;

		String[] splits = dirName.split("/");
		for (String split : splits) {
//          System.out.println("-- split = " + split);

			StorageManager storage = (StorageManager) MainAct.mAct.getSystemService(STORAGE_SERVICE);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				List<StorageVolume> volumes = storage.getStorageVolumes();
				for (StorageVolume volume : volumes) {
//                    System.out.println("-- volume.getDescription:" + volume.getDescription(MainAct.mAct));
//                    System.out.println("-- volume.getUuid:" + volume.getUuid());

					if(split.contains("emulated")) {
						if (volume.getUuid() == null) {
							volumeName = volume.getDescription(MainAct.mAct);
//							System.out.println("-- volumeName 1:" + volumeName);
						}
					}
					else if (split.equals(volume.getUuid())) {
						volumeName = volume.getDescription(MainAct.mAct);
//						System.out.println("-- volumeName 2:" + volumeName);
					}
				}
			}
		}
		return volumeName;
	}


	public void getUsbDeviceDetail() {

		UsbManager manager = (UsbManager) MainAct.mAct.getSystemService(Context.USB_SERVICE);

		Context context = MainAct.mAct;

		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();

			String actionString = context.getPackageName()+ ".USB_PERMISSION";

			PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0,
					new Intent(actionString), 0);

			manager.requestPermission(device, mPermissionIntent);

//			System.out.println("-- device.getDeviceName() = " + device.getDeviceName());
//			System.out.println("-- device.getManufacturerName() = " + device.getManufacturerName() );
//			System.out.println("-- device.getProductName() = " + device.getProductName()  );
//			System.out.println("-- device.getProductId() = " + device.getProductId()   );
//			System.out.println("-- device.getDeviceClass() = " + device.getDeviceClass()   );
//			System.out.println("-- device.getDeviceSubclass() = " + device.getDeviceSubclass()   );
		}}

}
