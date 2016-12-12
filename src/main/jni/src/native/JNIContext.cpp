#include "../../include/native/jnicontext.h"
#include "../../include/log.h"

extern JNIEnv *g_env;

void JNIContext::init( jobject jobj, jclass clazz ){
    m_jsniffer = g_env->NewGlobalRef(jobj);
    m_jsnifferClazz = (jclass)g_env->NewGlobalRef(clazz);
    m_jPacketClazz  = (jclass)g_env->NewGlobalRef(g_env->FindClass("com/jupiter/snifferframework/Packet"));
    if (m_jPacketClazz != NULL) {
        m_fAcked    = g_env->GetFieldID(m_jPacketClazz, "acked", "J");
        m_fAckSeq   = g_env->GetFieldID(m_jPacketClazz, "ackSeq", "J");
        m_fSeq      = g_env->GetFieldID(m_jPacketClazz, "seq", "J");
        m_fWindow   = g_env->GetFieldID(m_jPacketClazz, "window", "I");
        m_fSrcIP    = g_env->GetFieldID(m_jPacketClazz, "srcIP", "Ljava/lang/String;");
        m_fDestIP   = g_env->GetFieldID(m_jPacketClazz, "destIP", "Ljava/lang/String;");
        m_fSrcPort  = g_env->GetFieldID(m_jPacketClazz, "srcPort", "I");
        m_fDestPort = g_env->GetFieldID(m_jPacketClazz, "destPort", "I");
        m_fExtra    = g_env->GetFieldID(m_jPacketClazz, "extra", "I"),
        m_fData     = g_env->GetFieldID(m_jPacketClazz, "data", "[B");
        m_fType     = g_env->GetFieldID(m_jPacketClazz, "type", "I");
        m_PacketInit = g_env->GetMethodID(m_jPacketClazz, "<init>", "()V");
    }

    m_onNewPacket = g_env->GetMethodID(m_jsnifferClazz,
    		"onNewPacket",
    		"(ILcom/jupiter/snifferframework/Packet;)V");
}

JNIContext::~JNIContext(){
    g_env->DeleteGlobalRef(m_jsniffer);
    g_env->DeleteGlobalRef(m_jsnifferClazz);
    g_env->DeleteGlobalRef(m_jPacketClazz);
}

jobject JNIContext::jsniffer() const{
    return m_jsniffer;
}

jclass JNIContext::jsniffer_class() const{
    return m_jsnifferClazz;
}

JNIContext::JNIContext() {

}

void JNIContext::notify_new_packet(JNIEnv *env, Packet amsg){
	LOGE("-->notifyNewPacket");
    if (m_jPacketClazz != NULL) {
        if (m_PacketInit != NULL) {
            jobject data = env->NewObject(m_jPacketClazz, m_PacketInit);
            jstring srcIP, destIP;
            jbyteArray bytes;
            if (data != NULL) {
                if (m_fSrcIP!= NULL ) {
                    srcIP = env->NewStringUTF(amsg.m_src_ip);
                    if (srcIP != NULL) {
                        env->SetObjectField(data, m_fSrcIP, srcIP);
                    }
                }
                if (m_fDestIP != NULL) {
                    destIP = env->NewStringUTF(amsg.m_dest_ip);
                    if (destIP != NULL) {
                        env->SetObjectField(data, m_fDestIP, destIP);
                    }
                }
                if (m_fData != NULL) {
                    bytes = env->NewByteArray(amsg.m_length);
                    env->SetByteArrayRegion(bytes, 0, amsg.m_length, (jbyte*)amsg.m_data);
                    env->SetObjectField(data, m_fData, bytes);
                }
                if (m_fSrcPort != NULL) {
                    env->SetIntField(data, m_fSrcPort, amsg.m_src_port);
                }
                if (m_fDestPort != NULL) {
                    env->SetIntField(data, m_fDestPort, amsg.m_dest_port);
                }
                if (m_fAcked != NULL) {
                    env->SetLongField(data, m_fAcked, amsg.m_acked);
                }
                if (m_fSeq != NULL) {
                    env->SetLongField(data, m_fSeq, amsg.m_seq);
                }
                if (m_fAckSeq != NULL) {
                    env->SetLongField(data, m_fAckSeq, amsg.m_ack_seq);
                }
                if (m_fWindow != NULL) {
                    env->SetIntField(data, m_fWindow, amsg.m_window);
                }
                if (m_fExtra != NULL) {
                    env->SetIntField(data, m_fExtra, amsg.m_extra);
                }
                if (m_fType != NULL) {
                    env->SetIntField(data, m_fType, (jint)amsg.m_type);
                }

                env->CallVoidMethod(m_jsniffer, m_onNewPacket, amsg.m_event, data);

                if (data != NULL) {
                    env->DeleteLocalRef(data);
                }
                if (srcIP != NULL) {
                    env->DeleteLocalRef(srcIP);
                }
                if (destIP != NULL) {
                    env->DeleteLocalRef(destIP);
                }
                if (bytes != NULL) {
                    env->DeleteLocalRef(bytes);
                }
            }
        }
    }
}

char* JNIContext::c_str( jstring& jstr){
    char *rtn = 0;
    jclass clsstring = g_env->FindClass("java/lang/String");
    jstring strencode = g_env->NewStringUTF("UTF-8");
    jmethodID mid = g_env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr= (jbyteArray)g_env->CallObjectMethod(jstr,mid,strencode);
    jsize alen = g_env->GetArrayLength(barr);
    jbyte * ba = g_env->GetByteArrayElements(barr,JNI_FALSE);
    if(alen > 0) {
        rtn = new char[alen + 1];
        memcpy(rtn,ba,alen);
        rtn[alen]=0;
    }
    g_env->ReleaseByteArrayElements(barr,ba,0);
    return rtn;
}
