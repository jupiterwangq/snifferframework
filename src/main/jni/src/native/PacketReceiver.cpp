#include "../../include/native/packetreceiver.h"
#include "../../include/ipc/connection.h"
#include "../../include/threadproxy.h"
#include "../../include/log.h"
#include "../../include/native/jnicontext.h"
#include "../../include/autolock.h"

extern JavaVM *g_jvm;

PacketReceiver::PacketReceiver() : m_stop_flag(false), m_producer(NULL), m_consumer(NULL){
	m_conn = new IPCConnection<ShmConsumer, Packet, Path>();
}

PacketReceiver::~PacketReceiver(){
	stop_receive();
	if( m_producer != NULL ){
		delete m_producer;
		m_producer = NULL;
	}
	if (m_consumer != NULL) {
		delete m_consumer;
		m_consumer = NULL;
	}
}

PacketReceiver *PacketReceiver::get(){
	static PacketReceiver s_instance;
	return &s_instance;
}

/**
 * Note:Do Not do time cost operation in this thread
 */
void *PacketReceiver::producer_thread( void *args ){
	Packet apkt;
	while( true ) {
		int nread = m_conn->read(&apkt);
		LOGE("read pkt,env=%d", apkt.m_event);
		m_buffer.push(apkt);
	}
}

void *PacketReceiver::consumer_thread(void *args) {
	JNIEnv *env = NULL;
	g_jvm->AttachCurrentThread(&env, NULL);

	while( !m_stop_flag) {
		Packet apkt = m_buffer.pop();
		JNIContext::get()->notify_new_packet(env, apkt);
	}

	g_jvm->DetachCurrentThread();

	delete m_conn;
	m_conn = NULL;
}

void PacketReceiver::stop_receive() {
	m_stop_flag = true;
}

void PacketReceiver::start_receive() {
	m_conn->unlink();
	if( !m_conn->create() ){
		LOGE("-->Client:create ipc channel err.");
		delete m_conn;
		return;
	}

	m_producer = new ThreadProxy<PacketReceiver>::Runnable(this, &PacketReceiver::producer_thread, NULL);
	ThreadProxy<PacketReceiver>::create(&m_tidp, NULL, m_producer);
	m_consumer = new ThreadProxy<PacketReceiver>::Runnable(this, &PacketReceiver::consumer_thread, NULL);
	ThreadProxy<PacketReceiver>::create(&m_tidc, NULL, m_consumer);
}
