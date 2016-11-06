//
// Created by wangqiang on 16/6/13.
//

#ifndef SNIFFER_CIRCLEBUFFER_H
#define SNIFFER_CIRCLEBUFFER_H

#include <pthread.h>
#include "autolock.h"

/**
 * 环形队列
 */
template<typename T, unsigned BUFFER_SIZE = 100>
class CircleBuffer {
public:

    CircleBuffer();

    ~CircleBuffer() {
        clear();
    }

    /**
     * 队列是否为空
     */
    bool is_empty() {
        return m_tail == m_head;
    }

    /**
     * 队列是否满了
     */
    bool is_full() {
        return (m_tail + 1) % BUFFER_SIZE == m_head;
    }

    /**
     * 队列当前的大小
     */
    unsigned size() {
        return (m_tail - m_head + BUFFER_SIZE) % BUFFER_SIZE;
    }

    /**
     * 向队列中添加一个元素,如果队列已满,调用者将阻塞直到队列有空间
     */
    void push( T ele);

    /**
     * 从队列中取一个元素,如果队列为空,调用者将阻塞等待直到有元素为止
     */
    T pop();

    void clear() {
        m_head = m_tail = 0;
    }

private:
    unsigned m_head, m_tail;

    pthread_mutex_t m_mutex;
    pthread_cond_t  m_wait_push, m_wait_pop;

    T m_buffer[BUFFER_SIZE];
};

template <typename T, unsigned BUFFER_SIZE >
CircleBuffer<T, BUFFER_SIZE>::CircleBuffer() : m_head(0), m_tail(0) {
    m_mutex = PTHREAD_MUTEX_INITIALIZER;
    m_wait_push = PTHREAD_COND_INITIALIZER;
    m_wait_pop  = PTHREAD_COND_INITIALIZER;
}

template <typename T, unsigned BUFFER_SIZE>
void CircleBuffer<T,BUFFER_SIZE>::push(T ele) {
    AutoLock __lock(m_mutex);
    while( is_full() ) {
        pthread_cond_wait(&m_wait_push, &m_mutex);
    }
    m_buffer[m_tail] = ele;
    m_tail = (m_tail + 1) % BUFFER_SIZE;
    pthread_cond_signal(&m_wait_pop);
}

template <typename T, unsigned BUFFER_SIZE>
T CircleBuffer<T,BUFFER_SIZE>::pop() {
    AutoLock __lock(m_mutex);
    while( is_empty()) {
        pthread_cond_wait(&m_wait_pop, &m_mutex);
    }
    T ret = m_buffer[m_head];
    m_head = (m_head + 1) % BUFFER_SIZE;
    pthread_cond_signal(&m_wait_push);
    return ret;
};

#endif //SNIFFER_CIRCLEBUFFER_H
