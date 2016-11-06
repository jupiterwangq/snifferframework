package com.jupiter.snifferframework;

/**
 * Created by wangqiang on 16/6/20.
 * 数据包,此对象在native侧生成并填充数据
 */

public class Packet {

    public static final int TYPE_TCP = 0;
    public static final int TYPE_UDP = 1;
    public static final int TYPE_EVENT = 1000;  //Some special event from native

    public String srcIP = "";
    public String destIP = "";
    public int srcPort;
    public int destPort;
    public int type;

    /**
     * The following only used for tcp
     * TODO add data you are intrested in.
     */
    public long seq;
    public long acked;
    public long ackSeq;
    public int  window;

    /** any extra data */
    public int  extra;

    /**
     * Data in this packet
     */
    public byte[] data;

    /**
     * get tuple4 string
     * @return
     */
    public String tuple4() {
        StringBuilder sb = new StringBuilder();
        sb.append(srcIP).append(":").append(srcPort).append(",")
          .append(destIP).append(":").append(destPort);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("tuple4:").append(tuple4())
          .append("\nseq:").append(seq)
          .append("\nacked:").append(acked)
          .append("\nack_seq:").append(ackSeq)
          .append("\nwindow:").append(window)
          .append("\nextra:").append(extra)
          .append("\ndata:").append(new String(data));
        return sb.toString();
    }

    public void recycle() {
        if (data != null) {
            data = null;
        }
    }
}
