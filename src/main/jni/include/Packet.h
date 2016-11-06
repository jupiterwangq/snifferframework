//
// Created by wangqiang on 16/6/20.
//

#ifndef SNIFFER_PACKET_H
#define SNIFFER_PACKET_H

/**
 * 数据包的封装,在共享内存上传输的数据实体
 */
struct Packet {
public:
    /**enum hack*/
    enum{
        //负载数据的大小(64K),一般情况下足够了
        BUFFER_UNIT_SIZE = 64 * 1024
    };

    Packet();

    /**
     * 拷贝:这里要把数据通过共享内存传递给android,所以要对数据进行深拷贝
     */
    Packet( const Packet& src);

    Packet &operator=(const Packet &src);

    /**
     * 将源指针指向的数据拷贝到目标指针指向的地址中
     */
    static void copy_ptr(Packet *src, Packet *dest);

    char m_src_ip[16];
    char m_dest_ip[16];
    long m_acked;
    long m_seq;
    long m_ack_seq;
    int  m_window;
    int  m_src_port;
    int  m_dest_port;

    int  m_extra;

    //事件,定义在event.h中
    int m_event;
    //负载数据的长度
    int m_length;
    //数据包类型 0:TCP 1:UDP
    char m_type;
    //负载数据
    char m_data[BUFFER_UNIT_SIZE];

private:
    void do_copy(const Packet &src);
};

#endif //SNIFFER_PACKET_H
