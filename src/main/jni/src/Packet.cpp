//
// Created by wangqiang on 16/6/21.
//
#include <stdlib.h>
#include "../include/Packet.h"

void Packet::do_copy(const Packet &src) {
    m_seq = src.m_seq;
    m_window = src.m_window;
    m_acked  = src.m_acked;
    m_ack_seq = src.m_ack_seq;
    m_src_port = src.m_src_port;
    m_dest_port = src.m_dest_port;
    m_length = src.m_length;
    m_event  = src.m_event;
    m_extra = src.m_extra;
    m_type = src.m_type;
    strcpy(m_src_ip, src.m_src_ip);
    strcpy(m_dest_ip, src.m_dest_ip);
    memcpy(m_data, src.m_data, m_length);
}

Packet::Packet(const Packet &src) {
    do_copy(src);
}

Packet::Packet() {
    memset(m_src_ip,  0, sizeof(m_src_ip));
    memset(m_dest_ip, 0, sizeof(m_dest_ip));
}

Packet& Packet::operator=(const Packet &src) {
    if ( &src == this ) {
        return *this;
    }
    do_copy(src);
    return *this;
}

void Packet::copy_ptr(Packet *src, Packet *dest) {
    if (src == NULL || dest == NULL || src == dest) return;
    *dest = *src;
}

