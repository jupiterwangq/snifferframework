#ifndef __SNIFFER_H__
#define __SNIFFER_H__

#ifdef __cplusplus
extern "C"{
#endif
#ifdef __cplusplus
}
#endif
#include <string.h>
#include <pthread.h>
#include <threadproxy.h>
#include <singleton.h>
#include "SnifferInterface.h"

/**
 * @author jupiter
 */
class Sniffer : public Singleton<Sniffer> {
public:

    ~Sniffer(){ }

    void set_interface( const SnifferInterface &snifferInterface );

    /**
     * 启动sniffer
     * argc 启动参数的个数
     * argv 启动参数
     * async 是否异步启动
     */
    int start_sniff( int argc, char **argv, bool async = false );

    void stop_sniff();

    /**
     * 关闭校验和
     */
    void close_checksum();

    /**
     * 通知packet
     */
    void notify_packet(const Packet& pkt);

    /**
     * 异步运行pcap的线程
     */
    void *pcap_thread( void *args );

private:

    MAKE_FRIENDS(Sniffer);

    /**
     * 解析启动参数
     */
    void parse_param(int argc, char **argv);

    int prepare_sniffer();

    Sniffer();

    void clear();

    void* snifferThread(void *args);

    ThreadProxy<Sniffer>::Runnable *m_runnable;

    pthread_t m_tid;

    SnifferInterface m_interface;
};

#endif
