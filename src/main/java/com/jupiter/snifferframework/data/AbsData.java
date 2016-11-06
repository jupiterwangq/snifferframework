package com.jupiter.snifferframework.data;

import android.text.TextUtils;
import android.util.Log;

import com.jupiter.snifferframework.Sniffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangqiang on 16/6/10.
 * 用来抽象协议数据,比如http请求/响应,DNS数据,SMTP数据等等
 */
public abstract class AbsData {

    public static final int STATE_DATA_NOT_COMPLETE = 0;

    /**
     * 表示特定协议的数据已经组装完成,可以交给UI去展示了
     */
    public static final int STATE_DATA_COMPLETE     = 1;

    /**
     * 负载数据的类型,目前暂时先处理这么多
     */
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_JSON    = 1;
    public static final int TYPE_TEXT    = 2;
    public static final int TYPE_XML     = 3;
    public static final int TYPE_HTML    = 4;
    public static final int TYPE_GIF     = 5;
    public static final int TYPE_JPEG    = 6;
    public static final int TYPE_PNG     = 7;
    public static final int TYPE_WEBP    = 8;

    /**
     * 负载数据的压缩方式(没有压缩、gzip/zlib等)
     */
    public static final int NO_ENCODING   = 1;
    public static final int ENCODING_GZIP = 2;
    public static final int ENCODING_ZLIB = 3;

    /**
     * 错误码的定义
     */
    public static final int NO_ERROR = 0;

    //TCP头部相关字段的定义
    public static final String WINDOW  = "window";  //滑动窗口大小
    public static final String ACKED   = "acked";   //确认的序号
    public static final String SEQ     = "seq";     //序号
    public static final String ACK_SEQ = "ack-seq"; //确认序列号

    public static final String[] TCP_HEADERS = {
            WINDOW,
            ACKED,
            SEQ,
            ACK_SEQ
    };

    /** 指定的key是否是tcp头部的字段 */
    public static final boolean isTcpHeader(String k) {
        for (String h : TCP_HEADERS) {
            if (h.equals(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取数据的状态
     * @return
     */
    public int getState() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
    }

    public void setErrorCode(int code) {
        mErrorCode = code;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * 获取数据的协议号(比如80为http,53为dns)
     * @return
     */
    public abstract int getProto();

    /**
     * 获取头部数据,不同协议头部数据不一样,由子类自行实现
     * @return
     */
    public abstract HashMap<String,String> getHeaders();

    /**
     * 获取负载数据
     */
    public abstract byte[] getPayLoad();

    /**
     * 获取概要信息
     * @param
     */
    public abstract String getSummary();

    /**
     * 是否是请求
     * @return
     */
    public boolean getIsRequest() {
        return isRequest;
    }

    /**
     * 获取四元组[源IP,源端口,目的IP,目的端口]
     * @return
     */
    public String getTuple4() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(srcIP).append(",").append(srcPort)
          .append(",").append(destIP).append(",").append(destPort)
          .append(")");
        return sb.toString();
    }

    /**
     * 获取指定的头部字段值
     * @param key
     * @return
     */
    public String getHeaderValue(String key) {
        return "";
    }

    /**
     * 头部的数量
     * @return
     */
    public int getHeadersCount() {
        return 0;
    }

    /**
     * 获取负载数据的压缩类型
     */
    public int getPayloadEncoding() {
        HashMap<String,String> headers = getHeaders();
        if (headers == null) return NO_ENCODING;
        String encodingStr = headers.get("Content-Encoding");
        if (TextUtils.isEmpty(encodingStr)) return NO_ENCODING;
        if (encodingStr.contains("gzip")) {
            return ENCODING_GZIP;
        } else if (encodingStr.contains("deflate")) {
            return ENCODING_ZLIB;
        }
        return NO_ENCODING;
    }

    /**
     * 获取负载数据的类型
     * @return
     */
    public int getPayLoadType() {
        Map<String,String> headers = getHeaders();
        if (headers == null) return TYPE_UNKNOWN;
        String contentType = headers.get("Content-Type");
        if (TextUtils.isEmpty(contentType)) return TYPE_UNKNOWN;

        if (contentType.contains("text/html")) {
            return TYPE_HTML;
        } else if (contentType.contains("text/plain")) {
            return TYPE_TEXT;
        } else if (contentType.contains("text/xml") ||
                contentType.contains("application/xml")) {
            return TYPE_XML;
        } else if (contentType.contains("application/json") || contentType.contains("x-json")) {
            return TYPE_JSON;
        } else if (contentType.contains("image/gif")) {
            return TYPE_GIF;
        } else if (contentType.contains("image/png")) {
            return TYPE_PNG;
        } else if (contentType.contains("image/jpeg")) {
            return TYPE_JPEG;
        } else if (contentType.contains("image/webp")) {
            return TYPE_WEBP;
        } else if (contentType.contains("charset")) {
            //TODO 这里后续需要处理字符集
            return TYPE_TEXT;
        }
        return TYPE_UNKNOWN;
    }

    /**
     * 获取特定类型负载数据的文本描述
     * @param type 负载数据的类型
     * @return
     */
    public static String getPayloadType(int type) {
        switch( type ) {
            case TYPE_GIF:
                return "GIF动画图";
            case TYPE_HTML:
                return "HTML文本";
            case TYPE_JPEG:
                return "JPEG图片";
            case TYPE_PNG:
                return "PNG图片";
            case TYPE_JSON:
                return "JSON";
            case TYPE_TEXT:
                return "普通文本";
            case TYPE_XML:
                return "XML文本";
            case TYPE_WEBP:
                return "WEBP图片";
            case TYPE_UNKNOWN:
                return "未知";
            default:
                return "未知2";
        }
    }

    /**
     * 获取特定压缩方式的文本描述
     * @param type 压缩类型
     * @return
     */
    public static String getCompressType(int type) {
        switch(type) {
            case NO_ENCODING:
                return "未压缩";
            case ENCODING_GZIP:
                return "GZIP";
            case ENCODING_ZLIB:
                return "ZLIB";
            default:
                return "未知";
        }
    }

    public int getPayloadLength() {
        byte[] data = getPayLoad();
        if (data == null || data.length == 0) return 0;
        return data.length;
    }

    public void setIsRequest(boolean isRequest) {
        this.isRequest = isRequest;
    }

    /**
     * 数据是否完整(只有完整的数据才能展示在UI上)
     * @return
     */
    public boolean isDataComplete() {
        return mState == STATE_DATA_COMPLETE;
    }

    /**
     * 此数据是否已经通知到了上层去展示
     * @return
     */
    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified() {
        isNotified = true;
    }

    protected int mState = STATE_DATA_NOT_COMPLETE;

    /**
     * TCP information,app may use these information to do something.
     */
    public long seq, ack_seq, acked;
    public int window;

    /**
     * 类型:0为TCP,1为UDP
     */
    public int type;

    public String srcIP, destIP;
    public int srcPort, destPort;

    private boolean isRequest = true;

    private int mErrorCode = NO_ERROR;

    private boolean isNotified = false;
}
