package com.jupiter.snifferframework.data.dns;

import com.jupiter.snifferframework.Sniffer;
import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.protocol.Protocol;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wangqiang on 16/7/16.
 * 表示一个DNS数据,具体可以参考DNS协议
 */

public class DnsData extends AbsData {

    public static final String ID = "ID";
    public static final String FLAG = "FLAG";

    public static final String QR = "QR";
    public static final String OPCODE = "OPCODE";
    public static final String AA = "AA";
    public static final String TC = "TC";
    public static final String RD = "RD";
    public static final String RA = "RA";
    public static final String Z  = "Z";
    public static final String RCODE = "RCODE";

    public static final String QDCOUNT = "QDCOUNT";
    public static final String NCOUNT  = "ANCOUNT";
    public static final String NSCOUNT = "NSCOUNT";
    public static final String ARCOUNT = "ARCOUNT";

    /**
     * DNS头部
     */
    public static class DnsHeader {
        public int id;
        public int flag;
        public int qdcount;  //count of questions
        public int ancount;  //count of answers
        public int nscount;
        public int arcount;
    }

    /**
     * DNS查询
     */
    public static class DnsQuestion {
        public String qname;
        public int  qtype;
        public int  qclass;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(qname).append(",Type:")
              .append(typeStr(qtype))
              .append(",Class:")
              .append(classStr(qclass))
              .append("]");
            return sb.toString();
        }
    }

    /**
     * DNS应答
     */
    public static class DnsAnswer {
        public int name;
        public int type;
        public int clazz;
        public int   ttl;
        public int rdlen;
        public byte[] rddata;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append("Type:")
              .append(typeStr(type))
              .append(",Class:").append(classStr(clazz))
              .append(",length:").append(rdlen);
            if (type == 0x01 && rddata != null) {
                sb.append(",data:");
                for (byte b : rddata) {
                    int i = b & 0xff;
                    sb.append(i).append(".");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    @Override
    public HashMap<String,String> getHeaders() {
        mHeaders.put(ID,      Integer.toHexString(header.id));
        mHeaders.put(FLAG,    Integer.toHexString(header.flag));
        mHeaders.put(QDCOUNT, Integer.toHexString(header.qdcount));
        mHeaders.put(NCOUNT,  Integer.toHexString(header.ancount));
        mHeaders.put(NSCOUNT, Integer.toHexString(header.nscount));
        mHeaders.put(ARCOUNT, Integer.toHexString(header.arcount));
        return mHeaders;
    }

    @Override
    public byte[] getPayLoad() {
        if (getIsRequest()) {
            return genQuestionStr().getBytes();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(genQuestionStr()).append("\n");
            sb.append(gentAnwserStr());
            return sb.toString().getBytes();
        }
    }

    @Override
    public int getPayLoadType() {
        return AbsData.TYPE_TEXT;
    }

    @Override
    public String getSummary() {
        if (getIsRequest()) {
            return genQuestionStr();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Questions Count : ").append(questions.size()).append("\n");
            sb.append("Answer Count : ").append(answer.size());
            return sb.toString();
        }
    }

    @Override
    public boolean getIsRequest() {
        if (destPort == Protocol.DNS) {
            return true;
        }
        return false;
    }

    @Override
    public int getHeadersCount() {
        return mHeaders.size();
    }

    @Override
    public int getProto() {
        return Protocol.DNS;
    }

    private String genQuestionStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("Questions Count : ").append(questions.size()).append("\n")
                .append("Queries :\n");
        for (DnsQuestion q : questions) {
            sb.append(q.toString()).append("\n");
        }
        return sb.toString();
    }

    private String gentAnwserStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("Anwser Count : ").append(answer.size()).append("\n");
        sb.append("Anwsers :\n");
        for (DnsAnswer an : answer) {
            sb.append(an.toString()).append("\n");
        }
        return sb.toString();
    }

    private static String typeStr(int type) {
        switch (type) {
            case 0x01:
                return "A";
            case 0x02:
                return "NS";
            case 0x03:
                return "MD";
            case 0x04:
                return "MF";
            case 0x05:
                return "CNAME";
            case 0x06:
                return "SOA";
            case 0x07:
                return "MB";
            case 0x08:
                return "MG";
            case 0x09:
                return "MR";
            case 0x0a:
                return "NULL";
            case 0x0b:
                return "WKS";
            case 0x0c:
                return "PTR";
            case 0x0d:
                return "HINFO";
            case 0x0e:
                return "MINFO";
            case 0x0f:
                return "MX";
            case 0x64:
                return "UINFO";
            case 0x65:
                return "UID";
            case 0x66:
                return "GID";
            case 0xff:
                return "ANY";
            default:
                return "UNKNOWN";
        }
    }

    private static String classStr(int clazz) {
        switch (clazz) {
            case 0x01:
                return "IN";
            case 0x02:
                return "CSNET";
            case 0x03:
                return "CHAOS";
            case 0x04:
                return "HESIOD";
            case 0xff:
                return "ANY";
            default:
                return "UNKNOWN";
        }
    }

    public HashMap<String,String> mHeaders = new HashMap<>();

    //DNS header
    public DnsHeader header = new DnsHeader();

    //queries
    public ArrayList<DnsQuestion> questions = new ArrayList<>();

    //answers
    public ArrayList<DnsAnswer> answer = new ArrayList<>();
}
