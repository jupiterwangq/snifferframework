#ifndef __IPCCONNECTION_H__
#define __IPCCONNECTION_H__

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include "sema.h"
#include "../Packet.h"
#include "../log.h"
#include "../constants.h"

/** 以下为IPC类型的定义 */

//共享内存
struct Shm{};
//共享内存生产者
struct ShmProducer {};
//共享内存消费者
struct ShmConsumer {};

/**
 * 用来代表某种类型的IPC通道
 * Connection:IPC类型
 * Entity:在IPC通道上传输的实体
 */
template <typename Connection, typename Entity, typename Path>
class IPCConnection{
public:
    bool create( void *args = 0 ){
	}

    void close(){
	}

    /**
     * 从IPC中读取实体数据到dest中
     */
    int read( Entity *pentity) {
		return -1;
	}

    /**
     * 将数据写入到IPC
     */
	int write( Entity *pentity) {
		return -1;
	}

    void unlink(){
	}

};

/**
 * 偏特化版本,共享内存的生产者,向共享内存中写入数据
 */
template<typename Entity, typename Path>
class IPCConnection<ShmProducer, Entity, Path> {
public:
    IPCConnection();

    ~IPCConnection();

	bool create( void *args = 0 );

	void close();

	void unlink();

	int write( Entity*);

	int read( Entity*);

private:
	bool create_semaphore();

    //用于读写同步的信号量
	sem_t *m_sema_put, *m_sema_get;

	IPCConnection<Shm, Entity, Path> m_conn;
};

template <typename Entity, typename Path>
IPCConnection<ShmProducer, Entity, Path>::IPCConnection() : m_sema_put(NULL),
        m_sema_get(NULL){
}

template <typename Entity, typename Path>
IPCConnection<ShmProducer, Entity, Path>::~IPCConnection() {
    close();
}

template<typename Entity, typename Path>
bool IPCConnection<ShmProducer, Entity, Path>::create(void *args) {
    if(!create_semaphore()){
        return false;
    }

    if(!m_conn.create()) {
        return false;
    }

    return true;
}

template <typename Entity, typename Path>
int IPCConnection<ShmProducer, Entity, Path>::read( Entity* ) {
    //生产者只写不读,所以读方法不用实现
    return -1;
}

template <typename Entity, typename Path>
int IPCConnection<ShmProducer, Entity, Path>::write( Entity *pentity ) {
    if(sem_wait(m_sema_put) < 0 ){
        LOGE("-->p:sema_wait failed(%d)", errno);
        return -1;
    }

    int nwrite = m_conn.write(pentity);
    LOGE("-->p:write %d bytes to channel", nwrite);

    if(sem_post(m_sema_get) < 0) {
        LOGE("-->p:sema_post failed(%d)", errno);
        return -1;
    }

    return nwrite;
}

template <typename Entity, typename Path>
bool IPCConnection<ShmProducer, Entity, Path>::create_semaphore() {
	LOGE("-->create_semaphore enter.");
	m_sema_put = sem_open(Path::sema_put_file_path,
						  O_RDWR,
						  S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH,
						  1);
	m_sema_get = sem_open(Path::sema_get_file_path,
						  O_RDWR,
						  S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH,
						  0);
	if( m_sema_put == SEM_FAILED || m_sema_get == SEM_FAILED ){
		LOGE("-->p:create semaphore failed(%d).", errno);
		return false;
	}
	return true;
}

template <typename Entity, typename Path>
void IPCConnection<ShmProducer, Entity, Path>::close() {
    if( m_sema_put != NULL) {
        sem_close(m_sema_put);
        m_sema_put = NULL;
    }
    if( m_sema_get != NULL ) {
        sem_close(m_sema_get);
        m_sema_get = NULL;
    }
}

template <typename Entity, typename Path>
void IPCConnection<ShmProducer, Entity, Path>::unlink() {
    sem_unlink(Path::sema_put_file_path);
    sem_unlink(Path::sema_get_file_path);
    sem_unlink(Path::sema_mutex_file_path);
    m_conn.unlink();
}

/**
 * 偏特化版本,共享内存的消费者,从共享内存中读取数据
 */
template <typename Entity, typename Path>
class IPCConnection<ShmConsumer, Entity, Path> {
public:
    IPCConnection();

    ~IPCConnection();

    int read(Entity *);

    int write( Entity *);

    bool create( void *args = 0 );

    void close();

    void unlink();

private:
    bool open_semaphore();

    sem_t *m_sema_put, *m_sema_get;

    IPCConnection<Shm, Entity, Path> m_conn;
};

template <typename Entity, typename Path>
IPCConnection<ShmConsumer, Entity, Path>::IPCConnection() : m_sema_put(NULL), m_sema_get(NULL){

}

