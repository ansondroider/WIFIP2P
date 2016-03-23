package com.ansondroider.wifip2p.utils;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

/**
 * com.ansondroider.wifip2p.utils
 * Created by anson on 16-3-18.
 */
public class WifiP2pTransfer {
    String TAG = "WifiP2pTransfer";
    //WifiP2pProxy proxy;
    //String host;
    public static final int HOSTPORT = 9901;
    public static final int CLIENTPORT = 9902;

    public Object listenerLock = new Object();

    public static final String SELF = "127.0.0.1";

    int receivePort = 0;
    int sendPort = 0;

    boolean isGroupOwner;
    String groupHost = "127.0.0.1";
    String receiveHost = "127.0.0.1";
    String sendHost = "127.0.0.1";

    boolean transfering = false;
    boolean pause = false;
    boolean stop = false;

    final int SOCKET_TIMEOUT = 3000;

    WifiP2pHandler handler;

    public WifiP2pTransfer(WifiP2pInfo info, WifiP2pDevice thisDev) {
        //host = "127.0.0.1"; //info.groupOwnerAddress.getHostAddress();
        Log.d(TAG, "WifiP2pTransfer isGroupOWner = " + info.isGroupOwner);
        isGroupOwner = info.isGroupOwner;
        groupHost = info.groupOwnerAddress.getHostAddress();
        receiveHost = isGroupOwner ? info.groupOwnerAddress.getHostAddress() : getP2PIpAddress(thisDev.deviceAddress, groupHost);
        sendHost = info.groupOwnerAddress.getHostAddress();
        receivePort = HOSTPORT; //info.isGroupOwner ? HOSTPORT : CLIENTPORT;
        sendPort = HOSTPORT;//info.isGroupOwner ? CLIENTPORT : HOSTPORT;
    }

    public void setHandler(WifiP2pHandler h){
        handler = h;
    }

