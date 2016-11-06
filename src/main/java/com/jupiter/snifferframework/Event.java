package com.jupiter.snifferframework;

/**
 * Created by wangqiang on 16/6/9.
 * 各种事件的定义,必须要和native侧的定义取相同的值
 */

public final class Event {

    /**
     * 新的TCP连接
     */
    public static final int ENV_NEW_TCP_CONNECTION  = 0;

    /**
     * TCP连接关闭
     */
    public static final int ENV_TCP_CONNECTION_CLOSE = 1;

    /**
     * 从服务器发送过来的数据(响应)
     */
    public static final int ENV_NEW_DATA_FROM_SERVER = 2;

    /**
     * 发往服务器的数据(请求)
     */
    public static final int ENV_NEW_DATA_TO_SERVER = 3;

    /**
     * TCP连接被RST关闭
     */
    public static final int ENV_TCP_CONNECTION_CLOSED_BY_RST = 4;

    /**
     * UDP数据
     */
    public static final int ENV_NEW_UDP_DATA = 5;

    /**
     * 用于通知sniffer守护进程的pid
     */
    public static final int ENV_PID_EVENT = 6;

    /**
     * 用于通知pcap过滤表达式语法错误
     */
    public static final int ENV_PCAP_FILTER_ERROR = 7;

    // ======================== nids的告警事件 ======================== //
    public static final int ENV_WARN_IP  = 8;

    public static final int ENV_WARN_TCP = 9;

    public static final int ENV_WARN_SCAN = 10;
}
