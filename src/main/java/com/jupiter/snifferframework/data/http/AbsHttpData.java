package com.jupiter.snifferframework.data.http;

import android.text.TextUtils;
import android.util.Log;
import com.jupiter.snifferframework.Sniffer;
import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.protocol.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by wangqiang on 16/6/10.
 * HTTP数据的抽象
 */
public class AbsHttpData extends AbsData {

    private static final String Tag = "AbsHttpData";

    public static final String TE = "Transfer-Encoding";

    //添加一个头部
    public void addHeader(String head) {
        Log.e(Tag, "add header " + head);
        String[] kv = head.split(":");
        if (kv.length < 2) {
            return;
        }
        mHeaders.put(kv[0], kv[1]);
    }

    //添加一个头部
    public void addHeader(String k, String v) {
        mHeaders.put(k, v);
    }

    public void reset() {
        mData.reset();
    }

    //头部中是否含有TE(Transfer-Encoding:chunked)
    public boolean isTrunk() {
        return mHeaders.containsKey(TE) && "chunked".equalsIgnoreCase(getHeaderValue(TE));
    }

    @Override
    public int getProto() {
        return Protocol.HTTP;
    }

    /**
     * 从Header中获取Content-Length字段来决定负载数据的长度
     * @return
     */
    public int contentLength() {
        for( String key : mHeaders.keySet()) {
            if (key.equalsIgnoreCase("Content-Length")) {
                return Integer.valueOf(mHeaders.get(key).trim());
            }
        }
        return 0;
    }

    @Override
    public byte[] getPayLoad() {
        if ( mData == null || !isDataComplete()) {
            return null;
        }
        byte[] data = mData.toByteArray();
        return data;
    }

    @Override
    public String getSummary() {
        return null;
    }

    @Override
    public HashMap<String,String> getHeaders() {
        return mHeaders;
    }

    @Override
    public int getHeadersCount() {
        if (mHeaders != null) return mHeaders.size();
        return 0;
    }

    @Override
    public String getHeaderValue(String key) {
        if (mHeaders != null && mHeaders.size() > 0) {
            String v = mHeaders.get(key);
            if (!TextUtils.isEmpty(v)) return v.trim();
        }
        return "";
    }

    //填充http的起始行和头部
    public void fillStartLineAndHeaders(String data) {
        Log.e(Tag, "fill start line and headers,data:" + data);
        if (TextUtils.isEmpty(data)) return;
        data += "\r\n";
        int crlf1 = data.indexOf("\r\n");
        if (crlf1 > 0) {
            //start line
            mStartLine = new String(data.substring(0, crlf1));
            //headers
            int i = crlf1 + "\r\n".length();
            while (i >= 0 && i < data.length()) {
                int crlf = data.indexOf("\r\n", i);
                addHeader(data.substring(i, crlf));
                i = crlf + "\r\n".length();
            }
        }
        mHasHeaders = true;
    }

    public String getStartLine() {
        return mStartLine;
    }

    //追加数据
    public void appendData(byte[] partData) {
        Log.e(Tag, "append " + (partData == null ? "0" : partData.length) + " bytes");
        if (partData == null || partData.length == 0) return;
        try {
            mData.write(partData);
        } catch (IOException e) {
            Log.e(Tag, "Write data to stream err:" + e.toString());
        }
    }

    //获取当前已经组装好的数据
    public byte[] getCurrentData() {
        return mData.toByteArray();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mStartLine).append("\n");
        for (String k : mHeaders.keySet()) {
            sb.append(k).append(":").append(mHeaders.get(k)).append("\n");
        }
        return sb.toString();
    }

    public void close() {
        if (mData != null) {
            try {
                mData.close();
            } catch (Exception e) {

            }
        }
    }

    public boolean hasHeaders() {
        return mHasHeaders;
    }


    protected ByteArrayOutputStream mData = new ByteArrayOutputStream();

    /**
     * Http request/response start line
     */
    protected String mStartLine = "";

    /**
     * Http request/response header
     */
    protected HashMap<String, String> mHeaders = new LinkedHashMap<>();

    protected boolean mHasHeaders = false;
}
