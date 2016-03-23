package com.ansondroider.wifip2p;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;

import com.anson.acode.ALog;
import com.ansondroider.wifip2p.FileListDialog.OnFileSelectedListener;
import com.ansondroider.wifip2p.utils.MessageAdapter;
import com.ansondroider.wifip2p.utils.PeersAdapter;
import com.ansondroider.wifip2p.utils.WPGlobal;
import com.ansondroider.wifip2p.utils.WifiP2pHandler;
import com.ansondroider.wifip2p.utils.WifiP2pProxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends Activity implements OnItemClickListener, OnFileSelectedListener, AdapterView.OnItemLongClickListener{
	final String TAG = "ActivityMain";

	ListView lv_peers;
	ListView lv_messages;
	List<WifiP2pDevice> devices = new ArrayList<WifiP2pDevice>();
	PeersAdapter peerAdapter;
	MessageAdapter msgAdapter;

	H mHandler;
	WifiP2pProxy p2pProxy;
	
	class H extends WifiP2pHandler{
		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			switch(msg.what){
			case WPGlobal.MSG_P2P_PEERS_CHANGED:
				refreshList();
				break;
			}
		}

		@Override
		public void showMessage(String msg) {
			showMessageIn(msg);
		}

		@Override
		public void showNewFile(String file) {
			showNewFileIn(file);
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mHandler = new H();

		p2pProxy = new WifiP2pProxy(this);

		p2pProxy.setStateChangedListener(stateListener);
		p2pProxy.setHandler(mHandler);
		
		init();
	}
	
	protected void onDestroy() {
		p2pProxy.release();
		super.onDestroy();
	};
	
	StateReceiver receiver;
	void init(){
		lv_peers = (ListView)findViewById(R.id.lv_peers);
		lv_messages = (ListView)findViewById(R.id.lv_files);
		peerAdapter = new PeersAdapter(this, devices);
		lv_peers.setAdapter(peerAdapter);
		lv_peers.setOnItemClickListener(this);
		lv_peers.setOnItemLongClickListener(this);
		msgAdapter = new MessageAdapter(this);
		lv_messages.setAdapter(msgAdapter);
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

	}

	
	void refreshList(){
		devices.clear();
		WeakReference<WifiP2pDeviceList> peers = new WeakReference<WifiP2pDeviceList>(p2pProxy.getDeviceList());
		for (WifiP2pDevice peer: peers.get().getDeviceList()) {
			devices.add(peer);
		}

		peerAdapter.notifyDataSetChanged();
	}

	ProgressDialog connectPeerDialog = null;
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
		final WifiP2pDevice dev = devices.get(pos);
		if(dev.status == WifiP2pDevice.CONNECTED){
			//send file...
			//FileListDialog dialog = new FileListDialog(this, this);
			//dialog.show();

			editMessage();
			return;
		}

		connectPeerDialog = new ProgressDialog(this);
		connectPeerDialog.setTitle(R.string.dialog_connect_title);
		connectPeerDialog.setMessage(getString(R.string.dialog_connect_msg, dev.deviceName));
		connectPeerDialog.show();
		p2pProxy.connectToPeer(dev, new ActionListener() {
			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				connectPeerDialog.dismiss();
				ALog.d(TAG, "connectToPeer " + dev.deviceName + " success");
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
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {
		final WifiP2pDevice dev = devices.get(pos);
		if(dev.status == WifiP2pDevice.CONNECTED){
			p2pProxy.disconnectToPeer(new ActionListener() {
				@Override
				public void onSuccess() {
					Log.d(TAG, "disconnect success");
				}

				@Override
				public void onFailure(int i) {
					Log.d(TAG, "disconnect failed");
				}
			});
		}
		return true;
	}

	@Override
	public void onFileSelected(String file) {
		p2pProxy.sendFile(file);

	}

	WifiP2pProxy.StateChangedListener stateListener = new WifiP2pProxy.StateChangedListener() {
		@Override
		public void onInited() {
			String name = p2pProxy.getDeviceName();
			getActionBar().setTitle(name);
		}

		@Override
		public void onPeersDiscovery(WifiP2pDeviceList peers) {
			Log.d(TAG, "onPeersDiscovery:" + peers.getDeviceList().size());
			mHandler.sendEmptyMessage(WPGlobal.MSG_P2P_PEERS_CHANGED);
		}

		@Override
		public void onConnected() {

		}

		@Override
		public void onError() {

		}
	};

	void editMessage(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter message:");
		final EditText editor = new EditText(this);
		editor.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		builder.setView(editor);
		builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				sendMessage(editor.getText().toString());
			}
		});

		builder.create().show();
	}

	void sendMessage(String msg){
		MessageAdapter.ShortMessage sm = new MessageAdapter.ShortMessage("ME", msg, MessageAdapter.ShortMessage.MSG_TYPE_SEND);
		msgAdapter.addMessage(sm);

		p2pProxy.sendMessage(msg);
	}

	void showMessageIn(String msg){
		MessageAdapter.ShortMessage sm = new MessageAdapter.ShortMessage("YOU", msg, MessageAdapter.ShortMessage.MSG_TYPE_RECEIVE);
		msgAdapter.addMessage(sm);
	}

	void showNewFileIn(String file){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("File received");
		builder.setMessage(file);
		builder.create().show();
	}

}
