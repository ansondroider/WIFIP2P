package com.ansondroider.wifip2p.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.anson.acode.ALog;

import java.io.File;

/**
 * com.ansondroider.wifip2p.utils
 * Created by anson on 16-3-17.
 */
public class WifiP2pProxy extends BroadcastReceiver{
    final String TAG = "WifiP2pProxy";
    Context context;
    WifiManager wifiMgr;
    WifiP2pManager p2pManager;
    WifiP2pManager.Channel p2pChannel;

    WifiP2pTransfer p2pTransfer;

    WifiP2pDeviceList peers;
    WifiP2pHandler mHandler;

    public static final int STATE_DISABLE = -1;
    public static final int STATE_ENABLED = 0;
    public static final int STATE_ACTIVED = 1;
    public static final int STATE_CONNECTED = 2;

    WifiP2pDevice thisDevice = null;
    String myName = "";
    boolean mWifiP2pEnabled = false;
    boolean canConnectThirdDevice = true;

    boolean wifiEnabledBeforeStart = false;

    int state = STATE_DISABLE;
    int discoveryState = WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED;
    //init p2p manager
    public WifiP2pProxy(Context ctx){

        context = ctx;
        wifiMgr = (WifiManager)ctx.getSystemService(ctx.WIFI_SERVICE);
        //save wifi state.
        wifiEnabledBeforeStart = wifiMgr.isWifiEnabled();
        if(!wifiEnabledBeforeStart){
            wifiMgr.setWifiEnabled(true);
        }else{
            initP2p();
        }


        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        //mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION);
        ctx.registerReceiver(this, mIntentFilter);
    }

