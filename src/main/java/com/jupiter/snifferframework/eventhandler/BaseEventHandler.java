package com.jupiter.snifferframework.eventhandler;

import com.jupiter.snifferframework.data.EventData;

/**
 * Created by wangqiang on 16/8/31.
 */

public abstract class BaseEventHandler implements IEventHandler {

    @Override
    public EventData getEvent() {
        return mEvent;
    }

    protected EventData mEvent;
}
