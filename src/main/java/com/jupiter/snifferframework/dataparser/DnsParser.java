package com.jupiter.snifferframework.dataparser;

import android.util.Log;

import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.data.dns.DnsData;
import java.util.Arrays;

import com.jupiter.snifferframework.protocol.Protocol;
import com.jupiter.snifferframework.util.ByteArrayHelper;

/**
 * Created by wangqiang on 16/7/16.
 * DNS协议处理器,用于组装DNS数据,具体可以查阅DNS协议
 */

public class DnsParser implements IDataParser {

    public static final String TAG = "DnsStream";

    @Override
    public IDataParser canHandle(Packet packet) {
        if (packet.destPort == Protocol.DNS || packet.srcPort == Protocol.DNS) {
            return this;
        }
        return null;
    }

    @Override
    public AbsData parse(boolean output, Packet data) {
        mData = data.data;
        Log.e(TAG, "parse dns data:" + mData.length );
        DnsData d  = new DnsData();
        d.srcIP    = data.srcIP;
        d.destIP   = data.destIP;
        d.srcPort  = data.srcPort;
        d.destPort = data.destPort;
        parseHeaders(d);
        int answerStart = parseQuestion(d);
        if (d.header.ancount > 0) {
            parseAnswer(d, answerStart);
        }
        d.setState(AbsData.STATE_DATA_COMPLETE);
        return d;
    }

    @Override
    public void onStreamCreate() {
        Log.e(TAG, "dns stream create");
    }

    @Override
    public AbsData onStreamClosing() {
        return null;
    }

    @Override
    public AbsData onOutputData(Packet data) {
        return null;
    }

    @Override
    public AbsData onInputData(Packet data) {
        return null;
    }

    private void parseHeaders(DnsData data) {
        if (mData == null || mData.length == 0) return;

        //1.id
        if (mData.length >= 2) {
            byte[] bid = new byte[2];
            bid[0] = mData[0];
            bid[1] = mData[1];
            data.header.id = ByteArrayHelper.byte2ToUnsignedShort(bid, 0);
        }

        //2.QR(1bit),OPCode(4bit),AA(1bit),TC(1bit),RD(1bit),RA(1bit),Z(3bit),RCODE(4bit)
        if (mData.length >= 4) {
            byte[] bb = new byte[2];
            bb[0] = mData[2];
            bb[1] = mData[3];
            data.header.flag = ByteArrayHelper.byte2ToUnsignedShort(bb, 0);
            Log.e(TAG, "flag=" + data.header.flag);

        }

        //4.QDCOUNT
        if (mData.length >= 6) {
            byte[] qdcount = new byte[2];
            qdcount[0] = mData[4];
            qdcount[1] = mData[5];
            data.header.qdcount = ByteArrayHelper.byte2ToUnsignedShort(qdcount, 0);
            Log.e(TAG, "qdcount=" + data.header.qdcount);
        }

        //5.ANCOUNT
        if (mData.length >= 8) {
            byte[] ncount = new byte[2];
            ncount[0] = mData[6];
            ncount[1] = mData[7];
            data.header.ancount = ByteArrayHelper.byte2ToUnsignedShort(ncount, 0);
            Log.e(TAG, "ancount=" + data.header.ancount);
        }

        //6.NSCOUNT
        if (mData.length >= 10) {
            byte[] nscount = new byte[2];
            nscount[0] = mData[8];
            nscount[1] = mData[9];
            data.header.nscount = ByteArrayHelper.byte2ToUnsignedShort(nscount, 0);
            Log.e(TAG, "NSCOUNT=" + data.header.nscount);
        }

        //7.ARCOUNT
        if (mData.length >= 12) {
            byte[] arcount = new byte[2];
            arcount[0] = mData[10];
            arcount[1] = mData[11];
            data.header.arcount = ByteArrayHelper.byte2ToUnsignedShort(arcount, 0);
            Log.e(TAG, "ARCOUNT=" + data.header.arcount);
        }
    }

    private int parseQuestion( DnsData data ) {
        if (data.header.qdcount == 0) return 12;
        try {
            if (mData.length > 12) {
                int idx = 12;
                for (int i = 0; i < data.header.qdcount; i++) {
                    DnsData.DnsQuestion q = new DnsData.DnsQuestion();
                    StringBuilder nsb = new StringBuilder();
                    boolean nameEnd = false;
                    do {
                        byte len = mData[idx];
                        if (len == 0x00) {
                            nameEnd = true;
                            break;
                        }
                        int s = idx + 1;
                        int e = s + len;
                        if (e > mData.length) break;
                        byte[] name = Arrays.copyOfRange(mData, s, e);
                        nsb.append(new String(name)).append(".");
                        idx = e;
                    } while (idx < mData.length);

                    if (nameEnd) {
                        q.qname = nsb.toString();
                        Log.e(TAG, "q.qname=" + q.qname);
                        idx++;
                        byte[] qtype = new byte[2];
                        qtype[0] = mData[idx++];
                        qtype[1] = mData[idx++];
                        q.qtype = ByteArrayHelper.byte2ToUnsignedShort(qtype, 0);
                        Log.e(TAG, "q.qtype=" + q.qtype);
                        byte[] qclass = new byte[2];
                        qclass[0] = mData[idx++];
                        qclass[1] = mData[idx++];
                        q.qclass = ByteArrayHelper.byte2ToUnsignedShort(qclass, 0);
                        Log.e(TAG, "q.qclass=" + q.qclass);
                        data.questions.add(q);
                    }
                }
                return idx;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception when parse dns questions:" + e.toString());
        }
        return 12;
    }

    private void parseAnswer(DnsData data, int startPos) {
        if (startPos >= mData.length) return;
        int ancount = data.header.ancount;
        int idx = startPos;
        try {
            for (int i = 0; i < ancount; i++) {
                DnsData.DnsAnswer an = new DnsData.DnsAnswer();
                byte[] name = new byte[2];
                name[0] = mData[idx++];
                name[1] = mData[idx++];
                an.name = ByteArrayHelper.byte2ToUnsignedShort(name, 0);
                Log.e(TAG, "an.name=" + an.name);
                byte[] type = new byte[2];
                type[0] = mData[idx++];
                type[1] = mData[idx++];
                an.type = ByteArrayHelper.byte2ToUnsignedShort(type, 0);
                Log.e(TAG, "an.type=" + an.type);
                byte[] clazz = new byte[2];
                clazz[0] = mData[idx++];
                clazz[1] = mData[idx++];
                an.clazz = ByteArrayHelper.byte2ToUnsignedShort(clazz, 0);
                Log.e(TAG, "an.clazz=" + an.clazz);

                idx += 4; //ttl

                byte[] rdlen = new byte[2];
                rdlen[0] = mData[idx++];
                rdlen[1] = mData[idx++];
                an.rdlen = ByteArrayHelper.byte2ToUnsignedShort(rdlen, 0);
                Log.e(TAG, "an.rdlen=" + an.rdlen);

                if (an.rdlen > 0 && idx + an.rdlen <= mData.length) {
                    an.rddata = new byte[an.rdlen];
                    System.arraycopy(mData, idx, an.rddata, 0, an.rdlen);
                }

                data.answer.add(an);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "parse dns answer error:" + e.toString());
        }
    }

    private byte[] mData;
}
