package com.jupiter.snifferframework.eventhandler;

import com.jupiter.snifferframework.Event;
import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.Sniffer;
import com.jupiter.snifferframework.data.EventData;

/**
 * Created by wangqiang on 16/8/31.
 * 处理底层通知守护进程PID的事件
 */

public class PidEventHandler extends BaseEventHandler {
    @Override
    public boolean handleEvent(int event, Packet pkt) {
        if (event == Event.ENV_PID_EVENT) {
            mEvent = new EventData(Event.ENV_PID_EVENT);
            String pid = String.valueOf(pkt.extra);
            Sniffer.get().mNativeSnifferPid = Integer.parseInt(pid);
            mEvent.eventData = pid.getBytes();
            return true;
        }
        return false;
    }
}
