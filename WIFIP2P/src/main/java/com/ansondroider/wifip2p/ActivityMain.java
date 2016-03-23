package com.ansondroider.wifip2p;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.anson.acode.ALog;
import com.ansondroider.wifip2p.FileListDialog.OnFileSelectedListener;
import com.ansondroider.wifip2p.utils.PeersAdapter;
import com.ansondroider.wifip2p.utils.WPGlobal;

import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.IntentFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;

public class ActivityMain extends Activity implements OnItemClickListener, OnFileSelectedListener{
	final String TAG = "ActivityMain";
	WifiManager wifiMgr;
	WifiP2pManager p2pMgr;
	ImageButton ib_search;
	Channel p2pChannel;
	
	ListView lv_peers;
	List<WifiP2pDevice> devices = new ArrayList<WifiP2pDevice>();
	PeersAdapter peerAdapter;

	H mHandler;
	
	class H extends Handler{
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case WPGlobal.MSG_WIFI_STATE:
				ib_search.setEnabled(wifiMgr.isWifiEnabled());
				if(!wifiMgr.isWifiEnabled())stopSearchPeers();
				break;
			case WPGlobal.MSG_P2P_STOP_SEARCH:
				stopSearchPeers();
				break;
			case WPGlobal.MSG_P2P_PEERS_CHANGED:
				refreshList();
				break;
			}
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mHandler = new H();
		wifiMgr = (WifiManager)getSystemService(WIFI_SERVICE);
		p2pMgr = (WifiP2pManager)getSystemService(WIFI_P2P_SERVICE);

		initActionBar();
		
		init();
	}
	
	protected void onDestroy() {
		stopSearchPeers();
		super.onDestroy();
		unregisterReceiver(receiver);
	};
	
	StateReceiver receiver;
	void init(){
		lv_peers = (ListView)findViewById(R.id.lv_peers);
		peerAdapter = new PeersAdapter(this, devices);
		lv_peers.setAdapter(peerAdapter);
		lv_peers.setOnItemClickListener(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		
		receiver = new StateReceiver(mHandler);

		registerReceiver(receiver, filter);

		p2pChannel = p2pMgr.initialize(this, getMainLooper(), new ChannelListener() {
			@Override
			public void onChannelDisconnected() {
				// TODO Auto-generated method stub
				ALog.alog("initialize _onChannelDisconnected");
			}
		});
	}
	
	void initActionBar(){
		ActionBar action = getActionBar();
        View v = LayoutInflater.from(this).inflate(R.layout.action_menu, null);
        action.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM);
        action.setCustomView(v, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        Switch mSwitch = (Switch)findViewById(R.id.sw_wifi);
        mSwitch.setChecked(wifiMgr.isWifiEnabled());
        mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton cb, boolean checked) {
				// TODO Auto-generated method stub
				//Open or Close;
				wifiMgr.setWifiEnabled(checked);
			}
		});

        ib_search = (ImageButton)v.findViewById(R.id.ib_search);
        ib_search.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startSearchPeers();
			}
		});

        ib_search.setEnabled(wifiMgr.isWifiEnabled());
	}
	
	ProgressDialog progressDialog;
	void startSearchPeers(){
		ALog.d(TAG, "startSearchPeers");
		/**progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(R.string.dialog_progress_title);
		progressDialog.setMessage(getString(R.string.dialog_progress_msg));
		progressDialog.show();**/
		ib_search.setEnabled(false);
		p2pMgr.discoverPeers(p2pChannel, new ActionListener() {

			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				ALog.d(TAG, "discoverPeers onSuccess");
			}

			@Override
			public void onFailure(int result) {
				// TODO Auto-generated method stub
				ALog.d(TAG, "discoverPeers onFailure " + result);
			}
		});

		/** do NOT stop discorvery till device connected !!**/
		//mHandler.sendEmptyMessageDelayed(WPGlobal.MSG_P2P_STOP_SEARCH, WPGlobal.INTERVAL_SEARCH);
	}

	void stopSearchPeers(){
		ALog.d(TAG, "stopSearchPeers");
		ib_search.setEnabled(true);
		if(p2pMgr != null){
			p2pMgr.stopPeerDiscovery(p2pChannel, new ActionListener() {

				@Override
				public void onSuccess() {
					// TODO Auto-generated method stub
					ALog.d(TAG, "stopPeerDiscovery onSuccess");
				}

				@Override
				public void onFailure(int result) {
					// TODO Auto-generated method stub
					ALog.d(TAG, "stopPeerDiscovery onFailure " + result);
				}
			});
		}
	}
	
	void refreshList(){
		devices.clear();
		p2pMgr.requestPeers(p2pChannel, new PeerListListener() {
			
			@Override
			public void onPeersAvailable(WifiP2pDeviceList peers) {
				// TODO Auto-generated method stub
				ALog.d("onPeersAvailable " + peers.getDeviceList().size());
				Iterator<WifiP2pDevice> itera= peers.getDeviceList().iterator();
				while(itera.hasNext()){
					devices.add(itera.next());
				}
				
				peerAdapter.notifyDataSetChanged();

			}
		});


	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
		// TODO Auto-generated method stub
		//FileListDialog dialog = new FileListDialog(this);
		//dialog.show();
		connectToPeer(devices.get(pos));
	}
	
	ProgressDialog connectPeerDialog = null;
	void connectToPeer(final WifiP2pDevice dev){
		connectPeerDialog = new ProgressDialog(this);
		connectPeerDialog.setTitle(R.string.dialog_connect_title);
		connectPeerDialog.setMessage(getString(R.string.dialog_connect_msg, dev.deviceName));
		connectPeerDialog.show();
		WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = dev.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        p2pMgr.connect(p2pChannel, config, new ActionListener() {
			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				connectPeerDialog.dismiss();
				FileListDialog dialog = new FileListDialog(ActivityMain.this);
				dialog.show();
				ALog.d(TAG, "connectToPeer " + dev.deviceName + " success");
				stopSearchPeers();
			}
			@Override
			public void onFailure(int result) {
				// TODO Auto-generated method stub
				connectPeerDialog.dismiss();
				ALog.d(TAG, "connectToPeer " + dev.deviceName + " failed");
			}
		});
        
	}
	
	@Override
	public void onFileSelected(String file) {
		// TODO Auto-generated method stub
		sendFile(file);
	}
	
	void sendFile(String f){
		//p2pMgr.connect(p2pChannel, WifiP2pConfig., listener);
	}

}
