#ifndef __PACKET_BUFFER_H__
#define __PACKET_BUFFER_H__

#include <jni.h>
#include <constants.h>
#include <log.h>
#include "constants.h"

extern JNIEnv *g_env;

/**
 * 数据包缓冲区,默认1M
 * T:缓冲区的数据类型
 * Allocator:分配器,用来分配缓存,不同的类型,分配的方式有可能不相同
 * BUFF_SIZE:要分配的缓冲区大小
 */
template<typename T, typename Allocator, unsigned BUFF_SIZE = MAX_BUFF_SIZE>
class PacketBuffer{
public:

	PacketBuffer();
	~PacketBuffer();

	/**
	 * 转换函数,将buffer转换成对应的指针类型
	 */
	operator T*();

private:
	PacketBuffer(const PacketBuffer&);
	PacketBuffer& operator=(const PacketBuffer&);

	T *m_buffer;
};

template<typename T, typename Allocator, unsigned BUFF_SIZE>
PacketBuffer<T, Allocator, BUFF_SIZE>::operator T*(){
	return m_buffer;
}

/**
 * RAII原则:构造的时候分配资源,析构的时候释放资源
 */
template<typename T, typename Allocator, unsigned BUFF_SIZE>
PacketBuffer<T, Allocator, BUFF_SIZE>::PacketBuffer(){
	Allocator::construct(m_buffer, BUFF_SIZE);
}

/**
 * RAII原则:构造的时候分配资源,析构的时候释放资源
 */
template<typename T, typename Allocator, unsigned BUFF_SIZE>
PacketBuffer<T, Allocator, BUFF_SIZE>::~PacketBuffer(){
	Allocator::destruct(m_buffer);
}

/**
 * 分配器,用于分配/释放缓存
 */
template<typename T>
class Allocator{
public:

	static void construct(T*&, unsigned int);
	static void destruct(T*&);
	static void set(T*&, const char*, unsigned int);
};

template<typename T>
void Allocator<T>::construct(T*& buff, unsigned int size){
	buff = new T[size];
}

template<typename T>
void Allocator<T>::destruct(T*& buff){
	delete[] buff;
}

/**
 * 针对jbyteArray类型的特化版本
 */
template<>
class Allocator<jbyteArray>{
public:
	static void construct(jbyteArray*& buffer, unsigned int size){
		static jbyteArray buf = (jbyteArray)g_env->NewGlobalRef(g_env->NewByteArray(size));
		buffer = &buf;
	}

	static void destruct(jbyteArray*& buffer){
		g_env->DeleteGlobalRef(*buffer);
	}
};
#endif
