package com.jupiter.snifferframework.protocol;

import com.jupiter.snifferframework.Packet;

/**
 * Created by wangqiang on 16/10/13.
 * 协议信息
 */

public final class ProtocolInfo {
    public static final int TCP = Packet.TYPE_TCP;
    public static final int UDP = Packet.TYPE_UDP;

    public int    protoNo;
    public String[] protoName = new String[]{"", ""};
    public String[] protoDesc = new String[]{"", ""};

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[proto:").append(protoNo).append(",tcp name:").append(protoName[TCP])
                .append(",udp name:").append(protoName[UDP]).append(",tcp desc:")
                .append(protoDesc[TCP]).append(",udp desc:").append(protoDesc[UDP]).append("]\n");
        return sb.toString();
    }

}
