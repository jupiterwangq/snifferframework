package com.jupiter.snifferframework.data;

import android.text.TextUtils;

import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.Sniffer;
import com.jupiter.snifferframework.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * Created by wangqiang on 16/6/11.
 * 代表所有暂时还没有专门处理的协议的数据(比如SMTP,FTP等等),这类数据直接原样通知给上层
 */
public class UnknownData extends AbsData {

    public void fillData(Packet data) {
        seq     = data.seq;
        ack_seq = data.ackSeq;
        acked   = data.acked;
        window  = data.window;
        mData   = data.data;
        srcIP   = data.srcIP;
        destIP  = data.destIP;
        srcPort = data.srcPort;
        destPort= data.destPort;
        setState(STATE_DATA_COMPLETE);
    }

    @Override
    public int getPayLoadType() {
        return TYPE_UNKNOWN;
    }

    @Override
    public int getProto() {
        return destPort;
    }

    @Override
    public HashMap<String, String> getHeaders() {
        if (type == Packet.TYPE_TCP) {

            HashMap<String, String> map = new LinkedHashMap<>();
            map.put("window", String.valueOf(window));
            try {
                map.put("seq", Util.unsignedLong(seq).toString());
                map.put("ack-seq", Util.unsignedLong(ack_seq).toString());
                map.put("acked", Util.unsignedLong(acked).toString());
            } catch (IOException e) {

            }
            return map;
        }
        return null;
    }

    @Override
    public byte[] getPayLoad() {
        return mData;
    }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        String proto = Sniffer.getProtoName(destPort, type);
        sb.append("[协议:");
        String protocol = "";
        if (type == Packet.TYPE_TCP) {
            protocol = "TCP";
        } else if (type == Packet.TYPE_UDP) {
            protocol = "UDP";
        }
        if (!TextUtils.isEmpty(proto)) {
            sb.append(proto).append("  ").append(protocol).append("]\n");
            sb.append(getTuple4());
        } else {
            sb.append(getProto()).append(" ").append(protocol).append("]\n");
            sb.append(getTuple4());
        }
        return sb.toString();
    }

    @Override
    public boolean getIsRequest() {
        return mIsRequest;
    }

    public boolean mIsRequest = true;

    private byte[] mData;
}