    Thread listener = null;
    public void startListener(){
        Log.d(TAG, "startListener");
        listener = new Thread(){
            @Override
            public void run() {
                try {
                    synchronized (listenerLock) {
                        ServerSocket serverSocket = new ServerSocket(receivePort);
                        Log.d(TAG, "Server: Socket opened");
                        if (!isGroupOwner) {
                            sleep(300);
                            sendCMD(CMD_REGISTE, groupHost);
                        }

                        while (!stop && !Thread.currentThread().isInterrupted()) {
                            Socket client = serverSocket.accept();
                            String clientIp = client.getInetAddress().getHostAddress();
                            Log.d(TAG, "clientIp = " + clientIp);

                            handleRequest(client);
                        }
                        Log.d(TAG, "Server: Socket close..");
                        serverSocket.close();
                        listenerLock.notify();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();//从新设置线程的中断标志
                }


            }
        };
        listener.start();
    }

    public void stopListener(){
        Log.d(TAG, "stopListener");
        stop = true;
        pause = true;
        listener.interrupt();
        sendCMD(-1, SELF);
    }

    public void handleRequest(Socket client){
        Log.d(TAG, "-------------handleRequest----------------");
        try {
            InputStream inputStream = client.getInputStream();
            Request r = new Request(inputStream);

            if(r.type == CMD_TYPE_FILE) {
                Log.d(TAG, "receiveFile " + r.msg);
                String name = r.msg;
                File f = new File("/mnt/sdcard/" + name);
                FileOutputStream fos = new FileOutputStream(f);
                byte buf[] = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
                if(handler != null)handler.sendMessage(handler.obtainMessage(handler.P2PMSG_NEWFILE, f.getAbsolutePath()));
                Log.d(TAG, "handleRequest:" + f.getAbsolutePath());
                fos.flush();
                fos.close();
            }else if(r.type == CMD_TYPE_CMD){
                Log.d(TAG, "receive cmd " + r.msg);
                if(String.valueOf(CMD_REGISTE).equals(r.msg)){
                    sendHost = client.getInetAddress().getHostAddress();
                    if(handler != null)handler.sendMessage(handler.obtainMessage(handler.P2PMSG_REGISTER, sendHost));
                }
            }else if(r.type == CMD_TYPE_IM){
                Log.d(TAG, "new message in " + r.msg);
                if(handler != null)handler.sendMessage(handler.obtainMessage(handler.P2PMSG_NEWMESSAGE, r.msg));
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendInstanceMessage(final String im){
        Log.d(TAG, "sendInstanceMessage");
        new Thread() {
            @Override
            public void run() {

                Socket socket = new Socket();
                try {
                    Log.d(TAG, "sendInstanceMessage Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(sendHost, receivePort)), SOCKET_TIMEOUT);

                    Log.d(TAG, "sendInstanceMessage Client socket - " + socket.isConnected());
                    OutputStream os = socket.getOutputStream();
                    String content = CMD_TYPE_IM + ";;" + im + ";;" + "0\n";
                    os.write(content.getBytes());
                    os.flush();
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // Give up
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }.start();
    }

    public void sendCMD(final int cmd, final String host){
        Log.d(TAG, "sendCMD");
        new Thread() {
            @Override
            public void run() {

                Socket socket = new Socket();
                try {
                    Log.d(TAG, "sendCMD Opening socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, receivePort)), SOCKET_TIMEOUT);

                    Log.d(TAG, "sendCMD Client socket - " + socket.isConnected());
                    OutputStream os = socket.getOutputStream();
                    String content = CMD_TYPE_CMD + ";;" + cmd + ";;" + "0\n";
                    os.write(content.getBytes());
                    os.flush();
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // Give up
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }.start();
    }

    public void sendFile(final File src) {
        Log.d(TAG, "sendFile");
        new Thread() {
            @Override
            public void run() {

                Socket socket = new Socket();
                try {
                    Log.d(TAG, "sendFile Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(sendHost, sendPort)), SOCKET_TIMEOUT);

                    Log.d(TAG, "sendFile Client socket - " + socket.isConnected());
                    OutputStream os = socket.getOutputStream();
                    InputStream is = new FileInputStream(src);
                    transfering = true;
                    pause = false;

                    String header = (CMD_TYPE_FILE + ";;");
                    header += src.getName() + ";;";
                    header += src.length() + "\n";

                    os.write(header.getBytes());
                    int len;
                    byte buf[] = new byte[1024];
                    while ((len = is.read(buf)) != -1) {
                        os.write(buf, 0, len);
                        if(pause)break;
                    }
                    os.flush();
                    os.close();
                    is.close();
                    Log.d(TAG, "sendFile Client: " + src.getAbsolutePath() + " Data written");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // Give up
                                e.printStackTrace();
                            }
                        }
                    }
                }
                transfering = false;

            }
        }.start();
    }

    public void pause(){
        Log.d(TAG, "pause");
        this.pause = true;
    }

    public static final int CMD_TYPE_IM = 0;
    public static final String CMD_IM = "IM";
    public static final int CMD_TYPE_FILE = 1;
    public static final String CMD_FILE = "FILE";
    public static final int CMD_TYPE_CMD = 2;//start, stop, pause, update...
    public static final String CMD_CMD = "CMD";

    public static final int CMD_REGISTE = 0x11;

    public static class Request{
        String TAG = "Request";
        int type;
        String msg;
        long size;

        public Request(InputStream is){
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();

                String[] header = line.split(";;");
                type = Integer.valueOf(header[0]);
                msg = header[1];
                size = Long.valueOf(header[2]);

                Log.d(TAG, type + "::" + msg + "::" + size);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getP2PIpAddress(String macAddr, String groupOwnerAddress) {
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                /** find p2p only **/
                if (!intf.getName().contains("p2p"))
                    continue;

                //Log.v("getIpAddress", intf.getName() + ":" + macAddr);

                List<InetAddress> addrs = Collections.list(intf
                        .getInetAddresses());

                for (InetAddress addr : addrs) {

                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        String prxGroup = groupOwnerAddress.substring(0, groupOwnerAddress.lastIndexOf("."));
                        if(sAddr.startsWith(prxGroup)){
                            return sAddr;
                        }
                    }

                }
            }

        } catch (Exception ex) {
            Log.v("getIpAddress", "error in parsing");
        } // for now eat exceptions
        Log.v("getIpAddress", "returning empty ip address");
        return "";
    }
}
