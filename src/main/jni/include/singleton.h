//
// Created by wangqiang on 16/12/7.
// 利用CRRT实现一个单例模板用于获取单例
//

#ifndef SNIFFER_SINGLETON_H
#define SNIFFER_SINGLETON_H

#include <pthread.h>
#include "autolock.h"

//让指定类型成为友元以便访问
#define MAKE_FRIENDS(T) friend class Singleton<T>

template <typename T>
class Singleton {
public:
    static T &get_instance();
    ~Singleton();

protected:
    Singleton();

private:
    Singleton(const Singleton&);
    Singleton &operator=(const Singleton&);

    static pthread_mutex_t m_mutex;
    static T* volatile m_instance;
};

template <typename T>
T * volatile Singleton<T>::m_instance = NULL;

template <typename T>
pthread_mutex_t Singleton<T>::m_mutex = PTHREAD_MUTEX_INITIALIZER;;

template <typename T>
Singleton<T>::Singleton() {
}

template <typename T>
Singleton<T>::~Singleton() {
    pthread_mutex_destroy(&m_mutex);
    if (m_instance != NULL) {
        delete m_instance;
        m_instance = NULL;
    }
}

template <typename T>
T &Singleton<T>::get_instance() {
    if ( NULL == m_instance) {
        AutoLock lock(m_mutex);
        if (NULL == m_instance) {
            m_instance = new T();
        }
    }
    return *m_instance;
}

#endif //SNIFFER_SINGLETON_H
