#ifndef __AUTO_LOCK_H__
#define __AUTO_LOCK_H__

#include <pthread.h>

class AutoLock{
public:
	AutoLock(pthread_mutex_t &lock) : m_lock(lock){
		pthread_mutex_lock(&m_lock);
	}

	~AutoLock(){
		pthread_mutex_unlock(&m_lock);
	}
private:
	pthread_mutex_t& m_lock;
};

#endif
