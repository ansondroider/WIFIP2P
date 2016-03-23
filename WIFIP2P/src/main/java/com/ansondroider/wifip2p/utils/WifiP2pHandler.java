package com.ansondroider.wifip2p.utils;

import android.os.Handler;
import android.os.Message;

/**
 * com.ansondroider.wifip2p.utils
 * Created by anson on 16-3-21.
 */
abstract public class WifiP2pHandler extends Handler {

    public static final String TAG = "WifiP2pHandler";

    public static final int P2PMSG_NEWMESSAGE = 4001;
    public static final int P2PMSG_NEWFILE = 4002;
    public static final int P2PMSG_REGISTER = 4003;

    public static final String EXTRA_IM = "im";
    public static final String EXTRA_FILE = "file";
    public static final String EXTRA_DEVICE_NAME = "devName";
    public static final String EXTRA_DEVICE_ADDR = "devAddr";

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what){
            case P2PMSG_NEWMESSAGE:
                showMessage((String)msg.obj);
                break;
            case P2PMSG_NEWFILE:
                showNewFile((String)msg.obj);
                break;
            case P2PMSG_REGISTER:
                break;
        }
    }

    public abstract void showMessage(String msg);
    public abstract void showNewFile(String file);
}
