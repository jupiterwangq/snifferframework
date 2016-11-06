package com.jupiter.snifferframework.eventhandler;

import android.util.SparseArray;

import com.jupiter.snifferframework.Event;
import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.data.EventData;

/**
 * Created by wangqiang on 16/8/31.
 * 处理nids的告警事件
 */

public class NidsWarningEventHandler extends BaseEventHandler {

    //nids的告警码,必须与nids库中的定义一致
    public static final int NIDS_WARN_UNDEFINED = 0;
    public static final int NIDS_WARN_IP_OVERSIZED = 1;
    public static final int NIDS_WARN_IP_INVLIST = 2;
    public static final int NIDS_WARN_IP_OVERLAP = 3;
    public static final int NIDS_WARN_IP_HDR = 4;
    public static final int NIDS_WARN_IP_SRR = 5;
    public static final int NIDS_WARN_TCP_TOOMUCH = 6;
    public static final int NIDS_WARN_TCP_HDR = 7;
    public static final int NIDS_WARN_TCP_BIGQUEUE = 8;
    public static final int NIDS_WARN_TCP_BADFLAGS = 9;

    private static SparseArray<String> mErrors = new SparseArray<>();

    static {
        mErrors.put(NIDS_WARN_IP_HDR, "非法IP头部");
        mErrors.put(NIDS_WARN_IP_INVLIST, "无效IP碎片列");
        mErrors.put(NIDS_WARN_IP_OVERLAP, "IP数据重叠");
        mErrors.put(NIDS_WARN_IP_OVERSIZED, "数据包超长");
        mErrors.put(NIDS_WARN_IP_SRR, "源路由IP数据包");
        mErrors.put(NIDS_WARN_TCP_BADFLAGS, "错误的TCP标记");
        mErrors.put(NIDS_WARN_TCP_TOOMUCH, "TCP数据过多");
        mErrors.put(NIDS_WARN_TCP_HDR, "非法TCP头部");
        mErrors.put(NIDS_WARN_TCP_BIGQUEUE, "TCP接收队列数据过多");
        mErrors.put(NIDS_WARN_UNDEFINED, "");
    }

    @Override
    public boolean handleEvent(int event, Packet pkt) {
        if (event == Event.ENV_WARN_TCP ||
            event == Event.ENV_WARN_IP ||
            event == Event.ENV_WARN_SCAN) {
            switch (event) {
                case Event.ENV_WARN_TCP:
                case Event.ENV_WARN_IP:
                    mEvent = new EventData(event);
                    int warnNo = pkt.extra;
                    StringBuilder sb = new StringBuilder(mErrors.get(warnNo));
                    sb.append(" ").append("(").append(pkt.tuple4()).append(")");
                    mEvent.eventData = sb.toString().getBytes();
                    break;
                case Event.ENV_WARN_SCAN:
                    mEvent = new EventData(event);
                    mEvent.eventData = pkt.data;
                    break;
                default:
            }
            return true;
        }
        return false;
    }
}
