#ifndef __JNICONTEXT_H__
#define __JNICONTEXT_H__

#include <jni.h>
#include <string.h>
#include "../../include/packet.h"
#include "../packetbuffer.h"

/**
 * JNI上下文,提供一些通用接口
 */
class JNIContext{
public:
    static JNIContext* get( );

    static char* c_str( jstring& );

    void init( jobject jsnifferObj, jclass jsnifferClazz );

    jobject jsniffer() const;

    jclass jsniffer_class() const;

    void notify_new_packet(JNIEnv *, Packet packet);

    ~JNIContext();

private:
    jobject m_jsniffer;
    jclass  m_jsnifferClazz;
    jclass  m_jPacketClazz;

    jfieldID m_fSrcIP;
    jfieldID m_fDestIP;
    jfieldID m_fSrcPort;
    jfieldID m_fDestPort;
    jfieldID m_fAcked;
    jfieldID m_fSeq;
    jfieldID m_fAckSeq;
    jfieldID m_fWindow;
    jfieldID m_fData;
    jfieldID m_fExtra;
    jfieldID m_fType;

    jmethodID m_onNewPacket;
    jmethodID m_PacketInit;

    JNIContext();
    JNIContext( const JNIContext& );
    JNIContext& operator=( const JNIContext& );
};

#endif
