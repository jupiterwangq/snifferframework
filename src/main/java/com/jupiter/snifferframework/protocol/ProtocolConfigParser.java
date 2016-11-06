package com.jupiter.snifferframework.protocol;

import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by wangqiang on 16/10/13.
 * 从配置文件中解析出协议信息
 */

public class ProtocolConfigParser {

    public static final String TAG = "ProtocolParser";

    public static ProtocolConfigParser getInstance() {
        return sInstance;
    }

    public void parse(InputStream config) {
        if (config == null || isParsed) return;
        isParsed = true;
        InputStreamReader ir = new InputStreamReader(config);
        BufferedReader br = new BufferedReader(ir);
        try {
            String line = br.readLine();
            while (line != null) {
                parseItem(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            Log.e(TAG, "io error:" + e.toString());
        } finally {
            try {
                config.close();
            } catch (IOException e) {

            }
            for (ProtocolInfo info : mProtocols.values()) {
                Log.e(TAG, "proto:" + info.toString());
            }
        }
    }

    /**
     * 获取TCP端口号对应的协议名
     * @param protoNo 端口号
     * @return
     */
    public String getTcpProtoName(int protoNo) {
        return getProtoName(protoNo, ProtocolInfo.TCP);
    }

    /**
     * 获取UDP端口号对应的协议名
     * @param protoNo 端口号
     * @return
     */
    public String getUdpProtoName(int protoNo) {
        return getProtoName(protoNo, ProtocolInfo.UDP);
    }

    /**
     * 获取端口号对应的协议名
     * @param protoNo 端口号
     * @param transport 传输层协议
     * @return
     */
    public String getProtoName(int protoNo, int transport) {
        ProtocolInfo p = mProtocols.get(protoNo);
        if (p == null) {
            return "UNKNOWN";
        }
        if (transport != ProtocolInfo.UDP && transport != ProtocolInfo.TCP) return "UNKNOWN";
        return p.protoName[transport];
    }

    private void parseItem(String item) {
        if (TextUtils.isEmpty(item)) return;
        int idx = item.indexOf(' ');
        if (idx >= 0) {
            String proto = item.substring(0, idx);
            int idx2 = item.indexOf(' ', idx + 1);
            if (idx2 >= 0) {
                String name = item.substring(idx + 1, idx2);
                String desc = item.substring(idx2 + 1);
                if (proto.contains("/")) {
                    int idx3 = proto.indexOf('/');
                    int protoNo = Integer.parseInt(proto.substring(0, idx3));
                    String transport = proto.substring(idx3 + 1);
                    ProtocolInfo info = mProtocols.get(protoNo);
                    if (info == null) {
                        info = new ProtocolInfo();
                        info.protoNo = protoNo;
                    }
                    if ("tcp".equalsIgnoreCase(transport)) {
                        info.protoName[ProtocolInfo.TCP] = name;
                        info.protoDesc[ProtocolInfo.TCP] = desc;
                    } else if ("udp".equalsIgnoreCase(transport)) {
                        info.protoName[ProtocolInfo.UDP] = name;
                        info.protoDesc[ProtocolInfo.UDP] = desc;
                    }
                    mProtocols.put(protoNo, info);
                } else {
                    int protoNo = Integer.parseInt(proto);
                    ProtocolInfo info = mProtocols.get(protoNo);
                    if (info == null) {
                        info = new ProtocolInfo();
                        info.protoNo = protoNo;
                    }
                    info.protoName[ProtocolInfo.TCP] = name;
                    info.protoDesc[ProtocolInfo.TCP] = desc;
                    info.protoName[ProtocolInfo.UDP] = name;
                    info.protoDesc[ProtocolInfo.UDP] = desc;
                    mProtocols.put(protoNo, info);
                }
            }
        }
    }

    private ProtocolConfigParser() {

    }

    private boolean isParsed = false;

    private static ProtocolConfigParser sInstance = new ProtocolConfigParser();

    private HashMap<Integer, ProtocolInfo> mProtocols = new HashMap<>();
}
