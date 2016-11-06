package com.jupiter.snifferframework.eventhandler;

import com.jupiter.snifferframework.Event;
import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.data.EventData;

/**
 * Created by wangqiang on 16/8/31.
 * 处理底层过滤表达式语法错误的事件
 */

public class FilterErrorEventHandler extends BaseEventHandler {

    @Override
    public boolean handleEvent(int event, Packet pkt) {
        if (event == Event.ENV_PCAP_FILTER_ERROR) {
            mEvent = new EventData(Event.ENV_PCAP_FILTER_ERROR);
            return true;
        }
        return false;
    }
}
