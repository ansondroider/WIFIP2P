package com.ansondroider.wifip2p.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ansondroider.wifip2p.R;

import java.util.ArrayList;

/**
 * com.ansondroider.wifip2p.utils
 * Created by anson on 16-3-21.
 */
public class MessageAdapter extends BaseAdapter {
    ArrayList<ShortMessage> messages = new ArrayList<ShortMessage>();
    Context ctx = null;
    LayoutInflater inflater = null;
    public MessageAdapter(Context ctx){
        this.ctx = ctx;
        inflater = LayoutInflater.from(ctx);
    }

    public void addMessage(ShortMessage sm){
        messages.add(sm);
        notifyDataSetChanged();
    }

    public void clearMessage(){
        messages.clear();
        notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ShortMessage sm = messages.get(i);
        Holder holder = null;
        if(view == null){
            view = inflater.inflate(R.layout.list_item_message, null);
            holder = new Holder();
            holder.view = view;
            view.setTag(holder);
        }else{
            holder = (Holder)view.getTag();
        }

        sm.fillMessage(holder.view);
        return holder.view;
    }

    class Holder{
        View view;
    }

    public static final class ShortMessage{
        public static final int MSG_TYPE_SEND = 0;
        public static final int MSG_TYPE_RECEIVE = 1;

        int type = MSG_TYPE_SEND;
        String msg = "";
        String sender = "";

        public ShortMessage(String sender, String message, int type){
            this.type = type;
            msg = message;
            this.sender = sender;
        }

        void fillMessage(View view){
            if(view != null){
                TextView tv = (TextView)view.findViewById(R.id.tv_msg);
                tv.setText(type == MSG_TYPE_RECEIVE ? msg + ":" + sender : sender + ":" + msg);
                tv.setGravity(type == MSG_TYPE_SEND ? Gravity.LEFT : Gravity.RIGHT);
            }
        }
    }
}