    void initP2p(){
        Log.d(TAG, "initP2p");
        state = STATE_ENABLED;
        peers = new WifiP2pDeviceList();
        p2pManager = (WifiP2pManager)context.getSystemService(context.WIFI_P2P_SERVICE);
        //init Channel...
        p2pChannel = p2pManager.initialize(context, context.getMainLooper(), new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                ALog.alog("initialize onChannelDisconnected");
            }
        });

        active();
    }


    public String getDeviceName(){
        return thisDevice == null ? "" : thisDevice.deviceName;
    }

    public WifiP2pDevice getThisDevice(){
        return thisDevice;
    }

    public WifiP2pDeviceList getDeviceList(){
        return peers;
    }

    //active device discovery
    //MUST active, then other peer can find me
    public void active(){
        Log.d(TAG, "activity");
        p2pManager.discoverPeers(p2pChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                ALog.d(TAG, "discoverPeers onSuccess");
            }

            @Override
            public void onFailure(int result) {
                ALog.d(TAG, "discoverPeers onFailure " + result);
            }
        });
        state = STATE_ACTIVED;
    }

    //no use ????
    public void requestPeers(WifiP2pManager.PeerListListener peerListener) {
        Log.d(TAG, "requestPeers");
        p2pManager.requestPeers(p2pChannel, peerListener);
    }

    /**
     * connect to device...
     * @param dev wifip2p device
     * @param actionListener result callback
     */
    public void connectToPeer(final WifiP2pDevice dev, WifiP2pManager.ActionListener actionListener){
        Log.d(TAG, "connectToPeer");
        if(state == STATE_CONNECTED && !canConnectThirdDevice){
            Log.e(TAG, "already connect to another group!!!!");
            return;
        }
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = dev.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        p2pManager.connect(p2pChannel, config, actionListener);
    }

    public void disconnectToPeer(WifiP2pManager.ActionListener actionListener){
        Log.d(TAG, "disconnectToPeer");
        /** FUCK!!! why not cancelConnect(...) **/
        //p2pManager.cancelConnect(p2pChannel, actionListener);
        p2pManager.removeGroup(p2pChannel, actionListener);
    }

    //mabe, deactive when app exit
    public void deactive(){
        Log.d(TAG, "deactive");
        if(p2pManager != null){
            p2pManager.stopPeerDiscovery(p2pChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    ALog.d(TAG, "stopPeerDiscovery onSuccess");
                }

                @Override
                public void onFailure(int result) {
                    ALog.d(TAG, "stopPeerDiscovery onFailure " + result);
                }
            });
        }
        state = STATE_ENABLED;
    }

    public void release(){
        Log.d(TAG, "release");
        if(p2pTransfer != null)p2pTransfer.stopListener();
        context.unregisterReceiver(this);
        if(state == STATE_CONNECTED){
            synchronized (p2pTransfer.listenerLock) {
                try {
                    p2pTransfer.listenerLock.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                disconnectToPeer(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        ALog.d(TAG, "disconnect Success");
                    }

                    @Override
                    public void onFailure(int i) {
                        ALog.d(TAG, "disconnect failed");
                    }
                });
            }
        }
        deactive();
        //restore wifi state.
        if(!wifiEnabledBeforeStart)wifiMgr.setWifiEnabled(wifiEnabledBeforeStart);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, action);

        if(action.equals(wifiMgr.WIFI_STATE_CHANGED_ACTION)){
            if(!wifiEnabledBeforeStart && !mWifiP2pEnabled){
                //initP2p();
            }
        }else if(action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
            /**
             * init p2p info when p2p is enabled!!
             */
            mWifiP2pEnabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,
                    WifiP2pManager.WIFI_P2P_STATE_DISABLED) == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
            if(mWifiP2pEnabled && state < 0){
                initP2p();
            }

        }else if(action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
            /**
             * when find device, will calll this;
             * and can get device list from WifiP2pDeviceList.
             */
            peers = (WifiP2pDeviceList) intent.getParcelableExtra(EXTRA_P2P_DEVICE_LIST);
            if(stateChangedListener != null)stateChangedListener.onPeersDiscovery(peers);

        }else if(action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
            if (p2pManager == null) return;
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pInfo wifip2pinfo = (WifiP2pInfo) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            if(networkInfo.isConnected()){
                state = STATE_CONNECTED;
                if (p2pManager != null) {
                    p2pManager.requestGroupInfo(p2pChannel, new WifiP2pManager.GroupInfoListener() {
                        @Override
                        public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                            Log.d(TAG, wifiP2pGroup.toString());
                            updateGroupInfo(wifiP2pGroup);
                        }
                    });
                }

                /** get connection info and then we can create Socket **/
                p2pManager.requestConnectionInfo(p2pChannel, connetionInfoListener);
            }else{
                /** connection lose, stop Server socket **/
                if(p2pTransfer != null)p2pTransfer.stopListener();
            }
            /** update status of devices **/
            if(stateChangedListener != null)stateChangedListener.onPeersDiscovery(peers);

            if(discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED && peers.getDeviceList().size() == 0){
                active();
            }

        }else if(action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
            //after wifip2p started!
            thisDevice = (WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            if(stateChangedListener != null) stateChangedListener.onInited();

            if(thisDevice.status == WifiP2pDevice.CONNECTED){
                /** device is connected **/
                state = STATE_CONNECTED;
                if(stateChangedListener != null){
                    stateChangedListener.onConnected();
                }
            }else{
                /** device is disconnected **/
                if(state != STATE_ACTIVED){
                    active();
                }
            }

        }else if(action.equals(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)){
            int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
            this.discoveryState = discoveryState;
            if(WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED == discoveryState){
                //requestPeers(null);
                Log.d(TAG, "WIFI_P2P_DISCOVERY_STARTED");
            }else{
                /**
                 * retry discovery
                 * if already connect. should NOT go discovery...
                 */
                ///if(state != STATE_CONNECTED)active();

                Log.d(TAG, "WIFI_P2P_DISCOVERY_STOPPED");
            }
        }
    }

    void updateGroupInfo(WifiP2pGroup group){
        boolean addToPeers = peers.getDeviceList().size() == 0;
        //for(WifiP2pDevice dev : group.getClientList()){
            //Log.d(TAG, dev.deviceName + "," + dev.deviceAddress + "," + WifiP2pTransfer.getP2PIpAddress(dev.deviceAddress, "192.168.49.1"));
        //    peers.getDeviceList().add(dev);
        //}
        if(addToPeers){
            requestPeers(new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                    peers = wifiP2pDeviceList;
                    if(stateChangedListener != null)stateChangedListener.onPeersDiscovery(peers);
                }
            });
        }
    }

    StateChangedListener stateChangedListener;
    public void setStateChangedListener(StateChangedListener stateChangedListener){
        this.stateChangedListener = stateChangedListener;
    }
    public interface StateChangedListener{
        void onInited();
        void onPeersDiscovery(WifiP2pDeviceList peers);
        void onConnected();
        void onError();
    }

    public void sendFile(String file){
        if(state == STATE_CONNECTED){
            p2pTransfer.sendFile(new File(file));
        }else{
            Log.e(TAG, "no connection");
        }
    }

    public void sendMessage(String msg){
        if(state == STATE_CONNECTED){
            p2pTransfer.sendInstanceMessage(msg);
        }else{
            Log.e(TAG, "no connection");
        }
    }

    public void setHandler(WifiP2pHandler handler){
        mHandler = handler;
    }


    WifiP2pManager.ConnectionInfoListener connetionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            canConnectThirdDevice = wifiP2pInfo.isGroupOwner;
            p2pTransfer = new WifiP2pTransfer(wifiP2pInfo, thisDevice);
            p2pTransfer.startListener();
            p2pTransfer.setHandler(mHandler);
        }
    };


    public static final String EXTRA_P2P_DEVICE_LIST = "wifiP2pDeviceList";

    /**
     * after connected
     * android.net.wifi.p2p.THIS_DEVICE_CHANGED
     * android.net.wifi.p2p.CONNECTION_STATE_CHANGE
     */

}
