#ifndef __PACKET_RECEIVER_H__
#define __PACKET_RECEIVER_H__

#include <path.h>
#include <singleton.h>
#include "../ipc/connection.h"
#include "../threadproxy.h"
#include "../circlebuffer.h"

/**
 * 数据包接收器,两个线程:
 * 生产者线程:从共享内存中读取数据包并将接收的包加入到环形队列中;
 * 消费者线程:从环形队列中取出包并通知app
 */
class PacketReceiver : public Singleton<PacketReceiver>{
public:
	void start_receive();

	void stop_receive();

	~PacketReceiver();

private:
	enum {
		BUFFER_SIZE = 32
	};

	MAKE_FRIENDS(PacketReceiver);

	PacketReceiver();

	void *producer_thread(void *args);
	void *consumer_thread(void *args);

	IPCConnection<ShmConsumer, Packet, Path> *m_conn;

	volatile bool m_stop_flag;

	pthread_t m_tidp, m_tidc;

	ThreadProxy<PacketReceiver>::Runnable *m_producer, *m_consumer;

	CircleBuffer<Packet, BUFFER_SIZE> m_buffer;
};

#endif
