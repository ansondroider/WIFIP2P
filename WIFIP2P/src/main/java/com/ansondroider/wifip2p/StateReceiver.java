package com.ansondroider.wifip2p;

import com.anson.acode.ALog;
import com.ansondroider.wifip2p.utils.WPGlobal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;

public class StateReceiver extends BroadcastReceiver {
	
	Handler h = null;
	WifiP2pManager p2pMgr;
	
	public StateReceiver(Handler h){
		this.h = h;
		//this.p2pMgr = p2pMgr;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
			h.sendEmptyMessage(WPGlobal.MSG_WIFI_STATE);
		}else if(action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)){
			
		}else if(action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)){
			ALog.d(action);
			h.sendEmptyMessage(WPGlobal.MSG_P2P_PEERS_CHANGED);
		}else if(action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)){
			
		}else if(action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)){
			
		}
	}

}
