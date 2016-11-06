package com.jupiter.snifferframework.eventhandler;

import android.os.Handler;
import android.os.Message;

import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.Sniffer;
import com.jupiter.snifferframework.data.EventData;
import java.util.ArrayList;

/**
 * Created by wangqiang on 16/8/31.
 * 处理底层的告警事件以及其他特殊事件
 */

public class EventHandler implements IEventHandler {

    public EventHandler(Handler handler) {
        mHandler = handler;
        initEventHandlers();
    }

    @Override
    public boolean handleEvent(int event, Packet pkt) {
        for (IEventHandler h : mHandlers) {
            if (h.handleEvent(event, pkt)) {
                sendEvent(h.getEvent());
                return true;
            }
        }
        return false;
    }

    @Override
    public EventData getEvent() {
        return null;
    }

    private void sendEvent(EventData event) {
        if (event == null) return;
        Message msg = mHandler.obtainMessage(Sniffer.MSG_EVENT, event);
        mHandler.sendMessage(msg);
    }

    private void initEventHandlers() {
        mHandlers.add(new PidEventHandler());
        mHandlers.add(new FilterErrorEventHandler());
        mHandlers.add(new NidsWarningEventHandler());
    }

    private ArrayList<IEventHandler> mHandlers = new ArrayList<>();
    private Handler mHandler;
}