template <typename Entity, typename Path>
IPCConnection<ShmConsumer, Entity, Path>::~IPCConnection() {
    close();
}

template <typename Entity, typename Path>
int IPCConnection<ShmConsumer, Entity, Path>::read(Entity *pentity) {
    if( sem_wait(m_sema_get) < 0 ) {
        LOGE("-->c:sema wait err(%d)!", errno);
        return -1;
    }

    int nread = m_conn.read(pentity);
    LOGE("c:read %d bytes from channel", nread);

    if( sem_post(m_sema_put) < 0) {
        LOGE("-->c:sema post err(%d)!", errno);
        return -1;
    }

    return nread;
}

template <typename Entity, typename Path>
int IPCConnection<ShmConsumer, Entity, Path>::write(Entity*) {
    //消费者只读不写
    return -1;
}

template <typename Entity, typename Path>
bool IPCConnection<ShmConsumer, Entity, Path>::create( void *args ) {
    if( !open_semaphore() ) {
        return false;
    }
    if( !m_conn.create()) {
        return false;
    }
    return true;
}

template <typename Entity, typename Path>
void IPCConnection<ShmConsumer, Entity, Path>::close() {
    if( m_sema_put != NULL) {
        sem_close(m_sema_put);
        m_sema_put = NULL;
    }
    if( m_sema_get != NULL ) {
        sem_close(m_sema_get);
        m_sema_get = NULL;
    }
}

template <typename Entity, typename Path>
bool IPCConnection<ShmConsumer, Entity, Path>::open_semaphore() {
    LOGE("-->client open semaphore");
    m_sema_put = sem_open(Path::sema_put_file_path,
                          O_RDWR | O_CREAT,
                          S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH,
                          1);
    m_sema_get = sem_open(Path::sema_get_file_path,
                          O_RDWR | O_CREAT,
                          S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH ,
                          0);

    if( m_sema_put == SEM_FAILED || m_sema_get == SEM_FAILED ) {
        LOGE("-->c:create semaphore failed(%d).", errno);
        return false;
    }
    LOGE("-->client open semaphore ok.");
    return true;
}

template <typename Entity, typename Path>
void IPCConnection<ShmConsumer, Entity, Path>::unlink() {
    sem_unlink(Path::sema_put_file_path);
    sem_unlink(Path::sema_get_file_path);
    sem_unlink(Path::sema_mutex_file_path);
    m_conn.unlink();
};

/**
 * 共享内存
 */
template <typename Entity, typename Path>
class IPCConnection<Shm, Entity, Path> {
public:
    IPCConnection();
    ~IPCConnection();

    bool create( void *args = 0 );

    void close();

    int read(Entity *);

    int write(Entity *);

    void unlink();

private:
    Entity *m_pmsg;

    bool init();
};

template <typename Entity, typename Path>
IPCConnection<Shm, Entity, Path>::IPCConnection() {

}

template <typename Entity, typename Path>
IPCConnection<Shm, Entity, Path>::~IPCConnection() {
    close();
}

template <typename Entity, typename Path>
bool IPCConnection<Shm, Entity, Path>::create( void *args) {
    return init();
}

template <typename Entity, typename Path>
bool IPCConnection<Shm, Entity, Path>::init(){
    int fd = open(Path::shm_file_path, O_RDWR | O_CREAT,
                  S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IROTH | S_IWOTH | S_IXOTH);
    if( fd < 0 ){
        LOGE("-->shm_open error.");
        return false;
    }
    ftruncate(fd, sizeof(Entity));

    m_pmsg = (Entity*)mmap(NULL, sizeof(Entity),
                           PROT_READ | PROT_WRITE,
                           MAP_SHARED,
                           fd,
                           0);
    if( m_pmsg == MAP_FAILED ){
        LOGE("-->mmap error.");
        ::close(fd);
        return false;
    }

    ::close(fd);

    return true;
}

template <typename Entity, typename Path>
void IPCConnection<Shm, Entity, Path>::close() {
    munmap( m_pmsg, sizeof(Entity));
}

template <typename Entity, typename Path>
int IPCConnection<Shm, Entity, Path>::read( Entity *pentity) {
    Entity::copy_ptr(m_pmsg, pentity);
    return sizeof(Entity);
}

template <typename Entity, typename Path>
int IPCConnection<Shm, Entity, Path>::write( Entity *pentity) {
    Entity::copy_ptr(pentity, m_pmsg);
    return sizeof(Entity);
};

template <typename Entity, typename Path>
void IPCConnection<Shm, Entity, Path>::unlink() {
    ::unlink(Path::shm_file_path);
}

#endif
