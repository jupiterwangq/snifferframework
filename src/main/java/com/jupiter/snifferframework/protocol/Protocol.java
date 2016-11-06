package com.jupiter.snifferframework.protocol;

/**
 * Created by wangqiang on 16/10/14.
 */

public final class Protocol {
    //常用协议的定义(端口号),目前只处理了HTTP和DNS
    public static final int HTTP  = 80;
    public static final int SMTP  = 25;
    public static final int POP3  = 110;
    public static final int FTP   = 20;
    public static final int TFTP  = 69;     //udp
    public static final int HTTPS = 443;
    public static final int SNMP  = 161;    //udp
    public static final int TELNET = 23;
    public static final int DNS    = 53;    //tcp or udp
    public static final int HTTP_PROXY = 8080;

    public static final int ALL    = -1;    //通配全部协议

    static {
        final int[] PROTOCOLS = {
                HTTP,
                SMTP,
                POP3,
                FTP,
                TFTP,
                HTTPS,
                SNMP,
                TELNET,
                DNS,
                HTTP_PROXY,
                ALL
        };
    }
}
