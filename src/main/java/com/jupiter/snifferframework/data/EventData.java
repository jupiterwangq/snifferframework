package com.jupiter.snifferframework.data;

import android.text.TextUtils;

import com.jupiter.snifferframework.Event;
import com.jupiter.snifferframework.Sniffer;
import com.jupiter.snifferframework.data.AbsData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by wangqiang on 16/8/29.
 * 用来代表底层的某个特殊事件,比如pcap过滤表示语法错误等等
 */

public class EventData extends AbsData {

    private static HashMap<Integer, String> EVENT_STR = new HashMap<>();

    static {
        EVENT_STR.put(Event.ENV_WARN_SCAN, "端口扫描攻击");
        EVENT_STR.put(Event.ENV_WARN_IP,   "异常IP数据包");
        EVENT_STR.put(Event.ENV_WARN_TCP,  "异常TCP数据包");
    }


    public EventData (int event) {
        this.event = event;
    }

    @Override
    public int getProto() {
        return Sniffer.SNIFFER_EVENT;
    }

    @Override
    public HashMap<String, String> getHeaders() {
        return null;
    }

    @Override
    public byte[] getPayLoad() {
        return eventData;
    }

    @Override
    public String getSummary() {
        Date d = new Date(mTimeStamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String typeStr = EVENT_STR.get(event);
        typeStr = TextUtils.isEmpty(typeStr) ? "" : typeStr;
        return typeStr + "  " + sdf.format(d);
    }

    public String getDetail() {
        if (eventData != null && eventData.length > 0) {
            return new String(eventData);
        }
        return "";
    }

    public long mTimeStamp;
    public byte[] eventData;
    public int event = -1;
}
