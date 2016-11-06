#ifndef __EVENT_H__
#define __EVENT_H__

/**
 * PID event,tell the native sniffer pid to android,
 * pid is stored in Packet#m_seq
 */
#define ENV_PID_EVENT -1

/**
 * New tcp connection
 */
#define ENV_NEW_TCP_CONNECTION 0

/**
 * Tcp connection close
 */
#define ENV_TCP_CONNECTION_CLOSE 1

/**
 * New tcp data from server
 */
#define ENV_NEW_DATA_FROM_SERVER 2

/**
 * New tcp data to server
 */
#define ENV_NEW_DATA_TO_SERVER 3

/**
 * Tcp connection is closed by RST
 */
#define ENV_TCP_CONNECTION_CLOSED_BY_RST 4

/**
 * Udp data
 */
#define ENV_NEW_UDP_DATA 5

#define ENV_PID 6

#define ENV_ERROR_FILTER_EXPR 7

// ========================= nids的网络告警 ========================= //

/**
 * IP头部异常
 */
#define ENV_WARN_IP 8

/**
 * TCP头部异常
 */
#define ENV_WARN_TCP 9

/**
 * 端口扫描告警
 */
#define ENV_WARN_SCAN 10

#endif
