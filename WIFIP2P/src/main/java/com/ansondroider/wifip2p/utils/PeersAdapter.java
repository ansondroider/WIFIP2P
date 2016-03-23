package com.ansondroider.wifip2p.utils;

import java.util.Collection;
import java.util.List;

import com.anson.acode.ALog;
import com.ansondroider.wifip2p.R;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PeersAdapter extends BaseAdapter {
	List<WifiP2pDevice> devices;
	
	Context cxt;
	LayoutInflater flater;
	public PeersAdapter(Context context, List<WifiP2pDevice> peers){
		cxt = context;
		flater = LayoutInflater.from(context);
		this.devices = peers;
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return devices == null ? 0 : devices.size();
	}

	@Override
	public Object getItem(int pos) {
		// TODO Auto-generated method stub
		return devices == null ? null : devices.get(pos);
	}

	@Override
	public long getItemId(int pos) {
		// TODO Auto-generated method stub
		return pos;
	}

	@Override
	public View getView(int pos, View currentView, ViewGroup parent) {
		// TODO Auto-generated method stub
		Item item;
		if(currentView != null && currentView.getTag() != null){
			item = (Item)currentView.getTag();
		}else{
			View v = flater.inflate(R.layout.list_item_peer, null);
			TextView tv = (TextView)v.findViewById(R.id.tv_name);
			ImageView iv = (ImageView)v.findViewById(R.id.iv_header);
			item = new Item();
			item.header = iv;
			item.name = tv;
			v.setTag(item);
			currentView = v;
		}
		//ALog.d("getView " + devices.get(pos).deviceName);
		int status = devices.get(pos).status;
		int color = (status == WifiP2pDevice.CONNECTED) ? Color.GREEN : (status == WifiP2pDevice.INVITED ? Color.BLUE : Color.WHITE);
		item.name.setTextColor(color);
		item.name.setText(devices.get(pos).deviceName + "\n" + devices.get(pos).deviceAddress);
		return currentView;
	}
	
	class Item{
		ImageView header;
		TextView name;
	}

}
