package com.jupiter.snifferframework.eventhandler;

import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.data.EventData;

/**
 * Created by wangqiang on 16/8/31.
 */

public interface IEventHandler {
    /**
     * 处理底层的特殊事件(通知pid,nids的告警事件等等)
     * @param event
     * @param pkt
     * @return
     */
    boolean handleEvent(int event, Packet pkt);

    /**
     * 获取最终生成的事件
     */
    EventData getEvent();
}
