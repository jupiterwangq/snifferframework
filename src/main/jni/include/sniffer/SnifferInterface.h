//
// Created by wangqiang on 16/9/18.
//

#ifndef SNIFFER_SNIFFERINTERFACE_H
#define SNIFFER_SNIFFERINTERFACE_H

#include <Packet.h>

struct SnifferInterface {
    void (*on_new_packet)( const Packet& packet );
};

#endif //SNIFFER_SNIFFERINTERFACE_H
