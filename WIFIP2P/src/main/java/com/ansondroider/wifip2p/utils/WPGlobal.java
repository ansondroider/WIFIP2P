package com.ansondroider.wifip2p.utils;

import android.app.Application;

public class WPGlobal extends Application {
	
	public final static int MSG_WIFI_STATE = 0;
	public final static int MSG_P2P_STOP_SEARCH = 5;
	public final static int MSG_P2P_PEERS_CHANGED = 7;
	
	public final static String EXTRA_WIFI_STATE = "wifi_state";
	public final static String EXTRA_FILE_GET = "file";
	
	public final static int INTERVAL_SEARCH = 10 * 1000;
	
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}
}
